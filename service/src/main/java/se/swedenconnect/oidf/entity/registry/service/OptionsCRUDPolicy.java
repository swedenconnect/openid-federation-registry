/*
 * Copyright 2025 Sweden Connect
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
package se.swedenconnect.oidf.entity.registry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.OrganizationRepository;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.POLICIES;

/**
 * OptionsCRUDPolices is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDPolicy extends OptionsCRUDAdapter {

  private final PolicyRepository policyRepository;
  private final OrganizationRepository organizationRepository;

  /**
   * Constructs an OptionsCRUDPolicy with the specified repositories and organization supplier.
   *
   * @param instanceRepository the repository for managing application instances
   * @param settingsRepository the repository for managing settings
   * @param policyRepository the repository for managing policies
   * @param organizationRepository the repository for managing organizations
   * @param userAssignedOrganization a supplier for retrieving the user-assigned organization entity
   */
  public OptionsCRUDPolicy(
      final InstanceRepository instanceRepository, final SettingsRepository settingsRepository,
      final PolicyRepository policyRepository, final OrganizationRepository organizationRepository,
      final Supplier<OrganizationEntity> userAssignedOrganization) {
    super(settingsRepository, userAssignedOrganization);
    this.policyRepository = policyRepository;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return POLICIES == fkKeyType;
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<PolicyEntity> policyEntity = this.policyRepository.findById(id);

    if (policyEntity.isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "POLICIES already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());

    // Create
    final PolicyEntity newPolicyEntity = new PolicyEntity();
    newPolicyEntity.setPolicyId(id);
    newPolicyEntity.setName("<NoUsed>");//TODO: REMOVE
    newPolicyEntity.setPolicy("{}");//TODO: REMOVE
    newPolicyEntity.setOrganization(getCurrentOrganization());

    final PolicyEntity savedPolicyEntity = this.policyRepository.saveAndFlush(newPolicyEntity);
    super.deleteSettings(fkKeyType, savedPolicyEntity.getPolicyId().toString());
    super.insertSettings(fkKeyType, savedPolicyEntity.getPolicyId().toString(), validatedInData);
    return this.toRecord(validatedInData);
  }



  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final PolicyEntity policyEntity = this.policyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    if (!policyEntity.getOrganization().getOrganizationId()
        .equals(getCurrentOrganization().getOrganizationId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "This policy does not belong to the current organization");
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    super.deleteSettings(fkKeyType, policyEntity.getPolicyId().toString());
    super.insertSettings(fkKeyType, policyEntity.getPolicyId().toString(), validatedInData);
    this.policyRepository.saveAndFlush(policyEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final PolicyEntity policyEntity = this.policyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType,
        super.getSettingsEntities(POLICIES, policyEntity.getPolicyId().toString()));

    final OptionsRecord optionsRecord = toRecord(mergeValues);
    return optionsRecord;

  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    return toRecord(getTemplateSettings(fkKeyType));
  }

  @Override
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    final PolicyEntity entity = this.policyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    final List<SettingsEntity> options = deleteSettings(POLICIES, entity.getPolicyId().toString());
    this.policyRepository.delete(entity);
    this.policyRepository.flush();
    return this.toRecord(options);
  }

  @Override
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {
    return this.policyRepository.findByOrganizationId(super.getCurrentOrganization().getOrganizationId())
        .stream()
        .map(entity -> {
              final Map<String, Object> e = super.getSettingsEntities(POLICIES, entity.getPolicyId().toString())
                  .stream()
                  .collect(Collectors.toMap(
                      SettingsEntity::getKey,
                      SettingsEntity::castValue
                  ));
              e.put("id", entity.getPolicyId().toString());
              return e;
            }
        )
        .toList();
  }

}

