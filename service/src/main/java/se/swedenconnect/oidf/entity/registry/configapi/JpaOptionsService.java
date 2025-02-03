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

package se.swedenconnect.oidf.entity.registry.configapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.jpaentity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.jpaentity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.jpaentity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.jpaentity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidationFailException;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidator;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.SettingsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service class for handling CRUD operations for SettingsEntity.
 */
@Slf4j
public class JpaOptionsService {

  private final SettingsRepository repository;
  private final ModuleRepository moduleRepository;
  private final RegistryAuditService registryAuditService;
  private final InstanceRepository instanceRepository;
  private PropertyValidators validatorFactory = new PropertyValidators();

  public JpaOptionsService(final SettingsRepository repository, final ModuleRepository moduleRepository,
      final RegistryAuditService registryAuditService, final InstanceRepository instanceRepository) {
    this.repository = repository;
    this.moduleRepository = moduleRepository;
    this.registryAuditService = registryAuditService;
    this.instanceRepository = instanceRepository;
  }

  public SettingsRecord create(final FkKeyType fkKeyType, final String id, final SettingsRecord record) {

    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByExternalIdAndModuleType(id, fkKeyType.name());

    final List<SettingsEntity> template = getTemplateSettings(fkKeyType);
    if (template.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No template found for:%s %s".formatted(fkKeyType, id));
    }

    if (moduleEntity.isEmpty()) {
      // Create module
      final ModuleEntity newModuleEntity = new ModuleEntity();
      newModuleEntity.setExternalId(id);
      newModuleEntity.setModuleType(fkKeyType.name());
      mergeValidateData(template, record.getValues());
      final ModuleEntity savedModuleEntity = this.moduleRepository.save(newModuleEntity);

      final List<SettingsEntity> mergeSettingsEntity =
          mergeValidateData(template, record.getValues());

      mergeSettingsEntity.forEach(settingsEntity -> {
        settingsEntity.setFkId(savedModuleEntity.getModuleId());
        settingsEntity.setFkType(savedModuleEntity.getModuleType());
      });

      this.repository.saveAllAndFlush(mergeSettingsEntity);
      return toRecord(mergeSettingsEntity);
    }

    final List<SettingsEntity> validatedInData = mergeValidateData(template, record.getValues());
    validatedInData.forEach(settingsEntity -> {
      settingsEntity.setFkId(moduleEntity.get().getModuleId());
      settingsEntity.setFkType(moduleEntity.get().getModuleType());
    });
    this.repository.deleteAllInBatch(moduleEntity.get().getSettingsEntityList());

    this.repository.saveAllAndFlush(validatedInData);

    return toRecord(validatedInData);
  }

  public SettingsRecord get(final FkKeyType fkKeyType, final String id) {
    final Optional<ModuleEntity> moduleEntity = moduleRepository
        .findByExternalIdAndModuleType(id, fkKeyType.name());
    if (moduleEntity.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for:%s %s".formatted(fkKeyType, id));
    }
    final SettingsRecord template = getTemplate(fkKeyType);
    final SettingsRecord data = toRecord(moduleEntity.get().getSettingsEntityList());
    final List<Values> values = mergeValues(template.getValues(), data.getValues());
    addOptionsForInstanceID(values);
    validateEntityIdentifier(fkKeyType, values);

    return SettingsRecord.builder()
        .values(values)
        .build();
  }

  private void validateEntityIdentifier(final FkKeyType fkKeyType, final List<Values> values) {
    final Optional<Values> entityIdentifyer = values.stream()
        .filter(value -> value.getKey().equals("entity_identifier"))
        .findFirst();

    if (entityIdentifyer.isEmpty()) {
      return;
    }
    // Todo validate that this entity_identifier comply to the organization rules
  }

  private void addOptionsForInstanceID(List<Values> values) {
    values.stream()
        .filter(value -> value.getValueType().equals(SettingDataType.OPTIONS.name()))
        .filter(value -> value.getKey().equals("instance_id"))
        .findFirst()
        .ifPresent(value -> {
          value.setOptions(instanceRepository
              .findAll()//ToDo: Filter out instance according to your organization regulation
              .stream()
              .map(instanceEntity ->
                  OptionRecord.builder()
                      .key(instanceEntity.getInstanceId())
                      .value(instanceEntity.getName())
                      .selected(value.getValue().equals(instanceEntity.getInstanceId()))
                      .build())
              .toList());
        });
  }

  public SettingsRecord getTemplate(final FkKeyType fkKeyType) {
    final SettingsRecord settingsRecord =
        toRecord(getTemplateSettings(fkKeyType));
    addOptionsForInstanceID(settingsRecord.getValues());
    return settingsRecord;
  }

  public List<SettingsEntity> getTemplateSettings(final FkKeyType FKKEYTYPE) {
    return this.repository.findByFkTypeAndFkId(FKKEYTYPE.name(), "TEMPLATE");
  }

  private List<Values> mergeValues(List<Values> templateValues, List<Values> dataValues) {
    return templateValues
        .stream()
        .map(templateValue ->
            dataValues.stream()
                .filter(dataValue -> dataValue.getKey().equals(templateValue.getKey()))
                .map(dataValue -> {
                  templateValue.setValue(dataValue.getValue());
                  return templateValue;
                })
                .findFirst()
                .orElse(null)
        )
        .filter(Objects::nonNull)
        .toList();
  }

  private List<SettingsEntity> mergeValidateData(List<SettingsEntity> templateValues, List<Values> dataValues) {
    return templateValues
        .stream()
        .map(templateValue ->
            dataValues.stream()
                .filter(dataValue -> dataValue.getKey().equals(templateValue.getKey()))
                .map(dataValue -> {
                  final PropertyValidator validator = validatorFactory.resolveValidator(dataValue.getValidation());
                  validator.validate(dataValue.getKey(), dataValue.getValue());
                  return SettingsEntity.builder()
                      .key(dataValue.getKey())
                      .value(dataValue.getValue())
                      .build();
                })
                .findFirst()
                .orElse(null)
        ).toList();
  }

  /**
   * public SettingsRecord update(final String settingRecordId, final SettingsRecord record) { if
   * (!settingRecordId.equals(record.getSettingsRecordId())){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
   * "SettingsRecordId has to match in json payload."); } return this.create(record); }
   *
   *
   * public void delete(final String settingRecordId) { this.repository.findByExternalId(settingRecordId) .map(entity ->
   * { this.registryAuditService .settingsDelete(settingRecordId,this.toRecord(entity)); return entity; })
   * .ifPresent(this.repository::delete); }
   */

  private SettingsRecord toRecord(final List<SettingsEntity> entitys) {
    return SettingsRecord.builder()
        .values(entitys.stream().map(entity -> Values.builder()
            .settingDescription(entity.getDescription())
            .validation(entity.getValidation())
            .key(entity.getKey())
            .value(entity.getValue())
            .valueType(entity.getValueDataType())
            //.options(entity.getOptions())
            .build()).toList())
        .build();
  }

   /* private SettingsEntity mergeRecordIntoEntity(
        final SettingsRecord record,
        final SettingsEntity entity){


            final SettingsEntity newEntity = entity.toBuilder()
                .description(record.getDescription())
                .key(record.getKey())
                .value(record.getValue())
                .valueDataType(SettingsEntity.VALUE_DATA_TYPE.valueOf(record.getValueType()))
                .build();
            if(entity.getExternalId() == null){
                newEntity.setExternalId(record.getSettingsRecordId());
            }
            return newEntity;

    }*/
}