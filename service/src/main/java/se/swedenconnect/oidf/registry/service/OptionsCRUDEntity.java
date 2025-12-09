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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.OptionChildren;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.*;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OptionsCRUDHostedEntity is a service class that extends OptionsCRUDAdapter to provide CRUD (Create, Read, Update,
 * Delete) operations specifically for entities classified with the FkKeyType.FEDERATION_ENTITY type.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OptionsCRUDEntity extends BaseOptionsCRUD {

  final EntityRepository entityRepository;
  final PolicyRepository policyRepository;

  /**
   * Constructor for OptionsCRUDEntity.
   *
   * @param settingsRepository the repository for managing application settings
   * @param organizationService the service handling organizational operations
   * @param entityRepository the repository for managing entity data
   * @param policyRepository the repository for managing policy data
   */
  public OptionsCRUDEntity(
      final SettingsRepository settingsRepository,
      final OrganizationService organizationService,
      final EntityRepository entityRepository,
      final PolicyRepository policyRepository) {
    super(settingsRepository, organizationService);
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return FkKeyType.FEDERATION_ENTITY == fkKeyType || FkKeyType.SUBORDINATE_ENTITY == fkKeyType;
  }

  @Override
  public OptionsRecord template(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    final OptionsRecord record = this.toRecord(this.getTemplateSettings(organizationRecord, fkKeyType));
    this.addOptionsForPolicyId(organizationRecord, Objects.requireNonNull(record.getOption()));
    return record;
  }

  private EntityKeyType getEntityKeyType(final FkKeyType fkKeyType) {
    return switch (fkKeyType) {
      case SUBORDINATE_ENTITY -> EntityKeyType.SUBORDINATE_ENTITY;
      case FEDERATION_ENTITY -> EntityKeyType.FEDERATION_ENTITY;
      default -> throw new IllegalArgumentException("Unsupported FkKeyType: %s".formatted(fkKeyType));
    };
  }

  @Override
  public OptionsRecord create(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id,
      final OptionsRecord record) {

    final EntityKeyType entityKeyType = this.getEntityKeyType(fkKeyType);
    final Optional<EntityEntity> entity = this.entityRepository
        .findByOrgNumberAndEntityIdAndEntityKeyType(organizationRecord.orgNumber(), id, entityKeyType);

    if (entity.isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Module already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);
    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, Objects.requireNonNull(record.getOption()));

    // Create
    final EntityEntity newEntity = new EntityEntity();
    newEntity.setEntityId(id);
    newEntity.setEntityType(entityKeyType);
    newEntity.setOrganization(super.getCurrentOrganization(organizationRecord));
    this.loadPolicyIfExist(organizationRecord, validatedInData).ifPresent(newEntity::setPolicyEntity);

    final EntityEntity savedEntity = this.entityRepository.saveAndFlush(newEntity);
    super.deleteSettings(fkKeyType, savedEntity.getEntityId().toString());
    super.insertSettings(fkKeyType, savedEntity.getEntityId().toString(), validatedInData);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord update(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType, final UUID id,
      final OptionsRecord record) {
    final EntityKeyType entityKeyType = this.getEntityKeyType(fkKeyType);
    final EntityEntity entity = this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), id, entityKeyType)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No entity found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> template = this.getTemplateSettings(organizationRecord, fkKeyType);

    final List<SettingsEntity> validatedInData =
        this.createAndValidateInputData(organizationRecord, template, Objects.requireNonNull(record.getOption()));
    this.loadPolicyIfExist(organizationRecord, validatedInData).ifPresent(entity::setPolicyEntity);

    this.ruleIfHostedEntityAndModulesTaOrImExistIssuerAndSubjectHasToBeTheSameOrThrow(entity, validatedInData);
    super.deleteSettings(fkKeyType, entity.getEntityId().toString());
    super.insertSettings(fkKeyType, entity.getEntityId().toString(), validatedInData);

    this.entityRepository.saveAndFlush(entity);

    final OptionsRecord updatedRecord = this.toRecord(validatedInData);
    this.addOptionsForPolicyId(organizationRecord, Objects.requireNonNull(updatedRecord.getOption()));
    return updatedRecord;
  }

  @Override
  public OptionsRecord get(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType, final UUID id) {
    final EntityKeyType entityKeyType = this.getEntityKeyType(fkKeyType);

    final EntityEntity entity = this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), id, entityKeyType)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No entity found for:%s %s".formatted(fkKeyType, id)));

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(organizationRecord,
        fkKeyType,
        super.getSettingsEntities(fkKeyType, entity.getEntityId()));

    final OptionsRecord record = this.toRecord(mergeValues);
    this.addOptionsForPolicyId(organizationRecord, Objects.requireNonNull(record.getOption()));
    record.setOptionChildren(entity.getModules().stream()
        .map(moduleEntity ->
            OptionChildren.builder()
                .optionGroup(moduleEntity.getModuleType())
                .idGroup(moduleEntity.getModuleId().toString())
                .build()
        ).toList());
    return record;
  }

  private void ruleIfHostedEntityAndModulesTaOrImExistIssuerAndSubjectHasToBeTheSameOrThrow(
      final EntityEntity entityEntity,
      final List<SettingsEntity> settingsEntityList) {

    if (entityEntity.getEntityType() != EntityKeyType.FEDERATION_ENTITY) {
      return;
    }

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.TRUSTANCHOR, FkKeyType.INTERMEDIATE))
        .forEach(moduleEntity -> {
          final String issuer = settingsEntityList.stream()
              .filter(settingsEntity -> settingsEntity.getKey().equals("issuer"))
              .findFirst()
              .orElseThrow()
              .getValue();

          final String subject = settingsEntityList.stream()
              .filter(settingsEntity -> settingsEntity.getKey().equals("subject"))
              .findFirst()
              .orElseThrow()
              .getValue();

          if (!issuer.equalsIgnoreCase(subject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                ("Issuer and subject must be the same on the entity:%s that this module will "
                    + "be mounted to. Issuer: %s, Subject: %s").formatted(entityEntity.getEntityId(), issuer, subject));
          }

        });
  }

  @Override
  public OptionsRecord delete(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType, final UUID id) {
    final EntityEntity entity = this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), id, this.getEntityKeyType(fkKeyType))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    this.entityRepository.delete(entity);
    return this.toRecord(entity.getSettingsEntityList());
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord, final FkKeyType fkKeyType) {
    return this.entityRepository.findByOrgNumberAndEntityKeyType(
            organizationRecord.orgNumber(), this.getEntityKeyType(fkKeyType))
        .stream()
        .map(entity -> {
              final Map<String, Object> e = super.getSettingsEntities(fkKeyType, entity.getEntityId())
                  .stream()
                  .collect(Collectors.toMap(
                      SettingsEntity::getKey,
                      SettingsEntity::castValue
                  ));
              e.put("id", entity.getEntityId().toString());
          e.put("option-children", entity.getModules().stream()
              .map(moduleEntity ->
                  Map.of("option-group", moduleEntity.getModuleType(),
                      "id-group", moduleEntity.getModuleId().toString())
              ).toList());
              return e;
            }
        )
        .toList();
  }

  protected Optional<PolicyEntity> loadPolicyIfExist(final OrganizationRecord organizationRecord,
      final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    final String parameterName = "policy_id";
    return dataValues.stream()
        .filter(value -> value.getKey().equals(parameterName))
        .map(SettingsEntity::getValue)
        .filter(Objects::nonNull)
        .filter(value -> !value.isBlank())
        .map(UUID::fromString)
        .map(policyid ->
            this.policyRepository.findByOrgNumberAndPolicyId(organizationRecord.orgNumber(), policyid))
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid policy_id, does not exist")))
        .findFirst();
  }

  protected void addOptionsForPolicyId(final OrganizationRecord organizationRecord, final List<Values> values) {
    final String parameterName = "policy_id";

    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), parameterName))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.policyRepository.findByOrgNumber(organizationRecord.orgNumber())
                .stream()
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getPolicyId().toString())
                        .value(entity.getSettingsEntity("name").orElseThrow().getValue())
                        .selected(Objects.equals(value.getValue(), entity.getPolicyId().toString()))
                        .build())
                .toList()));
  }

}
