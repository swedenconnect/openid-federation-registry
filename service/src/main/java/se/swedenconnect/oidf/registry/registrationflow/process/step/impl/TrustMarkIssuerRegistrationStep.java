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
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;

import java.util.ArrayList;
import java.util.List;
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

  /**
   * Constructor.
   *
   * @param trustMarkRepository repository for resolving trust marks by issuer and type
   * @param tmFlowAssignmentRepository repository for looking up flows assigned to trust marks
   * @param registrationStepRepository repository for resolving step definitions
   * @param processEngine engine for running sub-flows
   */
  public TrustMarkIssuerRegistrationStep(final TrustMarkRepository trustMarkRepository,
      final TrustMarkFlowAssignmentRepository tmFlowAssignmentRepository,
      @Lazy final RegistrationStepRepository registrationStepRepository,
      final ProcessEngine processEngine) {
    this.trustMarkRepository = trustMarkRepository;
    this.tmFlowAssignmentRepository = tmFlowAssignmentRepository;
    this.registrationStepRepository = registrationStepRepository;
    this.processEngine = processEngine;
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
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final var requested = ctx.<SerializableList<TrustmarkSource>>get(ContextKey.TRUSTMARKS_REQUESTED);

    if (requested.isEmpty() || requested.get().isEmpty()) {
      return StepResult.success("No trust marks requested — step skipped");
    }

    final List<String> skippedNotFound = new ArrayList<>();
    final List<String> skippedNoFlow = new ArrayList<>();
    final List<String> failed = new ArrayList<>();

    for (final TrustmarkSource source : requested.get()) {
      final String issuerEntityId = source.trustMarkIssuer();

      for (final TrustmarkSource.TrustMarkStatus tmStatus : source.trustmarks()) {
        final String trustmarkType = tmStatus.trustmarkType();

        final var trustMark = this.trustMarkRepository
            .findByIssuerEntityIdAndTrustmarkType(issuerEntityId, trustmarkType);

        if (trustMark.isEmpty()) {
          log.warn("TrustMarkIssuerRegistrationStep: trust mark not found for issuer='{}' type='{}'",
              issuerEntityId, trustmarkType);
          skippedNotFound.add(trustmarkType);
          continue;
        }

        final UUID trustmarkId = trustMark.get().getTrustmarkId();
        final var assignment = this.tmFlowAssignmentRepository.findByTrustMarkTrustmarkId(trustmarkId);

        if (assignment.isEmpty()) {
          log.warn("TrustMarkIssuerRegistrationStep: no flow assigned to trust mark '{}' ({})",
              trustmarkType, issuerEntityId);
          skippedNoFlow.add(trustmarkType);
          continue;
        }

        final ProcessFlow subFlow = Mapper.toMidOnlyProcessFlow(
            assignment.get().getRegistrationFlow(), this.registrationStepRepository);

        if (subFlow.getProcessFlow().isEmpty()) {
          log.warn("TrustMarkIssuerRegistrationStep: flow '{}' for trust mark '{}' has no MID steps",
              assignment.get().getRegistrationFlow().getName(), trustmarkType);
          continue;
        }

        // Sub-context scoped to this specific trust mark
        final ProcessContext subCtx = ctx.copy();
        subCtx.put(ContextKey.TRUSTMARKS_REQUESTED, new SerializableList<>(List.of(
            new TrustmarkSource(issuerEntityId, List.of(tmStatus)))));

        final ProcessReport report = this.processEngine.run(subFlow.getProcessFlow(), subCtx);
        if (!report.isSuccessful() && !report.isPendingApproval()) {
          log.warn("TrustMarkIssuerRegistrationStep: sub-flow '{}' failed for trust mark '{}'",
              subFlow.getName(), trustmarkType);
          failed.add(trustmarkType);
        }
      }
    }

    if (!failed.isEmpty()) {
      return StepResult.success(
          "Trust mark flows completed with failures for: %s. Not found: %s. No flow assigned: %s"
              .formatted(failed, skippedNotFound, skippedNoFlow));
    }
    return StepResult.success(
        "Trust mark flows completed. Not found: %s. No flow assigned: %s"
            .formatted(skippedNotFound, skippedNoFlow));
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
