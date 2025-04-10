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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecordMetadata;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.BaseEntity;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.validation.PropertyValidator;
import se.swedenconnect.oidf.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.validation.VariabelValueResolver;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.BAD_REQUEST;
import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.NOT_FOUND;

/**
 * Abstract implementation of the {@link OptionsCRUD} interface providing common functionality for managing and
 * manipulating settings, templates, and related data records. This adapter simplifies CRUD operations by offering
 * reusable utility methods for transforming, validating, and persisting records.
 *
 * @author Per Fredrik Plars
 */
public abstract class BaseOptionsCRUD implements OptionsCRUD {
  private final SettingsRepository settingsRepository;
  private final PropertyValidators validatorFactory = new PropertyValidators();
  private final OrganizationService organizationService; //TODO remove this from class
  private final ZoneOffset offset = ZoneOffset.UTC;

  protected BaseOptionsCRUD(
      final SettingsRepository settingsRepository,
      final OrganizationService organizationService) {
    this.settingsRepository = settingsRepository;
    this.organizationService = organizationService;
  }

  protected OrganizationEntity getCurrentOrganization(final OrganizationRecord organizationRecord) {
    return Optional.ofNullable(this.organizationService.findCreate(organizationRecord.orgNumber(),
            organizationRecord.orgName()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No organization assigned"));
  }

  protected OptionsRecord toRecord(final List<SettingsEntity> entities) {
    final OptionsRecord.Builder optionsRecord = OptionsRecord.builder()
        .option(entities.stream()
            .map(entity -> Values.builder()
                .settingDescription(entity.getDescription())
                .validation(entity.getValidation())
                .key(entity.getKey())
                .value(entity.getValue())
                .valueType(entity.getValueDataType())
                .options(null)
                .build())
            .toList());

    entities.stream()
        .max(Comparator.comparing(BaseEntity::getLastModifiedDate))
        .ifPresent(baseEntity -> {
          optionsRecord.metadata(OptionsRecordMetadata.builder()
              .changeby(baseEntity.getLastModifiedBy())
              .createdby(baseEntity.getCreatedBy())
              .changedate(OffsetDateTime.of(baseEntity.getLastModifiedDate(), this.offset))
              .createddate(OffsetDateTime.of(baseEntity.getCreatedDate(), this.offset))
              .build());
        });

    return optionsRecord.build();
  }

  protected List<SettingsEntity> getTemplateSettings(final OrganizationRecord organizationRecord,
      final FkKeyType fkkeytype) {
    final List<SettingsEntity> templates = this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), "TEMPLATE");
    if (templates.isEmpty()) {
      throw new RegistryServerException(NOT_FOUND,
          "No template found for:%s".formatted(fkkeytype));
    }
    final VariabelValueResolver valueResolver = VariabelValueResolver.orgResolver(organizationRecord);
    templates.forEach(settingsEntity ->
        settingsEntity.setValue(valueResolver.insertTemplateValues(settingsEntity.getValue())));
    return templates;
  }

  protected Map<String, Object> getStringObjectMap(final FkKeyType fkKeyType, final UUID id) {
    final Map<String, Object> e = this.getSettingsEntities(fkKeyType, id)
        .stream()
        .collect(Collectors.toMap(
            SettingsEntity::getKey,
            SettingsEntity::castValue
        ));
    e.put("id", id.toString());
    return e;
  }

  @Override
  public OptionsRecord template(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {

    return this.toRecord(this.getTemplateSettings(organizationRecord, fkKeyType));
  }

  protected List<SettingsEntity> insertValuesInTemplate(final OrganizationRecord organizationRecord,
      final FkKeyType fkkeytype,
      final List<SettingsEntity> dataValues) {

    final List<SettingsEntity> templateValues = this.getTemplateSettings(organizationRecord, fkkeytype);
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

  protected List<SettingsEntity> createAndValidateInputData(final OrganizationRecord organizationRecord,
      final List<SettingsEntity> templateValues,
      final List<Values> dataValues) {

    final Map<String, Values> dataMap = dataValues.stream().collect(Collectors.toMap(Values::getKey, v -> v));

    return templateValues
        .stream()
        .map(templateValue -> {
          final Values dataValue = dataMap.get(templateValue.getKey());
          final String valueToBeValidated = dataValue != null ? dataValue.getValue() : null;

          final PropertyValidator validator =
              this.validatorFactory.resolveValidator(templateValue.getValidation(),
                  VariabelValueResolver.orgResolver(organizationRecord));

          validator.validate(templateValue.getKey(), valueToBeValidated);

          if (dataValue == null) {
            return null;
          }

          return SettingsEntity.builder()
              .key(templateValue.getKey())
              .value(valueToBeValidated)
              .valueDataType(templateValue.getValueDataType())
              .build();

        })
        .filter(Objects::nonNull)
        .toList();
  }

  protected void ruleIssuerAndSubjectTheSameOrTrowException(final EntityEntity entityEntity) {
    final String issuer = entityEntity.getIssuer();
    final String subject = entityEntity.getSubject();
    if (!issuer.equalsIgnoreCase(subject)) {
      throw new RegistryServerException(BAD_REQUEST,
          ("Issuer and subject must be the same on the entity that this module will "
              + "be mounted to. Issuer: %s, Subject: %s").formatted(issuer, subject));
    }
  }

  protected List<SettingsEntity> deleteSettings(final FkKeyType fkkeytype, final String id) {
    final List<SettingsEntity> entities = this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), id);
    this.settingsRepository.deleteAllInBatch(entities);
    return entities;
  }

  protected List<SettingsEntity> insertSettings(final FkKeyType fkkeytype,
      final String id,
      final List<SettingsEntity> settingsEntities) {
    settingsEntities.forEach(settingsEntity -> {
      settingsEntity.setFkId(id);
      settingsEntity.setFkType(fkkeytype.name());
    });
    return this.settingsRepository.saveAllAndFlush(settingsEntities);
  }

  protected List<SettingsEntity> getSettingsEntities(final FkKeyType fkkeytype, final UUID id) {
    return this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), id.toString());
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    return Collections.emptyList();
  }
}
