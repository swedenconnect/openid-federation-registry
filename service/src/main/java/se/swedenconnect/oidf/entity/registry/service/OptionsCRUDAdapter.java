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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidator;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
public abstract class OptionsCRUDAdapter implements OptionsCRUD {
  private final RegistryAuditService registryAuditService;
  private final InstanceRepository instanceRepository;
  private final SettingsRepository settingsRepository;
  private final PropertyValidators validatorFactory = new PropertyValidators();

  protected OptionsCRUDAdapter(final RegistryAuditService registryAuditService,
      final InstanceRepository instanceRepository,
      final SettingsRepository settingsRepository) {
    this.registryAuditService = registryAuditService;
    this.instanceRepository = instanceRepository;
    this.settingsRepository = settingsRepository;
  }

  protected OptionsRecord toRecord(final List<SettingsEntity> entities) {
    return OptionsRecord.builder()
        .option(entities.stream()
            .map(entity -> Values.builder()
                .settingDescription(entity.getDescription())
                .validation(entity.getValidation())
                .key(entity.getKey())
                .value(entity.getValue())
                .valueType(entity.getValueDataType())
                .options(null)
                .build()).toList())
        .build();
  }

  protected void addOptionsForInstanceID(final List<Values> values) {
    values.stream()
        .filter(value -> value.getValueType().equals(SettingDataType.OPTIONS.name()))
        .filter(value -> value.getKey().equals("instance_id"))
        .findFirst()
        .ifPresent(value -> {
          value.setOptions(this.instanceRepository
              .findAll()//ToDo: Filter out instance according to your organization regulation
              .stream()
              .map(instanceEntity ->
                  OptionRecord.builder()
                      .key(instanceEntity.getInstanceId().toString())
                      .value(instanceEntity.getName())
                      .selected(value.getValue().equals(instanceEntity.getInstanceId()))
                      .build())
              .toList());
        });
  }

  protected List<SettingsEntity> getTemplateSettings(final FkKeyType fkkeytype) {
    final List<SettingsEntity> templates = this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), "TEMPLATE");
    if (templates.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No template found for:%s".formatted(fkkeytype));
    }
    return templates;
  }

  protected Optional<InstanceEntity> loadInstanceThrowIfNotExist(final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    // todo make sure to check for organization
    return dataValues.stream()
        .filter(value -> value.getKey().equals("instance_id"))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(this.instanceRepository::findById)
        .map(instanceEntity -> instanceEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid instance_id, does not exist in registry")))
        .findFirst();
  }

  protected List<SettingsEntity> insertValuesInTemplate(final FkKeyType fkkeytype,
      final List<SettingsEntity> dataValues) {

    final List<SettingsEntity> templateValues = this.getTemplateSettings(fkkeytype);

    return templateValues
        .stream()
        .map(templateValue ->
            dataValues.stream()
                .filter(dataValue -> dataValue.getKey().equals(templateValue.getKey()))
                .map(dataValue -> {
                  templateValue.setValue(dataValue.getValue());
                  templateValue.setValueDataType(dataValue.getValueDataType());
                  return templateValue;
                })
                .findFirst()
                .orElse(null)
        )
        .filter(Objects::nonNull)
        .toList();
  }

  protected List<SettingsEntity> createAndValidateInputData(final List<SettingsEntity> templateValues,
      final List<Values> dataValues) {

    return templateValues
        .stream()
        .map(templateValue ->
            dataValues.stream()
                .filter(dataValue -> dataValue.getKey().equals(templateValue.getKey()))
                .map(dataValue -> {
                  final PropertyValidator validator =
                      this.validatorFactory.resolveValidator(templateValue.getValidation());
                  validator.validate(templateValue.getKey(), dataValue.getValue());
                  return SettingsEntity.builder()
                      .key(dataValue.getKey())
                      .value(dataValue.getValue())
                      .valueDataType(dataValue.getValueType())
                      .build();
                })
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized key input:%s"
                    .formatted(dataValues.stream().map(Values::getKey).filter(Objects::nonNull)
                        .reduce("%s,%s"::formatted).orElse(""))))
        ).toList();
  }

  protected void deleteInsertSettings(final FkKeyType fkkeytype, final String id,
      final List<SettingsEntity> settingsEntities) {
    this.settingsRepository.deleteAllInBatch(this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), id));
    settingsEntities.forEach(settingsEntity -> {
      settingsEntity.setFkId(id);
      settingsEntity.setFkType(fkkeytype.name());
    });
    this.settingsRepository.saveAllAndFlush(settingsEntities);
  }

  protected List<SettingsEntity> getSettingsEntities(final FkKeyType fkkeytype, final String id) {
    return this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), id);
  }

}
