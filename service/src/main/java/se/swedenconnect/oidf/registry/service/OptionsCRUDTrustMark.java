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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.*;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;

import java.util.*;

import static se.swedenconnect.oidf.registry.entity.FkKeyType.TRUSTMARK;
import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.CONFLICT;
import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.NOT_FOUND;

/**
 * OptionsCRUDTrustMark is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDTrustMark extends BaseOptionsCRUD {

  private final TrustMarkRepository trustMarkRepository;
  private final ModuleRepository moduleRepository;

  /**
   * Constructor for creating an instance of OptionsCRUDTrustMark.
   *
   * @param organizationService An instance of OrganizationService used to handle organization-related operations.
   * @param settingsRepository An instance of SettingsRepository used to manage application settings.
   * @param trustMarkRepository An instance of TrustMarkRepository used to handle TrustMark-related data interactions.
   * @param moduleRepository An instance of ModuleRepository used to manage module-related data.
   */
  public OptionsCRUDTrustMark(
      final OrganizationService organizationService,
      final SettingsRepository settingsRepository,
      final TrustMarkRepository trustMarkRepository,
      final ModuleRepository moduleRepository) {
    super(settingsRepository, organizationService);
    this.trustMarkRepository = trustMarkRepository;
    this.moduleRepository = moduleRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return TRUSTMARK == fkKeyType;
  }

  @Override
  public OptionsRecord create(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<TrustMarkEntity> trustMarkEntity = this.trustMarkRepository.findByOrgNumberAndTrustmarkId(
        organizationRecord.orgNumber(), id);

    if (trustMarkEntity.isPresent()) {
      throw new RegistryServerException(CONFLICT,
          "TrustMark already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);
    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, Objects.requireNonNull(record.getOption()));

    // Create
    final TrustMarkEntity newTrustMarkEntity = new TrustMarkEntity();
    newTrustMarkEntity.setTrustmarkId(id);
    newTrustMarkEntity.setModule(this.loadModuleThrowIfNotExist(organizationRecord, validatedInData));

    final TrustMarkEntity savedTrustMarkEntity = this.trustMarkRepository.saveAndFlush(newTrustMarkEntity);
    super.deleteSettings(fkKeyType, savedTrustMarkEntity.getTrustmarkId().toString());
    super.insertSettings(fkKeyType, savedTrustMarkEntity.getTrustmarkId().toString(), validatedInData);

    return this.toRecord(validatedInData);
  }

  protected ModuleEntity loadModuleThrowIfNotExist(final OrganizationRecord organizationRecord,
      final List<SettingsEntity> dataValues) throws ResponseStatusException {

    return dataValues.stream()
        .filter(value -> value.getKey().equals("trustmarkissuer_id"))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(s -> this.moduleRepository.findByOrgNumberAndModuleIdAndModuleType(
            organizationRecord.orgNumber(), s, FkKeyType.TRUSTMARKISSUER.name()))
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new RegistryServerException(NOT_FOUND,
                "module_id, does not exist")))
        .findFirst()
        .orElseThrow(() -> new RegistryServerException(NOT_FOUND,
            "No trustmarkissuer to assign trustmarks to"));
  }

  @Override
  public OptionsRecord update(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);

    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, Objects.requireNonNull(record.getOption()));
    super.deleteSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString());
    super.insertSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString(), validatedInData);

    this.trustMarkRepository.saveAndFlush(trustMarkEntity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType, final UUID id) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(organizationRecord,
        fkKeyType,
        super.getSettingsEntities(fkKeyType, trustMarkEntity.getTrustmarkId()));

    return toRecord(mergeValues);
  }

  @Override
  public OptionsRecord template(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    final OptionsRecord optionsRecord = super.template(organizationRecord, fkKeyType);
    this.addOptionsForTrustMarkIssuerId(organizationRecord, Objects.requireNonNull(optionsRecord.getOption()));
    return optionsRecord;
  }

  protected void addOptionsForTrustMarkIssuerId(final OrganizationRecord organizationRecord,
      final List<Values> values) {
    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), "trustmarkissuer_id"))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.moduleRepository
                .findByOrgNumberAndModuleType(organizationRecord.orgNumber(), FkKeyType.TRUSTMARKISSUER.name())
                .stream()
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getModuleId().toString())
                        .value(entity.getEntity().getSettingsEntity("issuer").orElseThrow().getValue())
                        .selected(Objects.equals(value.getValue(), entity.getModuleId().toString()))
                        .build())
                .toList()));
  }

  @Override
  @Transactional
  public OptionsRecord delete(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType, final UUID id) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> deletedSettings =
        super.deleteSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString());
    this.trustMarkRepository.delete(trustMarkEntity);
    this.trustMarkRepository.flush();
    return this.toRecord(deletedSettings);
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    return this.trustMarkRepository.findByOrgNumber(organizationRecord.orgNumber())
        .stream()
        .map(entity -> super.getStringObjectMap(fkKeyType, entity.getTrustmarkId()))
        .toList();
  }

}

