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
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidator;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Abstract implementation of the {@link OptionsCRUD} interface providing common functionality for managing and
 * manipulating settings, templates, and related data records. This adapter simplifies CRUD operations by offering
 * reusable utility methods for transforming, validating, and persisting records.
 *
 * @author Per Fredrik Plars
 */
public abstract class OptionsCRUDAdapter implements OptionsCRUD {

  private final SettingsRepository settingsRepository;
  private final PropertyValidators validatorFactory = new PropertyValidators();
  private final Supplier<OrganizationEntity> userAssignedOrganization;
  protected OptionsCRUDAdapter(
      final SettingsRepository settingsRepository,
      final Supplier<OrganizationEntity> userAssignedOrganization) {
    this.settingsRepository = settingsRepository;
    this.userAssignedOrganization = userAssignedOrganization;
  }

  protected OrganizationEntity getCurrentOrganization() {
    return Optional.ofNullable(this.userAssignedOrganization.get())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No organization assigned"));
  }

  protected Predicate<PolicyEntity> hasRightOrganizationIdPolicyPredicate() {
    return entity -> Objects.equals(this.getCurrentOrganization().getOrganizationId(),
        entity.getOrganization().getOrganizationId());
  }

  protected Predicate<ModuleEntity> hasRightOrganizationIdModulePredicate() {
    return entity -> Objects.equals(this.getCurrentOrganization().getOrganizationId(),
        entity.getOrganization().getOrganizationId());
  }

  protected Predicate<EntityEntity> hasRightOrganizationIdEntityPredicate() {
    return entity -> Objects.equals(this.getCurrentOrganization().getOrganizationId(),
        entity.getOrganization().getOrganizationId());
  }

  protected Predicate<TrustMarkEntity> hasRightOrganizationIdTrustmarkPredicate() {
    return entity -> Objects.equals(this.getCurrentOrganization().getOrganizationId(),
        entity.getModule().getOrganization().getOrganizationId());
  }

  protected void throwUnauthorizedIfNotMatch(final UUID organizationId) {
    Optional.ofNullable(this.userAssignedOrganization.get())
        .map(OrganizationEntity::getOrganizationId)
        .filter(uuid -> uuid.equals(organizationId))
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Trying to alter data for other organization"));
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
                .build())
            .toList())
        .build();
  }


  protected List<SettingsEntity> getTemplateSettings(final FkKeyType fkkeytype) {
    final List<SettingsEntity> templates = this.settingsRepository.findByFkTypeAndFkId(fkkeytype.name(), "TEMPLATE");
    if (templates.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No template found for:%s".formatted(fkkeytype));
    }
    return templates;
  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    return this.toRecord(this.getTemplateSettings(fkKeyType));
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

    final Map<String, Values> dataMap = dataValues.stream().collect(Collectors.toMap(Values::getKey, v -> v));

    return templateValues
        .stream()
        .map(templateValue -> {
          final Values dataValue = dataMap.get(templateValue.getKey());
          final String valueToBeValidated = dataValue != null ? dataValue.getValue() : null;

          final PropertyValidator validator =
              this.validatorFactory.resolveValidator(templateValue.getValidation());
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {
    return Collections.emptyList();
  }
}
