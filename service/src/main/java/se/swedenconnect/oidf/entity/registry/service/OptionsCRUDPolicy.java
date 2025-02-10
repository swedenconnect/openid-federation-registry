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
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.OrganizationRepository;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
   * Constructor for OptionsCRUDPolices.
   *
   * @param registryAuditService the service used for registry auditing.
   * @param instanceRepository the repository for managing instances.
   * @param settingsRepository the repository for system settings.
   * @param policyRepository the repository for handling policyRepository.
   * @param organizationRepository the repository for managing modules.
   */
  public OptionsCRUDPolicy(final RegistryAuditService registryAuditService,
      final InstanceRepository instanceRepository, final SettingsRepository settingsRepository,
      final PolicyRepository policyRepository, final OrganizationRepository organizationRepository) {
    super(registryAuditService, instanceRepository, settingsRepository);
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
    this.loadOrganizationThrowIfNotExist(validatedInData).ifPresent(newPolicyEntity::setOrganizationEntity);

    final PolicyEntity savedPolicyEntity = this.policyRepository.saveAndFlush(newPolicyEntity);
    super.deleteInsertSettings(fkKeyType, savedPolicyEntity.getPolicyId().toString(), validatedInData);

    return this.toRecord(validatedInData);

  }

  protected Optional<OrganizationEntity> loadOrganizationThrowIfNotExist(final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    // todo make sure to check for organization
    return dataValues.stream()
        .filter(value -> value.getKey().equals("organization_id"))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(this.organizationRepository::findById)
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid organization_id, does not exist")))
        .findFirst();
  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final PolicyEntity policyEntity = this.policyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    super.deleteInsertSettings(fkKeyType, policyEntity.getPolicyId().toString(), validatedInData);
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
    this.addOptionsForInstanceID(optionsRecord.getOption());
    return optionsRecord;

  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    final OptionsRecord optionsRecord = toRecord(getTemplateSettings(fkKeyType));
    this.addOptionsForOrganizationId(Objects.requireNonNull(optionsRecord.getOption()));
    return optionsRecord;
  }

  protected void addOptionsForOrganizationId(final List<Values> values) {
    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), "organization_id"))
        .findFirst()
        .ifPresent(value -> {
          value.setOptions(this.organizationRepository.findAll()
              .stream()
              .map(entity ->
                  OptionRecord.builder()
                      .key(entity.getOrganizationId().toString())
                      .value(entity.getOrgName())
                      .selected(Objects.equals(value.getValue(), entity.getOrganizationId().toString()))
                      .build())
              .toList());
        });
  }

  @Override
  public void delete(final FkKeyType fkKeyType, final UUID id) {
    final PolicyEntity entity = this.policyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    deleteInsertSettings(POLICIES, entity.getPolicyId().toString(), Collections.emptyList());
    this.policyRepository.delete(entity);
    this.policyRepository.flush();
  }

}

