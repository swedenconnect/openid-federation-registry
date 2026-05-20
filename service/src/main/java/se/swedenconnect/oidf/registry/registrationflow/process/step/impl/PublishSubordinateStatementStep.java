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

import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.subordinate.service.SubordinateService;

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
@Slf4j
public class PublishSubordinateStatementStep extends NoConfigStepAdapter {

  private final RegistrationRepository registrationRepository;
  private final SubordinateService subordinateService;

  /**
   * Constructs a new ManualValidationStep.
   *
   * @param registrationRepository repository for persisting registrations
   * @param subordinateService subordinate service for creating and updating subordinates
   */
  public PublishSubordinateStatementStep(final RegistrationRepository registrationRepository,
      final SubordinateService subordinateService) {
    this.registrationRepository = registrationRepository;
    this.subordinateService = subordinateService;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  @Transactional
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final UUID registrationId = ctx.getRequired(ContextKey.REGISTRATION_ID);
    final Optional<JSONObject> metadataPolicy = ctx.get(ContextKey.METADATA_POLICY);
    final JWKSet ecJwks = ctx.getRequired(ContextKey.ENTITY_CONFIGURATION_JWKS, JWKSet.class);

    final Registration registration = this.registrationRepository.findById(registrationId)
        .orElseThrow();

    if(config.getBoolean("manualreview")) {
      registration.setStatus(RegistrationStatus.PENDING_APPROVAL);
      this.registrationRepository.save(registration);
      return StepResult.success("Registration is pending approval");
    }
    final Organization imOrganization = registration.getFlowAssignment().getTaIm().getOrganization();
    final OrganizationRecord org = new OrganizationRecord(imOrganization.getOrgNumber(), imOrganization.getOrgName(),
        null,null);

    this.subordinateService.getByEntityidentifierAndTaIm(entityId,
        registration.getFlowAssignment().getTaIm().getTaImId())
        .ifPresentOrElse(subordinateDto -> {//Update
          metadataPolicy.ifPresent(subordinateDto::setMetadataPolicy);
          subordinateDto.setJwks(ecJwks.toJSONObject());
          this.subordinateService.updateSubordinate(org,subordinateDto.getSubordinateId(),subordinateDto);
           }, () -> {
          // Create
            final SubordinateDto newSubordinate = new SubordinateDto();
            newSubordinate.setSubordinateId(UUID.randomUUID());
            newSubordinate.setTaImId(registration.getFlowAssignment().getTaIm().getTaImId());
            newSubordinate.setEntityIdentifier(entityId);
            metadataPolicy.ifPresent(newSubordinate::setMetadataPolicy);
            newSubordinate.setJwks(ecJwks.toJSONObject());
            this.subordinateService.createSubordinate(org, newSubordinate);
        });


    /*
    this.subordinateService.getByEntityidentifierAndTaIm(entityId,
        registration.getFlowAssignment().getTaIm().getTaImId()).or(() ->
          {
              final SubordinateDto newSubordinate = new SubordinateDto();
              newSubordinate.setSubordinateId(UUID.randomUUID());
              newSubordinate.setTaImId(registration.getFlowAssignment().getTaIm().getTaImId());
              newSubordinate.setEntityIdentifier(entityId);
              metadataPolicy.ifPresent(newSubordinate::setMetadataPolicy);
              newSubordinate.setJwks(ecJwks.toJSONObject());
              return Optional.ofNullable(this.subordinateService.createSubordinate(new OrganizationRecord(),
              newSubordinate));
         }
    );

    final Subordinate subordinate =
        this.subordinateRepository.findByEntityidentifierAndTaIm_TaImId(entityId,
            registration.getFlowAssignment().getTaIm().getTaImId()).or(() -> {
          final Subordinate newSubordinate = new Subordinate();
          newSubordinate.setSubordinateId(UUID.randomUUID());
          newSubordinate.setTaIm(registration.getFlowAssignment().getTaIm());
          newSubordinate.setEntityidentifier(entityId);
          metadataPolicy.ifPresent(newSubordinate::setMetadataPolicy);
          return Optional.of(newSubordinate);
        }).orElseThrow();


      metadataPolicy.ifPresent(subordinate::setMetadataPolicy);
      subordinate.setJwks(ecJwks.toJSONObject());
      this.subordinateRepository.save(subordinate);
*/
      registration.setStatus(RegistrationStatus.APPROVED);
      this.registrationRepository.save(registration);
      return StepResult.success("Registration is approved. And published");
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("B292AA20-0F6A-4362-830F-B22AC36B76ED");
  }

  @Override
  public String getDescription() {
    return "This step will create a subordinate statement for this a EntityId. Require JWKS and entityid";
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(new StepConfigurationValue("manualreview",
        StepConfigurationValue.DATA_TYPE.BOOLEAN,
        "If true this request is redirected to a manual approval flow","false"));
  }
}