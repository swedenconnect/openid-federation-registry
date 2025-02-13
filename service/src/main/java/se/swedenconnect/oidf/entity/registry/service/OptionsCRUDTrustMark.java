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
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.entity.registry.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.TRUSTMARK;

/**
 * OptionsCRUDTrustMark is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDTrustMark extends OptionsCRUDAdapter {

  private final TrustMarkRepository trustMarkRepository;
  private final ModuleRepository moduleRepository;

  /**
   * Constructor for OptionsCRUDTrustMark.
   *
   * @param instanceRepository the repository for managing instances.
   * @param settingsRepository the repository for system settings.
   * @param trustMarkRepository the repository for handling trust marks.
   * @param moduleRepository the repository for managing modules.
   */
  public OptionsCRUDTrustMark(
      final InstanceRepository instanceRepository, final SettingsRepository settingsRepository,
      final TrustMarkRepository trustMarkRepository, final ModuleRepository moduleRepository) {
    super(instanceRepository, settingsRepository);
    this.trustMarkRepository = trustMarkRepository;
    this.moduleRepository = moduleRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return TRUSTMARK == fkKeyType;
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<TrustMarkEntity> trustMarkEntity = this.trustMarkRepository.findById(id);

    if (trustMarkEntity.isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "TrustMark already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());

    // Create
    final TrustMarkEntity newTrustMarkEntity = new TrustMarkEntity();
    newTrustMarkEntity.setTrustmarkId(id);
    this.loadModuleThrowIfNotExist(validatedInData).ifPresent(newTrustMarkEntity::setModule);

    final TrustMarkEntity savedTrustMarkEntity = this.trustMarkRepository.saveAndFlush(newTrustMarkEntity);
    super.deleteSettings(fkKeyType, savedTrustMarkEntity.getTrustmarkId().toString());
    super.insertSettings(fkKeyType, savedTrustMarkEntity.getTrustmarkId().toString(), validatedInData);

    return this.toRecord(validatedInData);
  }

  protected Optional<ModuleEntity> loadModuleThrowIfNotExist(final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    // todo make sure to check for organization
    return dataValues.stream()
        .filter(value -> value.getKey().equals("trustmarkissuer_id"))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(s -> this.moduleRepository.findByModuleIdAndModuleType(s, FkKeyType.TRUSTMARKISSUER.name()))
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid module_id, does not exist")))
        .findFirst();
  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    super.deleteSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString());
    super.insertSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString(), validatedInData);

    this.trustMarkRepository.saveAndFlush(trustMarkEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType,
        super.getSettingsEntities(TRUSTMARK, trustMarkEntity.getTrustmarkId().toString()));

    final OptionsRecord optionsRecord = toRecord(mergeValues);
    this.addOptionsForInstanceID(Objects.requireNonNull(optionsRecord.getOption()));
    return optionsRecord;

  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    final OptionsRecord optionsRecord = toRecord(getTemplateSettings(fkKeyType));
    this.addOptionsForTrustMarkIssuerId(Objects.requireNonNull(optionsRecord.getOption()));
    return optionsRecord;
  }

  protected void addOptionsForTrustMarkIssuerId(final List<Values> values) {
    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), "trustmarkissuer_id"))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.moduleRepository.findByModuleType(FkKeyType.TRUSTMARKISSUER.name())
                .stream()
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getModuleId().toString())
                        .value(entity.getSettingsEntity("entity-identifier").orElseThrow().getValue())
                        .selected(Objects.equals(value.getValue(), entity.getModuleId().toString()))
                        .build())
                .toList()));
  }

  @Override
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    final List<SettingsEntity> deletedSettings =
        super.deleteSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString());
    this.trustMarkRepository.delete(trustMarkEntity);
    this.trustMarkRepository.flush();
    return this.toRecord(deletedSettings);
  }

  @Override
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {
    return this.trustMarkRepository.findAll()
        .stream()
        .map(entity -> {
              final Map<String, Object> e = super.getSettingsEntities(TRUSTMARK, entity.getTrustmarkId().toString())
                  .stream()
                  .collect(Collectors.toMap(
                      SettingsEntity::getKey,
                      SettingsEntity::castValue
                  ));
              e.put("id", entity.getTrustmarkId().toString());
              return e;
            }
        )
        .toList();
  }

}

