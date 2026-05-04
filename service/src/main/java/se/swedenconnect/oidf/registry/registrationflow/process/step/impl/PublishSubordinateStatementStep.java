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
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowRepository;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Last step in every registration flow. Saves a snapshot of the collected data
 * to the {@code registrations} table with status {@code PENDING} so an operator
 * can review it and create the subordinate statement manually.
 * <p>
 * If a {@code PENDING} record already exists for the same {@code entity_id} the
 * snapshot is refreshed (idempotent re-submission). If the entity is already
 * {@code APPROVED} the step fails so the caller gets a 409.
 *
 * @author Per Fredrik Plars
 */
@Component
public class ManualValidationStep extends NoConfigStepAdapter {

  private final RegistrationRepository registrationRepository;
  private final TaImRepository taImRepository;
  private final FlowRepository flowRepository;
  private final JsonMapper objectMapper;

  /**
   * Constructs a new ManualValidationStep.
   *
   * @param registrationRepository repository for persisting registrations
   * @param taImRepository repository for trust anchor intermediates
   * @param flowRepository repository for registration flows
   * @param objectMapper JSON mapper
   */
  public ManualValidationStep(final RegistrationRepository registrationRepository,
      final TaImRepository taImRepository,
      final FlowRepository flowRepository,
      final JsonMapper objectMapper) {
    this.registrationRepository = registrationRepository;
    this.taImRepository = taImRepository;
    this.flowRepository = flowRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final UUID taimId = ctx.getRequired(ContextKey.TAIM_ID);
    final UUID flowId = ctx.getRequired(ContextKey.REGISTRATION_FLOW_ID);

    final Optional<Registration> approved =
        this.registrationRepository.findByEntityIdAndStatus(entityId, RegistrationStatus.APPROVED);
    if (approved.isPresent()) {
      return StepResult.failure("Entity %s is already registered (APPROVED)".formatted(entityId), List.of());
    }

    final String jwks = ctx.<String> get(ContextKey.ENTITY_CONFIGURATION_JWKS).orElse(null);
    final String metadata = ctx.<String> get(ContextKey.ENTITY_CONFIGURATION_METADATA).orElse(null);
    final String metadataPolicy = ctx.<String> get(ContextKey.METADATA_POLICY).orElse(null);
    final String trustmarks = ctx.<String> get(ContextKey.TRUSTMARKS_REQUESTED).orElse(null);

    final Optional<Registration> existing =
        this.registrationRepository.findByEntityIdAndStatus(entityId, RegistrationStatus.PENDING);

    if (existing.isPresent()) {
      final Registration reg = existing.get();
      reg.setJwks(jwks);
      reg.setMetadata(metadata);
      reg.setMetadataPolicy(metadataPolicy);
      reg.setTrustmarksRequested(trustmarks);
      this.registrationRepository.save(reg);
      return StepResult.success();
    }

    final TrustAnchorIntermediateModule taIm = this.taImRepository.findById(taimId)
        .orElseThrow(() -> new IllegalStateException("TaIm not found: " + taimId));
    final RegistrationFlow flow = this.flowRepository.findById(flowId)
        .orElseThrow(() -> new IllegalStateException("RegistrationFlow not found: " + flowId));

    final Registration registration = new Registration();
    registration.setId(UUID.randomUUID());
    registration.setTaIm(taIm);
    registration.setRegistrationFlow(flow);
    registration.setEntityId(entityId);
    registration.setJwks(jwks);
    registration.setMetadata(metadata);
    registration.setMetadataPolicy(metadataPolicy);
    registration.setTrustmarksRequested(trustmarks);
    registration.setStatus(RegistrationStatus.PENDING);

    this.registrationRepository.save(registration);
    return StepResult.success();
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("B292AA20-0F6A-4362-830F-B22AC36B76ED");
  }
}