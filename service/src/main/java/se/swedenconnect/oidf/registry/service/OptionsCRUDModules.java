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
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.registry.entity.SettingDataType;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.errorhandling.RegistryClientException;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.BAD_REQUEST;
import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.NOT_FOUND;

/**
 * OptionsCRUDModules is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDModules extends BaseOptionsCRUD {

  private final ModuleRepository moduleRepository;
  private final EntityRepository entityRepository;

  /**
   * Constructs an instance of the OptionsCRUDModules class.
   *
   * @param settingsRepository the repository instance for managing settings
   * @param moduleRepository the repository instance for managing modules
   * @param entityRepository the repository instance for managing entities
   * @param organizationService the service instance for managing organizations
   */
  public OptionsCRUDModules(final SettingsRepository settingsRepository,
      final ModuleRepository moduleRepository,
      final EntityRepository entityRepository,
      final OrganizationService organizationService) {
    super(settingsRepository, organizationService);
    this.moduleRepository = moduleRepository;
    this.entityRepository = entityRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return switch (fkKeyType) {
      case RESOLVER -> true;
      case TRUSTANCHOR -> true;
      case INTERMEDIATE -> true;
      case TRUSTMARKISSUER -> true;
      default -> false;
    };
  }

  @Override
  public OptionsRecord template(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType) {
    final OptionsRecord record = this.toRecord(this.getTemplateSettings(organizationRecord, fkKeyType));
    this.addOptionsForEntityId(organizationRecord, Objects.requireNonNull(record.getOption()));
    return record;
  }

  @Override
  public OptionsRecord create(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name());

    if (moduleEntity.isPresent()) {
      super.throwNotFoundIfNotMatch(organizationRecord, moduleEntity.get().getOrganization().getOrganizationId());
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Module already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);
    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, record.getOption());

    // Create
    final ModuleEntity newModuleEntity = new ModuleEntity();
    newModuleEntity.setModuleId(id);
    newModuleEntity.setModuleType(fkKeyType.name());
    newModuleEntity.setOrganization(super.getCurrentOrganization(organizationRecord));

    final EntityEntity entityEntity = this.loadEntityThrowIfNotExist(organizationRecord, validatedInData);
    ruleIssuerAndSubjectTheSameOrTrowException(entityEntity);
    newModuleEntity.setEntity(entityEntity);

    final ModuleEntity savedModuleEntity = this.moduleRepository.saveAndFlush(newModuleEntity);
    super.deleteSettings(fkKeyType, savedModuleEntity.getModuleId().toString());
    super.insertSettings(fkKeyType, savedModuleEntity.getModuleId().toString(), validatedInData);
    return this.toRecord(validatedInData);

  }

  @Override
  public OptionsRecord update(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .filter(super.hasRightOrganizationIdModulePredicate(organizationRecord))
        .orElseThrow(() -> new RegistryClientException(NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final EntityEntity entityEntity = moduleEntity.getEntity();
    ruleIssuerAndSubjectTheSameOrTrowException(entityEntity);

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);

    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, record.getOption());
    super.deleteSettings(fkKeyType, moduleEntity.getModuleId().toString());
    super.insertSettings(fkKeyType, moduleEntity.getModuleId().toString(), validatedInData);
    this.moduleRepository.saveAndFlush(moduleEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new RegistryClientException(NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwNotFoundIfNotMatch(organizationRecord, moduleEntity.getOrganization().getOrganizationId());

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(organizationRecord,
        fkKeyType, moduleEntity.getSettingsEntityList());
    return toRecord(mergeValues);
  }

  @Override
  @Transactional
  public OptionsRecord delete(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new RegistryClientException(NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwNotFoundIfNotMatch(organizationRecord, moduleEntity.getOrganization().getOrganizationId());

    this.moduleRepository.delete(moduleEntity);
    return this.toRecord(moduleEntity.getSettingsEntityList());
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType) {

    return this.moduleRepository.findByModuleType(fkKeyType.name())
        .stream()
        .filter(super.hasRightOrganizationIdModulePredicate(organizationRecord))
        .map(entity -> {
              final Map<String, Object> e = entity.getSettingsEntityList()
                  .stream()
                  .collect(Collectors.toMap(
                      SettingsEntity::getKey,
                      SettingsEntity::castValue
                  ));
              e.put("id", entity.getModuleId().toString());
              return e;
            }
        )
        .toList();
  }

  protected void addOptionsForEntityId(final OrganizationRecord organizationRecord, final List<Values> values) {
    final String parameter = "entity_id";
    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), parameter))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.entityRepository.findByOrgNumberAndEntityKeyType(
                    organizationRecord.orgNumber(), EntityKeyType.FEDERATION_ENTITY)
                .stream()
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getEntityId().toString())
                        .value(entity.getIssuer())
                        .selected(Objects.equals(value.getValue(), entity.getEntityId().toString()))
                        .build())
                .toList()));
  }

  protected EntityEntity loadEntityThrowIfNotExist(final OrganizationRecord organizationRecord,
      final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    final String parameter = "entity_id";
    return dataValues.stream()
        .filter(value -> value.getKey().equals(parameter))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(s -> this.entityRepository
            .findByOrgNumberAndEntityIdAndEntityKeyType(organizationRecord.orgNumber(), s,
                EntityKeyType.FEDERATION_ENTITY))
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new RegistryClientException(BAD_REQUEST,
                "Invalid %s, does not exist".formatted(parameter))))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No trustmarkissuer to assign trustmarks to"));
  }

}

