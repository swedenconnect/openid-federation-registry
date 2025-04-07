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
package se.swedenconnect.oidf.registry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.registry.entity.FkKeyType.POLICIES;

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

  /**
   * Constructs an instance of OptionsCRUDPolicy with the specified dependencies.
   *
   * @param settingsRepository the repository for accessing settings data
   * @param policyRepository the repository for accessing policy data
   * @param organizationService the service for managing and retrieving organization information
   */
  public OptionsCRUDPolicy(
      final SettingsRepository settingsRepository,
      final PolicyRepository policyRepository,
      final OrganizationService organizationService) {
    super(settingsRepository, organizationService);
    this.policyRepository = policyRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return POLICIES == fkKeyType;
  }

  @Override
  public OptionsRecord create(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<PolicyEntity> policyEntity = this.policyRepository.findById(id);
    if (policyEntity.isPresent()) {
      super.throwNotFoundIfNotMatch(organizationRecord, policyEntity.get().getOrganizationId());

      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "POLICIES already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData
        = this.createAndValidateInputData(organizationRecord, template, record.getOption());

    // Create
    final PolicyEntity newPolicyEntity = new PolicyEntity();
    newPolicyEntity.setPolicyId(id);

    newPolicyEntity.setOrganization(getCurrentOrganization(organizationRecord));

    final PolicyEntity savedPolicyEntity = this.policyRepository.saveAndFlush(newPolicyEntity);
    super.deleteSettings(fkKeyType, savedPolicyEntity.getPolicyId().toString());
    super.insertSettings(fkKeyType, savedPolicyEntity.getPolicyId().toString(), validatedInData);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord update(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final PolicyEntity policyEntity = this.policyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    super.throwNotFoundIfNotMatch(organizationRecord, policyEntity.getOrganizationId());

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, record.getOption());
    super.deleteSettings(fkKeyType, policyEntity.getPolicyId().toString());
    super.insertSettings(fkKeyType, policyEntity.getPolicyId().toString(), validatedInData);
    this.policyRepository.saveAndFlush(policyEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {
    final PolicyEntity policyEntity = this.policyRepository
        .findByPolicyIdAndOrganizationId(id, getCurrentOrganization(organizationRecord).getOrganizationId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwNotFoundIfNotMatch(organizationRecord, policyEntity.getOrganizationId());

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType,
        super.getSettingsEntities(POLICIES, policyEntity.getPolicyId()));

    return toRecord(mergeValues);

  }

  @Override
  @Transactional
  public OptionsRecord delete(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {
    final PolicyEntity entity = this.policyRepository
        .findByPolicyIdAndOrganizationId(id, getCurrentOrganization(organizationRecord).getOrganizationId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    final List<SettingsEntity> options = deleteSettings(POLICIES, entity.getPolicyId().toString());
    this.policyRepository.delete(entity);
    this.policyRepository.flush();
    return this.toRecord(options);
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    return this.policyRepository.findByOrganizationId(
            super.getCurrentOrganization(organizationRecord).getOrganizationId())
        .stream()
        .map(entity -> {
          final Map<String, Object> e = super.getSettingsEntities(POLICIES, entity.getPolicyId())
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

