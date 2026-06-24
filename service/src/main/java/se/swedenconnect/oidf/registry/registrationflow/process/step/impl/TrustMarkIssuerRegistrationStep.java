/*
 * Copyright 2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationStepRepository;
import se.swedenconnect.oidf.registry.registrationflow.dto.Mapper;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessEngine;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.repository.TrustMarkFlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;

import se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MID step that triggers the configured registration flow for each requested trust mark.
 * Each trust mark sub-flow runs as a MID-only pipeline so that framework PRE/POST steps
 * are not duplicated. Trust marks with no assigned flow are skipped with a warning.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class TrustMarkIssuerRegistrationStep extends NoConfigStepAdapter {

  private final TrustMarkRepository trustMarkRepository;
  private final TrustMarkFlowAssignmentRepository tmFlowAssignmentRepository;
  private final RegistrationStepRepository registrationStepRepository;
  private final ProcessEngine processEngine;
  private final RegistrationRepository registrationRepository;
  private final InternalPreTrustMarkRegistrationStep preTmStep;
  private final CreateTrustMarkSubjectStep postTmStep;

  /**
   * Constructor.
   *
   * @param trustMarkRepository repository for resolving trust marks by issuer and type
   * @param tmFlowAssignmentRepository repository for looking up flows assigned to trust marks
   * @param registrationStepRepository repository for resolving step definitions
   * @param processEngine engine for running sub-flows
   * @param registrationRepository repository for persisting sub-flow step results
   * @param preTmStep auto-injected PRE step that creates the TM_SUBORDINATE registration
   * @param postTmStep auto-injected POST step that creates the TrustMarkSubject
   */
  public TrustMarkIssuerRegistrationStep(final TrustMarkRepository trustMarkRepository,
      final TrustMarkFlowAssignmentRepository tmFlowAssignmentRepository,
      @Lazy final RegistrationStepRepository registrationStepRepository,
      final ProcessEngine processEngine,
      final RegistrationRepository registrationRepository,
      final InternalPreTrustMarkRegistrationStep preTmStep,
      final CreateTrustMarkSubjectStep postTmStep) {
    this.trustMarkRepository = trustMarkRepository;
    this.tmFlowAssignmentRepository = tmFlowAssignmentRepository;
    this.registrationStepRepository = registrationStepRepository;
    this.processEngine = processEngine;
    this.registrationRepository = registrationRepository;
    this.preTmStep = preTmStep;
    this.postTmStep = postTmStep;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public FlowType flowType() {
    return FlowType.INTERMEDIATE;
  }

  @Override
  @Transactional
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final var requested = ctx.<SerializableList<TrustmarkSource>>get(ContextKey.TRUSTMARKS_REQUESTED);

    if (requested.isEmpty() || requested.get().isEmpty()) {
      return StepResult.success("No trust marks requested — step skipped");
    }

    // STEP_APPROVED is still in ctx during execute (engine removes it after execute returns).
    // This tells us we are resuming an earlier approval.
    final boolean isResume = ctx.<Boolean>get(ContextKey.STEP_APPROVED).orElse(false);

    final List<String> skippedNotFound = new ArrayList<>();
    final List<String> skippedNoFlow = new ArrayList<>();
    final List<String> failed = new ArrayList<>();
    final List<String> pendingApproval = new ArrayList<>();

    final Optional<UUID> parentRegId = ctx.get(ContextKey.REGISTRATION_ID);

    for (final TrustmarkSource source : requested.get()) {
      for (final TrustmarkSource.TrustMarkStatus tmStatus : source.trustmarks()) {
        final String trustmarkType = tmStatus.trustmarkType();

        final Optional<TrustMark> trustMark = this.trustMarkRepository
            .findAllByTrustmarkType(trustmarkType)
            .stream().findFirst();

        if (trustMark.isEmpty()) {
          log.warn("TrustMarkIssuerRegistrationStep: trust mark not found for type='{}'", trustmarkType);
          skippedNotFound.add(trustmarkType);
          continue;
        }

        final UUID trustmarkId = trustMark.get().getTrustmarkId();
        final var assignment = this.tmFlowAssignmentRepository.findByTrustMarkTrustmarkId(trustmarkId);

        if (assignment.isEmpty()) {
          log.warn("TrustMarkIssuerRegistrationStep: no flow assigned to trust mark type='{}'", trustmarkType);
          skippedNoFlow.add(trustmarkType);
          continue;
        }

        // Build the sub-flow: [PRE (auto)] + [user MID steps] + [POST (auto)]
        final List<StepDefinition> midSteps = Mapper.toMidOnlyProcessFlow(
            assignment.get().getRegistrationFlow(), this.registrationStepRepository).getProcessFlow();

        final DefaultConfig emptyConfig = new DefaultConfig(java.util.Map.of());
        final List<StepDefinition> allSubSteps = new ArrayList<>();
        allSubSteps.add(new StepDefinition(this.preTmStep, emptyConfig));
        allSubSteps.addAll(midSteps);
        allSubSteps.add(new StepDefinition(this.postTmStep, emptyConfig));

        final String resolvedIssuerId = trustMark.get().getTrustmarkIssuer().getEntity().getSubject();

        // Find existing TM_SUBORDINATE registration (may exist from a previous run or fresh)
        final Optional<se.swedenconnect.oidf.registry.registrations.model.Registration> existingTmReg =
            parentRegId.flatMap(pid ->
                this.registrationRepository.findByEntityIdAndParentRegistration_RegistrationId(
                    trustmarkType, pid));

        final ProcessContext subCtx = ctx.copy();
        subCtx.put(ContextKey.TRUSTMARKS_REQUESTED, new SerializableList<>(List.of(
            new TrustmarkSource(resolvedIssuerId, List.of(tmStatus)))));

        final List<StepDefinition> stepsToRun;
        final int stepOffset;

        if (isResume && existingTmReg.isPresent()
            && existingTmReg.get().getPendingStepIndex() != null) {
          final int pendingIdx = existingTmReg.get().getPendingStepIndex();
          stepsToRun = allSubSteps.subList(pendingIdx, allSubSteps.size());
          stepOffset = pendingIdx;
          subCtx.put(ContextKey.REGISTRATION_ID, existingTmReg.get().getRegistrationId());
          log.debug("TrustMarkIssuerRegistrationStep: resuming sub-flow for '{}' from step {}",
              trustmarkType, pendingIdx);
        } else {
          stepsToRun = allSubSteps;
          stepOffset = 0;
        }

        final ProcessReport report = this.processEngine.run(stepsToRun, subCtx);

        // Find TM reg after sub-flow (PRE step may have just created it)
        final Optional<UUID> tmRegId = subCtx.get(ContextKey.REGISTRATION_ID);
        tmRegId.flatMap(this.registrationRepository::findById).ifPresent(tmReg -> {
          // Merge with previously saved step results when resuming
          final List<StepExecutionRecordDto> merged = new ArrayList<>();
          if (stepOffset > 0 && tmReg.getStepResults() != null) {
            merged.addAll(tmReg.getStepResults().subList(0,
                Math.min(stepOffset, tmReg.getStepResults().size())));
          }
          merged.addAll(RegistrationMapper.toStepExecutionRecordDtos(report));
          tmReg.setStepResults(merged);

          if (report.isPendingApproval()) {
            tmReg.setStatus(RegistrationStatus.PENDING_APPROVAL);
            tmReg.setPendingStepIndex(stepOffset + report.steps().size() - 1);
          } else {
            tmReg.setPendingStepIndex(null);
          }
          this.registrationRepository.save(tmReg);
        });

        if (report.isPendingApproval()) {
          log.info("TrustMarkIssuerRegistrationStep: sub-flow pending approval for '{}'", trustmarkType);
          pendingApproval.add(trustmarkType);
        } else if (!report.isSuccessful()) {
          log.warn("TrustMarkIssuerRegistrationStep: sub-flow failed for '{}'", trustmarkType);
          failed.add(trustmarkType);
        }
      }
    }

    if (!failed.isEmpty()) {
      return StepResult.success(
          "Trust mark flows completed with failures for: %s. Pending: %s. Not found: %s. No flow assigned: %s"
              .formatted(failed, pendingApproval, skippedNotFound, skippedNoFlow));
    }
    return StepResult.success(
        "Trust mark flows dispatched. Pending approval: %s. Not found: %s. No flow assigned: %s"
            .formatted(pendingApproval, skippedNotFound, skippedNoFlow));
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("F1A2B3C4-D5E6-4F7A-8B9C-0D1E2F3A4B5C");
  }

  @Override
  public String getDescription() {
    return "Triggers the configured registration flow for each requested trust mark. "
        + "Runs as MID-only sub-pipelines so PRE/POST framework steps are not duplicated.";
  }
}
