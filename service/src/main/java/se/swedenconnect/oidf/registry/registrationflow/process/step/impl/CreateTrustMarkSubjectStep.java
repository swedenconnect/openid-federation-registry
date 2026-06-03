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
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.trustmark.mapper.TrustmarkToDtoMapper;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkSubjectRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * POST step for trust mark enrollment flows.
 * <p>
 * Only runs when {@link AddTrustMarkSubjectStep} has set the
 * {@link ContextKey#TRUSTMARK_SUBJECT_PROCEED} signal in context — meaning the MID step's
 * approval gate was passed. If the signal is absent (approval still pending) this step is
 * skipped so no subject is created prematurely.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class CreateTrustMarkSubjectStep extends NoConfigStepAdapter {

  private final TrustMarkRepository trustMarkRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final RegistrationRepository registrationRepository;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param trustMarkRepository repository for resolving trust marks
   * @param trustMarkSubjectRepository repository for persisting trust mark subjects
   * @param registrationRepository repository for updating registration status
   * @param auditService audit service for publishing federation change events
   */
  public CreateTrustMarkSubjectStep(final TrustMarkRepository trustMarkRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final RegistrationRepository registrationRepository,
      final RegistryAuditService auditService) {
    this.trustMarkRepository = trustMarkRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.registrationRepository = registrationRepository;
    this.auditService = auditService;
  }

  @Override
  public StepType stepType() {
    return StepType.POST;
  }

  @Override
  public FlowType flowType() {
    return FlowType.TRUST_MARK_ISSUER;
  }

  @Override
  public String getDescription() {
    return "Creates the trust mark subject and marks the enrollment APPROVED. "
        + "Skipped when the MID step has not set the proceed signal (approval still pending).";
  }

  /** Only run when the MID step signalled that the subject should be created. */
  @Override
  public boolean canApply(final ProcessContext ctx, final StepConfig config) {
    return ctx.<Boolean>get(ContextKey.TRUSTMARK_SUBJECT_PROCEED).orElse(false);
  }

  @Override
  @Transactional
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);

    final Optional<Registration> tmRegOpt = ctx.<UUID>get(ContextKey.REGISTRATION_ID)
        .flatMap(this.registrationRepository::findById)
        .filter(r -> r.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE);

    if (tmRegOpt.isEmpty()) {
      log.warn("CreateTrustMarkSubjectStep: no TRUST_MARK_SUBORDINATE registration in context");
      return StepResult.success("No TM registration found — skipped");
    }

    final Registration tmReg = tmRegOpt.get();
    final String trustmarkType = tmReg.getEntityId();

    final Optional<SerializableList<TrustmarkSource>> requested = ctx.get(ContextKey.TRUSTMARKS_REQUESTED);
    if (requested.isEmpty()) {
      return StepResult.success("No trust marks in context — skipped");
    }

    final String issuerEntityId = requested.get().stream()
        .map(TrustmarkSource::trustMarkIssuer)
        .findFirst().orElse(null);

    if (issuerEntityId == null) {
      return StepResult.success("No issuer in context — skipped");
    }

    final Optional<TrustMark> trustMark = this.trustMarkRepository
        .findByIssuerEntityIdAndTrustmarkType(issuerEntityId, trustmarkType);

    if (trustMark.isEmpty()) {
      log.warn("CreateTrustMarkSubjectStep: trust mark not found for issuer='{}' type='{}'",
          issuerEntityId, trustmarkType);
      return StepResult.success("Trust mark not found — subject not created");
    }

    final TrustMark tm = trustMark.get();

    final boolean alreadyExists = this.trustMarkSubjectRepository
        .findByTrustMarkTrustmarkIdAndSubject(tm.getTrustmarkId(), entityId)
        .isPresent();

    if (alreadyExists) {
      tmReg.setStatus(RegistrationStatus.APPROVED);
      this.registrationRepository.save(tmReg);
      return StepResult.success("Subject already exists — registration marked APPROVED");
    }

    final TrustMarkSubject subject = new TrustMarkSubject();
    subject.setTrustmarksubjectId(UUID.randomUUID());
    subject.setTrustMark(tm);
    subject.setSubject(entityId);
    subject.setRevoked(false);
    subject.setGranted(OffsetDateTime.now());
    subject.setRegistration(tmReg);
    this.trustMarkSubjectRepository.save(subject);
    this.auditService.trustmarkSubjectCreated(
        subject.getTrustmarksubjectId(),
        tm.getTrustmarkIssuer().getEntity().getOrganization().getInstance().getInstanceId(),
        tm.getTrustmarkId(),
        tm.getTrustmarkIssuer().getEntity().getOrganization().getOrganizationId(),
        null,
        TrustmarkToDtoMapper.toDto(subject));

    tmReg.setStatus(RegistrationStatus.APPROVED);
    this.registrationRepository.save(tmReg);

    return StepResult.success("Trust mark subject created for type: " + trustmarkType);
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("B2C3D4E5-F6A7-4B8C-9D0E-1F2A3B4C5D6E");
  }
}
