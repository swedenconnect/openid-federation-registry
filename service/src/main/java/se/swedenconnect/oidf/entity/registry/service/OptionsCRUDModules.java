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
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

  /**
   * Constructor for the OptionsCRUDModules class. This initializes the module with the required dependencies for
   * performing CRUD operations.
   *
   * @param registryAuditService The service responsible for auditing registry actions.
   * @param repository The settings repository used for storing and retrieving settings data.
   * @param moduleRepository The module repository used for managing module entities.
   * @param instanceRepository The instance repository used for handling instance data.
   */
  public OptionsCRUDModules(final RegistryAuditService registryAuditService,
      final SettingsRepository repository,
      final ModuleRepository moduleRepository,
      final InstanceRepository instanceRepository) {
    super(registryAuditService, instanceRepository, repository);
    this.moduleRepository = moduleRepository;
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
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<ModuleEntity> moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name());

    if (moduleEntity.isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Module already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());

    // Create
    final ModuleEntity newModuleEntity = new ModuleEntity();
    newModuleEntity.setModuleId(id);
    newModuleEntity.setModuleType(fkKeyType.name());
    this.loadInstanceThrowIfNotExist(validatedInData).ifPresent(newModuleEntity::setInstance);

    final ModuleEntity savedModuleEntity = this.moduleRepository.saveAndFlush(newModuleEntity);
    super.deleteInsertSettings(fkKeyType, savedModuleEntity.getModuleId().toString(), validatedInData);

    return this.toRecord(validatedInData);

  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    //this.loadInstanceThrowIfNotExist(validatedInData).ifPresent(moduleEntity::setInstance);
    super.deleteInsertSettings(fkKeyType, moduleEntity.getModuleId().toString(), validatedInData);
    final ModuleEntity savedModuleEntity = this.moduleRepository.saveAndFlush(moduleEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType, moduleEntity.getSettingsEntityList());
    final OptionsRecord optionsRecord = toRecord(mergeValues);
    this.addOptionsForInstanceID(optionsRecord.getOption());
    //this.validateEntityIdentifier(fkKeyType, optionsRecord.getOption());
    return optionsRecord;

  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    final OptionsRecord optionsRecord = toRecord(getTemplateSettings(fkKeyType));
    addOptionsForInstanceID(optionsRecord.getOption());
    return optionsRecord;
  }

  @Override
  public void delete(final FkKeyType fkKeyType, final UUID id) {
    final ModuleEntity moduleEntity = this.moduleRepository
        .findByModuleIdAndModuleType(id, fkKeyType.name())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    //this.repository.deleteAllInBatch(moduleEntity.get().getSettingsEntityList());
    this.moduleRepository.delete(moduleEntity);
  }

}

