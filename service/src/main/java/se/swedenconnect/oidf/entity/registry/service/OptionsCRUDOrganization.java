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
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.OrganizationRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.ORGANIZATION;

/**
 * OptionsCRUDPolices is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDOrganization extends OptionsCRUDAdapter {

  private final OrganizationRepository organizationRepository;

  /**
   * Constructor for the OptionsCRUDOrganization class, which initializes the organization repository and inherits
   * behavior and properties from its superclass.
   *
   * @param instanceRepository the repository used for managing instance data
   * @param settingsRepository the repository used for managing application settings
   * @param organizationRepository the repository used for managing organization data
   */
  public OptionsCRUDOrganization(
      final InstanceRepository instanceRepository, final SettingsRepository settingsRepository,
      final OrganizationRepository organizationRepository) {
    super(instanceRepository, settingsRepository);
    this.organizationRepository = organizationRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return ORGANIZATION == fkKeyType;
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<OrganizationEntity> entity = this.organizationRepository.findById(id);

    if (entity.isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "POLICIES already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());

    // Create
    final OrganizationEntity newEntity = new OrganizationEntity();
    newEntity.setOrganizationId(id);

    validatedInData.stream()
        .filter(settingsEntity -> settingsEntity.getKey().equals("org-id"))
        .findFirst()
        .ifPresent(settingsEntity -> newEntity.setOrgId(settingsEntity.getValue()));

    validatedInData.stream()
        .filter(settingsEntity -> settingsEntity.getKey().equals("org-name"))
        .findFirst()
        .ifPresent(settingsEntity -> newEntity.setOrgName(settingsEntity.getValue()));

    validatedInData.stream()
        .filter(settingsEntity -> settingsEntity.getKey().equals("entityid-filter"))
        .findFirst()
        .ifPresent(settingsEntity -> newEntity.setEntityidFilter(settingsEntity.getValue()));

    final OrganizationEntity savedEntity = this.organizationRepository.saveAndFlush(newEntity);
    super.deleteSettings(fkKeyType, savedEntity.getOrganizationId().toString());
    super.insertSettings(fkKeyType, savedEntity.getOrganizationId().toString(), validatedInData);

    return this.toRecord(validatedInData);

  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final OrganizationEntity entity = this.organizationRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    super.deleteSettings(fkKeyType, entity.getOrganizationId().toString());
    super.insertSettings(fkKeyType, entity.getOrganizationId().toString(), validatedInData);
    this.organizationRepository.saveAndFlush(entity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final OrganizationEntity entity = this.organizationRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType,
        super.getSettingsEntities(ORGANIZATION, entity.getOrganizationId().toString()));

    return toRecord(mergeValues);

  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    return toRecord(getTemplateSettings(fkKeyType));
  }

  @Override
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    final OrganizationEntity entity = this.organizationRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    final List<SettingsEntity> optional = deleteSettings(ORGANIZATION, entity.getOrganizationId().toString());
    this.organizationRepository.delete(entity);
    this.organizationRepository.flush();
    return this.toRecord(optional);
  }

  @Override
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {
    return this.organizationRepository.findAll()
        .stream()
        .map(entity -> {
              final Map<String, Object> e =
                  super.getSettingsEntities(ORGANIZATION, entity.getOrganizationId().toString())
                      .stream()
                      .collect(Collectors.toMap(
                          SettingsEntity::getKey,
                          SettingsEntity::castValue
                      ));
              e.put("id", entity.getOrganizationId().toString());
              return e;
            }
        )

        .toList();
  }
}

