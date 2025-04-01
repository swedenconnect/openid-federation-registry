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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * OptionsCRUDModules is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDModules extends OptionsCRUDAdapter {

  private final ModuleRepository moduleRepository;
  private final EntityRepository entityRepository;

  /**
   * Constructor for OptionsCRUDModules, initializes necessary repositories and supplier for operations.
   *
   * @param settingsRepository the repository used for accessing and managing settings.
   * @param userAssignedOrganization a supplier providing the organization entity assigned to the user.
   * @param moduleRepository the repository for accessing and managing modules.
   * @param entityRepository the repository for accessing and managing entities.
   */
  public OptionsCRUDModules(final SettingsRepository settingsRepository,
      final Supplier<OrganizationEntity> userAssignedOrganization, final ModuleRepository moduleRepository,
      final EntityRepository entityRepository) {
    super(settingsRepository, userAssignedOrganization);
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
  public OptionsRecord template(final FkKeyType fkKeyType) {
    final OptionsRecord record = this.toRecord(this.getTemplateSettings(fkKeyType));
    this.addOptionsForEntityId(Objects.requireNonNull(record.getOption()));
    return record;
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name());

    if (moduleEntity.isPresent()) {
      super.throwUnauthorizedIfNotMatch(moduleEntity.get().getOrganization().getOrganizationId());
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Module already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());

    // Create
    final ModuleEntity newModuleEntity = new ModuleEntity();
    newModuleEntity.setModuleId(id);
    newModuleEntity.setModuleType(fkKeyType.name());
    newModuleEntity.setOrganization(super.getCurrentOrganization());

    final EntityEntity entityEntity = this.loadEntityThrowIfNotExist(validatedInData);
    ruleIssuerAndSubjectTheSameOrTrowException(entityEntity);
    newModuleEntity.setEntity(entityEntity);

    final ModuleEntity savedModuleEntity = this.moduleRepository.saveAndFlush(newModuleEntity);
    super.deleteSettings(fkKeyType, savedModuleEntity.getModuleId().toString());
    super.insertSettings(fkKeyType, savedModuleEntity.getModuleId().toString(), validatedInData);
    return this.toRecord(validatedInData);

  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .filter(super.hasRightOrganizationIdModulePredicate())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final EntityEntity entityEntity = moduleEntity.getEntity();
    ruleIssuerAndSubjectTheSameOrTrowException(entityEntity);

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    super.deleteSettings(fkKeyType, moduleEntity.getModuleId().toString());
    super.insertSettings(fkKeyType, moduleEntity.getModuleId().toString(), validatedInData);
    this.moduleRepository.saveAndFlush(moduleEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwUnauthorizedIfNotMatch(moduleEntity.getOrganization().getOrganizationId());
    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType, moduleEntity.getSettingsEntityList());
    //this.validateEntityIdentifier(fkKeyType, optionsRecord.getOption());
    return toRecord(mergeValues);
  }

  @Override
  @Transactional
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwUnauthorizedIfNotMatch(moduleEntity.getOrganization().getOrganizationId());

    this.moduleRepository.delete(moduleEntity);
    return this.toRecord(moduleEntity.getSettingsEntityList());
  }

  @Override
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {

    return this.moduleRepository.findByModuleType(fkKeyType.name())
        .stream()
        .filter(super.hasRightOrganizationIdModulePredicate())
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

  protected void addOptionsForEntityId(final List<Values> values) {
    final String parameter = "entity_id";
    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), parameter))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.entityRepository.findByEntityType(EntityKeyType.FEDERATION_ENTITY)
                .stream()
                .filter(this.hasRightOrganizationIdEntityPredicate())
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getEntityId().toString())
                        .value(entity.getIssuer())
                        .selected(Objects.equals(value.getValue(), entity.getEntityId().toString()))
                        .build())
                .toList()));
  }

  protected EntityEntity loadEntityThrowIfNotExist(final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    final String parameter = "entity_id";
    return dataValues.stream()
        .filter(value -> value.getKey().equals(parameter))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(s -> this.entityRepository.findByEntityIdAndEntityType(s, EntityKeyType.FEDERATION_ENTITY))
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid %s, does not exist".formatted(parameter))))
        .filter(this.hasRightOrganizationIdEntityPredicate())
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No trustmarkissuer to assign trustmarks to"));
  }

}

