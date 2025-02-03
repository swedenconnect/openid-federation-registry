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
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidator;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service class for handling CRUD operations for SettingsEntity.
 * @author Per Fredrik Plars
 */
@Slf4j
public class JpaOptionsService {

  private final SettingsRepository repository;
  private final ModuleRepository moduleRepository;
  private final RegistryAuditService registryAuditService;
  private final InstanceRepository instanceRepository;
  private final PropertyValidators validatorFactory = new PropertyValidators();

  /**
   * Constructs a new instance of {@code JpaOptionsService} with the specified dependencies.
   *
   * @param repository the {@link SettingsRepository} used to interact with settings data.
   * @param moduleRepository the {@link ModuleRepository} used for interacting with module entities.
   * @param registryAuditService the {@link RegistryAuditService} used for auditing operations related to the
   *     registry.
   * @param instanceRepository the {@link InstanceRepository} used for managing instance data.
   */
  public JpaOptionsService(final SettingsRepository repository, final ModuleRepository moduleRepository,
      final RegistryAuditService registryAuditService, final InstanceRepository instanceRepository) {
    this.repository = repository;
    this.moduleRepository = moduleRepository;
    this.registryAuditService = registryAuditService;
    this.instanceRepository = instanceRepository;
  }

  /**
   * Creates or updates a record in the system by either persisting it as a new entity
   * or replacing existing settings for an existing entity based on the provided inputs.
   * The operation is determined by the presence of an existing entity with the specified ID and type.
   *
   * @param fkKeyType the type of the foreign key that specifies the category of the module entity.
   * @param id the unique identifier of the external entity to either link or create a module for.
   * @param record the options data to associate with the entity, including configuration settings.
   * @return an {@link OptionsRecord} containing validated and stored options data.
   * @throws ResponseStatusException if no template is found for the given foreign key type and ID,
   *         or if any integrity conflicts occur during data processing.
   */
  public OptionsRecord create(final FkKeyType fkKeyType, final String id, final OptionsRecord record) {

    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByExternalIdAndModuleType(id, fkKeyType.name());

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    if (template.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No template found for:%s %s".formatted(fkKeyType, id));
    }
    final List<SettingsEntity> validatedInData = this.mergeValidateInputData(template, record.getOption());

    if (moduleEntity.isEmpty()) {
      // Create
      final ModuleEntity newModuleEntity = new ModuleEntity();
      newModuleEntity.setExternalId(id);
      newModuleEntity.setModuleType(fkKeyType.name());
      this.validateDataIntegrityForInstanceIDIfExist(validatedInData).ifPresent(newModuleEntity::setInstance);

      final ModuleEntity savedModuleEntity = this.moduleRepository.save(newModuleEntity);
      validatedInData.forEach(settingsEntity -> {
        settingsEntity.setFkId(savedModuleEntity.getModuleId());
        settingsEntity.setFkType(savedModuleEntity.getModuleType());
      });

      this.repository.saveAllAndFlush(validatedInData);
      return this.toRecord(validatedInData);
    }

    validatedInData.forEach(settingsEntity -> {
      settingsEntity.setFkId(moduleEntity.get().getModuleId());
      settingsEntity.setFkType(moduleEntity.get().getModuleType());
    });
    this.repository.deleteAllInBatch(moduleEntity.get().getSettingsEntityList());

    this.repository.saveAllAndFlush(validatedInData);

    return this.toRecord(validatedInData);
  }

  /**
   * Retrieves an OptionsRecord based on the given foreign key type and identifier.
   *
   * This method fetches the module entity associated with the specified foreign key
   * type and ID. If no matching entity is found, an exception is thrown. The method
   * then merges template and data values, validates the entity identifier, and
   * constructs an OptionsRecord with the associated data.
   *
   * @param fkKeyType the foreign key type used to find a corresponding module entity.
   * @param id the unique identifier used to locate the module entity.
   * @return an OptionsRecord containing the merged data and template options.
   * @throws ResponseStatusException if no module entity is found for the given ID and key type.
   */
  public OptionsRecord get(final FkKeyType fkKeyType, final String id) {
    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByExternalIdAndModuleType(id, fkKeyType.name());
    if (moduleEntity.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for:%s %s".formatted(fkKeyType, id));
    }
    final OptionsRecord template = this.getTemplate(fkKeyType);
    final OptionsRecord data = this.toRecord(moduleEntity.get().getSettingsEntityList());
    final List<Values> values = this.mergeValues(template.getOption(), data.getOption());
    this.addOptionsForInstanceID(values);
    this.validateEntityIdentifier(fkKeyType, values);

    return OptionsRecord.builder()
        .option(values)
        .build();
  }

