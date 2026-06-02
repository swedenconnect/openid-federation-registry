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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkSubjectRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adds the registering entity as a trust mark subject for each requested trust mark type.
 * Idempotent: skips types where a subject entry already exists.
 * Skips issuer/type combinations not found in the registry and records them as warnings.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class AddTrustMarkSubjectStep extends NoConfigStepAdapter {

  private final TrustMarkRepository trustMarkRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final RegistrationRepository registrationRepository;

  /**
   * Constructor.
   *
   * @param trustMarkRepository repository for resolving trust marks
   * @param trustMarkSubjectRepository repository for persisting trust mark subjects
   * @param registrationRepository repository for loading the source registration
   */
  public AddTrustMarkSubjectStep(final TrustMarkRepository trustMarkRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final RegistrationRepository registrationRepository) {
    this.trustMarkRepository = trustMarkRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.registrationRepository = registrationRepository;
  }

  @Override
  public StepType stepType() {
    return StepType.MID;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public FlowType flowType() {
    return FlowType.TRUST_MARK_ISSUER;
  }

  @Override
  @Transactional
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final boolean alreadyApproved = ctx.<Boolean>get(ContextKey.STEP_APPROVED).orElse(false);
    if (config.getBoolean("manualreview") && !alreadyApproved) {
      return StepResult.pendingApproval("Trust mark subject enrollment requires manual approval");
    }

    final Optional<SerializableList<TrustmarkSource>> requested =
        ctx.get(ContextKey.TRUSTMARKS_REQUESTED);

    if (requested.isEmpty() || requested.get().isEmpty()) {
      return StepResult.success("No trust marks requested — step skipped");
    }

    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final List<String> skipped = new ArrayList<>();
    int created = 0;

    for (final TrustmarkSource source : requested.get()) {
      for (final TrustmarkSource.TrustMarkStatus status : source.trustmarks()) {
        final Optional<TrustMark> trustMark = this.trustMarkRepository
            .findByIssuerEntityIdAndTrustmarkType(source.trustMarkIssuer(), status.trustmarkType());

        if (trustMark.isEmpty()) {
          skipped.add("%s / %s".formatted(source.trustMarkIssuer(), status.trustmarkType()));
          continue;
        }

        final TrustMark tm = trustMark.get();
        final boolean alreadyExists = this.trustMarkSubjectRepository
            .findByTrustMarkTrustmarkIdAndSubject(tm.getTrustmarkId(), entityId)
            .isPresent();

        if (!alreadyExists) {
          final Optional<Registration> parentReg = ctx.<UUID>get(ContextKey.REGISTRATION_ID)
              .flatMap(this.registrationRepository::findById);

          final Registration tmRegistration = new Registration();
          tmRegistration.setRegistrationId(UUID.randomUUID());
          tmRegistration.setEntityId(status.trustmarkType());
          tmRegistration.setRegistrationType(RegistrationType.TRUST_MARK_SUBORDINATE);
          tmRegistration.setStatus(RegistrationStatus.APPROVED);
          parentReg.ifPresent(p -> {
            tmRegistration.setFlowAssignment(p.getFlowAssignment());
            tmRegistration.setOrganization(p.getOrganization());
            tmRegistration.setParentRegistration(p);
          });
          this.registrationRepository.save(tmRegistration);

          final TrustMarkSubject subject = new TrustMarkSubject();
          subject.setTrustmarksubjectId(UUID.randomUUID());
          subject.setTrustMark(tm);
          subject.setSubject(entityId);
          subject.setRevoked(false);
          subject.setGranted(OffsetDateTime.now());
          subject.setRegistration(tmRegistration);
          this.trustMarkSubjectRepository.save(subject);
          created++;
        }
      }
    }

    if (!skipped.isEmpty()) {
      log.warn("AddTrustMarkSubjectStep: trust mark(s) not found in registry: {}", skipped);
    }

    return StepResult.success(
        "Added %d trust mark subject(s). Skipped %d unknown type(s).".formatted(created, skipped.size()));
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("3F8A1C2D-7E4B-4F9A-B5D6-8C0E2A3F1B4D");
  }

  @Override
  public String getDescription() {
    return "Adds the registering entity as a trust mark subject for each requested trust mark type. "
        + "Idempotent — skips subjects that already exist.";
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(new StepConfigurationValue("manualreview",
        StepConfigurationValue.DATA_TYPE.BOOLEAN,
        "If true, enrollment requires manual approval before subjects are added", "false"));
  }
}
