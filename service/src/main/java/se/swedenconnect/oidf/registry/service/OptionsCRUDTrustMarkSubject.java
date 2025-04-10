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
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.SettingDataType;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.errorhandling.RegistryClientException;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkSubjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.registry.entity.FkKeyType.TRUSTMARKSUBJECT;
import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.BAD_REQUEST;
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
public class OptionsCRUDTrustMarkSubject extends BaseOptionsCRUD {

  private static final FkKeyType FK_KEY_TYPE = TRUSTMARKSUBJECT;

  private final TrustMarkRepository trustMarkRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;

  /**
   * Constructs an instance of OptionsCRUDTrustMarkSubject.
   *
   * @param organizationService the service responsible for handling organization-related operations
   * @param settingsRepository the repository for accessing and managing settings data
   * @param trustMarkRepository the repository for accessing and managing trust mark data
   * @param trustMarkSubjectRepository the repository for accessing and managing trust mark subject data
   */
  public OptionsCRUDTrustMarkSubject(
      final OrganizationService organizationService,
      final SettingsRepository settingsRepository,
      final TrustMarkRepository trustMarkRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository) {
    super(settingsRepository, organizationService);
    this.trustMarkRepository = trustMarkRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return FK_KEY_TYPE == fkKeyType;
  }

  @Transactional
  @Override
  public OptionsRecord create(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<TrustMarkSubjectEntity> trustMarkEntity = this.trustMarkSubjectRepository.findById(id);

    if (trustMarkEntity.isPresent()) {
      super.throwNotFoundIfNotMatch(organizationRecord, trustMarkEntity.get()
          .getTrustMark()
          .getModule()
          .getOrganization()
          .getOrganizationId());
      throw new RegistryClientException(CONFLICT,
          "TrustMark already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);
    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, record.getOption());

    // Create
    final TrustMarkSubjectEntity newTrustMarkSubjectEntity = new TrustMarkSubjectEntity();
    newTrustMarkSubjectEntity.setTrustmarksubjectId(id);
    newTrustMarkSubjectEntity.setTrustMark(this.loadTrustMarkIDThrowIfNotExist(organizationRecord, validatedInData));

    final TrustMarkSubjectEntity saved = this.trustMarkSubjectRepository.saveAndFlush(newTrustMarkSubjectEntity);
    super.deleteSettings(fkKeyType, saved.getTrustmarksubjectId().toString());
    super.insertSettings(fkKeyType, saved.getTrustmarksubjectId().toString(), validatedInData);

    return this.toRecord(validatedInData);
  }

  protected TrustMarkEntity loadTrustMarkIDThrowIfNotExist(final OrganizationRecord organizationRecord,
      final List<SettingsEntity> dataValues)
      throws ResponseStatusException {

    return dataValues.stream()
        .filter(value -> value.getKey().equals("trustmark_id"))
        .map(SettingsEntity::getValue)
        .map(UUID::fromString)
        .map(this.trustMarkRepository::findById)
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid trustmark_id, does not exist")))
        .peek(trustMarkEntity ->
            super.throwNotFoundIfNotMatch(organizationRecord,
                trustMarkEntity.getModule().getOrganization().getOrganizationId()))
        .findFirst()
        .orElseThrow(() -> new RegistryClientException(BAD_REQUEST,
            "No trustmark to assign trustmarkssubjects to"));
  }

  @Override
  public OptionsRecord update(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {

    final TrustMarkSubjectEntity entity = this.trustMarkSubjectRepository
        .findById(id)
        .orElseThrow(() -> new RegistryClientException(NOT_FOUND,
            "No template found for:%s %s".formatted(fkKeyType, id)));

    super.throwNotFoundIfNotMatch(organizationRecord,
        entity.getTrustMark().getModule().getOrganization().getOrganizationId());
    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);

    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, record.getOption());
    super.deleteSettings(fkKeyType, entity.getTrustmarkId().toString());
    super.insertSettings(fkKeyType, entity.getTrustmarkId().toString(), validatedInData);

    this.trustMarkSubjectRepository.saveAndFlush(entity);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord get(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {
    final TrustMarkSubjectEntity entity = this.trustMarkSubjectRepository
        .findById(id)
        .orElseThrow(() -> new RegistryClientException(NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwNotFoundIfNotMatch(organizationRecord,
        entity.getTrustMark().getModule().getOrganization().getOrganizationId());

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(organizationRecord,
        fkKeyType,
        super.getSettingsEntities(FK_KEY_TYPE, entity.getTrustmarkId()));

    final OptionsRecord optionsRecord = toRecord(mergeValues);
    return optionsRecord;

  }

  @Override
  public OptionsRecord template(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    final OptionsRecord optionsRecord = toRecord(getTemplateSettings(organizationRecord, fkKeyType));
    this.addOptionsForTrustMarkId(organizationRecord, Objects.requireNonNull(optionsRecord.getOption()));
    return optionsRecord;
  }

  protected void addOptionsForTrustMarkId(final OrganizationRecord organizationRecord, final List<Values> values) {
    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), "trustmark_id"))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.trustMarkRepository.findAll()
                .stream()
                .filter(super.hasRightOrganizationIdTrustmarkPredicate(organizationRecord))
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getTrustmarkId().toString())
                        .value(entity
                            .getSettingsEntity("trust_mark_entity_id")
                            .orElseThrow().getValue())
                        .selected(Objects.equals(value.getValue(), entity.getTrustmarkId().toString()))
                        .build())
                .toList()));
  }

  @Override
  @Transactional
  public OptionsRecord delete(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType, final UUID id) {
    final TrustMarkEntity trustMarkEntity = this.trustMarkRepository
        .findById(id)
        .orElseThrow(() -> new RegistryClientException(NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwNotFoundIfNotMatch(organizationRecord,
        trustMarkEntity.getModule().getOrganization().getOrganizationId());
    final List<SettingsEntity> deletedSettings =
        super.deleteSettings(fkKeyType, trustMarkEntity.getTrustmarkId().toString());
    this.trustMarkRepository.delete(trustMarkEntity);
    this.trustMarkRepository.flush();
    return this.toRecord(deletedSettings);
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    return this.trustMarkRepository.findAll()
        .stream()
        .filter(entity -> Objects.equals(entity.getModule().getOrganization().getOrganizationId(),
            getCurrentOrganization(organizationRecord).getOrganizationId()))
        .map(entity -> {
          final Map<String, Object> e = super.getSettingsEntities(fkKeyType, entity.getTrustmarkId())
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

