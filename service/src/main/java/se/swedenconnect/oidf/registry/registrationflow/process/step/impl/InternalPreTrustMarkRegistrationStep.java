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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.Optional;
import java.util.UUID;

/**
 * PRE step for trust mark sub-flows.
 * Creates or finds the TRUST_MARK_SUBORDINATE registration record and sets
 * {@link ContextKey#REGISTRATION_ID} to its ID so subsequent steps operate on it.
 *
 * @author Felix Hellman
 */
@Component
public class InternalPreTrustMarkRegistrationStep extends NoConfigStepAdapter {

  private final RegistrationRepository registrationRepository;

  /**
   * Constructor.
   *
   * @param registrationRepository repository for registration records
   */
  public InternalPreTrustMarkRegistrationStep(final RegistrationRepository registrationRepository) {
    this.registrationRepository = registrationRepository;
  }

  @Override
  public StepType stepType() {
    return StepType.PRE;
  }

  @Override
  public FlowType flowType() {
    return FlowType.TRUST_MARK_ISSUER;
  }

  @Override
  public String getDescription() {
    return "Creates or finds the TRUST_MARK_SUBORDINATE registration for this enrollment request.";
  }

  @Override
  @Transactional
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final Optional<SerializableList<TrustmarkSource>> requested = ctx.get(ContextKey.TRUSTMARKS_REQUESTED);
    if (requested.isEmpty() || requested.get().isEmpty()) {
      return StepResult.success("No trust marks requested — skipped");
    }

    // Single TM type per sub-flow call; take the first trustmark type
    final String trustmarkType = requested.get().stream()
        .flatMap(source -> source.trustmarks().stream())
        .map(TrustmarkSource.TrustMarkStatus::trustmarkType)
        .findFirst()
        .orElse(null);

    if (trustmarkType == null) {
      return StepResult.success("No trust mark type found — skipped");
    }

    // REGISTRATION_ID currently holds the parent (SUBORDINATE) registration ID
    final Optional<Registration> parentReg = ctx.<UUID>get(ContextKey.REGISTRATION_ID)
        .flatMap(this.registrationRepository::findById);

    // Idempotent: reuse existing TM_SUBORDINATE reg for this type + parent
    final Optional<Registration> existing = parentReg.flatMap(parent ->
        this.registrationRepository.findByEntityIdAndParentRegistration_RegistrationId(
            trustmarkType, parent.getRegistrationId()));

    final Registration tmReg = existing.orElseGet(() -> {
      final Registration newReg = new Registration();
      newReg.setRegistrationId(UUID.randomUUID());
      newReg.setEntityId(trustmarkType);
      newReg.setRegistrationType(RegistrationType.TRUST_MARK_SUBORDINATE);
      newReg.setStatus(RegistrationStatus.STARTED);
      parentReg.ifPresent(p -> {
        newReg.setFlowAssignment(p.getFlowAssignment());
        newReg.setOrganization(p.getOrganization());
        newReg.setParentRegistration(p);
      });
      return this.registrationRepository.save(newReg);
    });

    ctx.put(ContextKey.REGISTRATION_ID, tmReg.getRegistrationId());
    return StepResult.success("Trust mark registration ready: " + tmReg.getRegistrationId());
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("A1B2C3D4-E5F6-4A7B-8C9D-0E1F2A3B4C5D");
  }
}