  private void validateEntityIdentifier(final FkKeyType fkKeyType, final List<Values> values) {
    final Optional<Values> entityIdentifyer = values.stream()
        .filter(value -> value.getKey().equals("entity_identifier"))
        .findFirst();

    if (entityIdentifyer.isEmpty()) {
      return;
    }
    // Todo
    // Lookup your org
    // make sure selected entityid is the same
  }

  private void addOptionsForInstanceID(final List<Values> values) {
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
                      .key(instanceEntity.getInstanceId())
                      .value(instanceEntity.getName())
                      .selected(value.getValue().equals(instanceEntity.getInstanceId()))
                      .build())
              .toList());
        });
  }

  /**
   * Retrieves a template as an {@code OptionsRecord} for the given {@code FkKeyType}.
   * The method fetches template settings, converts them into an {@code OptionsRecord}, and enhances it
   * with additional options specific to the instance ID.
   *
   * @param fkKeyType the foreign key type that determines which template settings are fetched and converted
   * @return an {@code OptionsRecord} containing the template settings and associated options
   */
  public OptionsRecord getTemplate(final FkKeyType fkKeyType) {
    final OptionsRecord optionsRecord =
        this.toRecord(this.getTemplateSettings(fkKeyType));
    this.addOptionsForInstanceID(optionsRecord.getOption());
    return optionsRecord;
  }

  /**
   * Retrieves a list of template settings based on the specified foreign key type.
   *
   * @param FKKEYTYPE the type of the foreign key, represented as an enumeration of {@code FkKeyType}.
   * @return a list of {@code SettingsEntity} objects corresponding to the template settings for the given
   * foreign key type.
   */
  public List<SettingsEntity> getTemplateSettings(final FkKeyType FKKEYTYPE) {
    return this.repository.findByFkTypeAndFkId(FKKEYTYPE.name(), "TEMPLATE");
  }

  private List<Values> mergeValues(final List<Values> templateValues, final List<Values> dataValues) {
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

  private List<SettingsEntity> mergeValidateInputData(final List<SettingsEntity> templateValues,
      final List<Values> dataValues) {
    return templateValues
        .stream()
        .map(templateValue ->
            dataValues.stream()
                .filter(dataValue -> dataValue.getKey().equals(templateValue.getKey()))
                .map(dataValue -> {
                  final PropertyValidator validator = this.validatorFactory.resolveValidator(dataValue.getValidation());
                  validator.validate(dataValue.getKey(), dataValue.getValue());
                  return SettingsEntity.builder()
                      .key(dataValue.getKey())
                      .value(dataValue.getValue())
                      .build();
                })
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized key input:%s"
                    .formatted(dataValues.stream().map(Values::getKey).filter(Objects::nonNull)
                        .reduce("%s,%s"::formatted).orElse(""))) )
        ).toList();
  }

  private Optional<InstanceEntity> validateDataIntegrityForInstanceIDIfExist(final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    // todo make sure to check for organization
    return dataValues.stream()
        .filter(value -> value.getKey().equals("instance_id"))
        .map(SettingsEntity::getValue)
        .map(this.instanceRepository::findById)
        .map(instanceEntity -> instanceEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid instance_id, does not exist in registry")))
        .findFirst();
  }

  private OptionsRecord toRecord(final List<SettingsEntity> entities) {
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

  /**
   * Deletes a module entity and its associated settings based on the provided foreign key type and identifier.
   *
   * @param fkKeyType the type of the foreign key used to identify the module entity.
   * @param identifyer the unique identifier corresponding to the module entity to be deleted.
   * @throws ResponseStatusException if no module entity matching the specified foreign key type and identifier is
   *     found.
   */
  public void delete(final FkKeyType fkKeyType, final String identifyer) {

    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByExternalIdAndModuleType(identifyer, fkKeyType.name());
    if (moduleEntity.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for:%s %s"
          .formatted(fkKeyType, identifyer));
    }
    this.repository.deleteAllInBatch(moduleEntity.get().getSettingsEntityList());
    this.moduleRepository.delete(moduleEntity.get());
  }
}