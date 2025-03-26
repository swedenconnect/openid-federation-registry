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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingDataType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * OptionsCRUDHostedEntity is a service class that extends OptionsCRUDAdapter to provide CRUD (Create, Read, Update,
 * Delete) operations specifically for entities classified with the FkKeyType.HOSTED_ENTITY type.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OptionsCRUDEntity extends OptionsCRUDAdapter {

  final EntityRepository entityRepository;
  final PolicyRepository policyRepository;

  /**
   * Constructor for OptionsCRUDEntity. Initializes the instance with the provided repositories and organization
   * supplier.
   *
   * @param settingsRepository the repository used to handle settings-related operations
   * @param userAssignedOrganization a supplier that provides the user-assigned organization entity
   * @param entityRepository the repository used for entity-related operations
   * @param policyRepository the repository used to manage policy-related operations
   */
  public OptionsCRUDEntity(
      final SettingsRepository settingsRepository,
      final Supplier<OrganizationEntity> userAssignedOrganization,
      final EntityRepository entityRepository,
      final PolicyRepository policyRepository) {
    super(settingsRepository, userAssignedOrganization);
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return FkKeyType.HOSTED_ENTITY == fkKeyType || FkKeyType.SUBORDINATE_ENTITY == fkKeyType;
  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    final OptionsRecord record = this.toRecord(this.getTemplateSettings(fkKeyType));
    this.addOptionsForPolicyId(Objects.requireNonNull(record.getOption()));
    return record;
  }

  private EntityKeyType getEntityKeyType(final FkKeyType fkKeyType) {
    return switch (fkKeyType) {
      case SUBORDINATE_ENTITY -> EntityKeyType.SUBORDINATE_ENTITY;
      case HOSTED_ENTITY -> EntityKeyType.HOSTED_ENTITY;
      default -> throw new IllegalArgumentException("Unsupported FkKeyType: %s".formatted(fkKeyType));
    };
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {

    final EntityKeyType entityKeyType = this.getEntityKeyType(fkKeyType);
    final Optional<EntityEntity> entity = this.entityRepository
        .findByEntityIdAndEntityType(id, entityKeyType);

    if (entity.isPresent()) {
      super.throwUnauthorizedIfNotMatch(entity.get().getOrganization().getOrganizationId());
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Module already exists for:%s %s".formatted(fkKeyType, id));
    }

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);
    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());

    // Create
    final EntityEntity newEntity = new EntityEntity();
    newEntity.setEntityId(id);
    newEntity.setEntityType(entityKeyType);
    newEntity.setOrganization(super.getCurrentOrganization());
    this.loadPolicyIfExist(validatedInData).ifPresent(newEntity::setPolicyEntity);

    final EntityEntity savedEntity = this.entityRepository.saveAndFlush(newEntity);
    super.deleteSettings(fkKeyType, savedEntity.getEntityId().toString());
    super.insertSettings(fkKeyType, savedEntity.getEntityId().toString(), validatedInData);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final EntityKeyType entityKeyType = this.getEntityKeyType(fkKeyType);
    final EntityEntity entity = this.entityRepository
        .findByEntityIdAndEntityType(id, entityKeyType)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No entity found for:%s %s".formatted(fkKeyType, id)));

    super.throwUnauthorizedIfNotMatch(entity.getOrganization().getOrganizationId());

    final List<SettingsEntity> template = this.getTemplateSettings(fkKeyType);

    final List<SettingsEntity> validatedInData = this.createAndValidateInputData(template, record.getOption());
    this.loadPolicyIfExist(validatedInData).ifPresent(entity::setPolicyEntity);

    this.ruleIfHostedEntityAndModulesTaOrImExistIssuerAndSubjectHasToBeTheSameOrThrow(entity, validatedInData);
    super.deleteSettings(fkKeyType, entity.getEntityId().toString());
    super.insertSettings(fkKeyType, entity.getEntityId().toString(), validatedInData);

    this.entityRepository.saveAndFlush(entity);

    final OptionsRecord updatedRecord = this.toRecord(validatedInData);
    this.addOptionsForPolicyId(Objects.requireNonNull(updatedRecord.getOption()));
    return updatedRecord;
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final EntityKeyType entityKeyType = this.getEntityKeyType(fkKeyType);
    final EntityEntity entity = this.entityRepository
        .findByEntityIdAndEntityType(id, entityKeyType)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No entity found for:%s %s".formatted(fkKeyType, id)));

    super.throwUnauthorizedIfNotMatch(entity.getOrganization().getOrganizationId());

    final List<SettingsEntity> mergeValues = insertValuesInTemplate(
        fkKeyType,
        super.getSettingsEntities(fkKeyType, entity.getEntityId()));

    final OptionsRecord record = this.toRecord(mergeValues);
    this.addOptionsForPolicyId(Objects.requireNonNull(record.getOption()));
    return record;
  }

  private void ruleIfHostedEntityAndModulesTaOrImExistIssuerAndSubjectHasToBeTheSameOrThrow(
      final EntityEntity entityEntity,
      final List<SettingsEntity> settingsEntityList) {

    if (entityEntity.getEntityType() != EntityKeyType.HOSTED_ENTITY) {
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
  @Transactional
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    final EntityEntity entity = this.entityRepository
        .findByEntityIdAndEntityType(id, this.getEntityKeyType(fkKeyType))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwUnauthorizedIfNotMatch(entity.getOrganization().getOrganizationId());
    this.entityRepository.delete(entity);
    return this.toRecord(entity.getSettingsEntityList());
  }

  @Override
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {
    return this.entityRepository.findByEntityType(this.getEntityKeyType(fkKeyType))
        .stream()
        .filter(super.hasRightOrganizationIdEntityPredicate())
        .map(entity -> {
              final Map<String, Object> e = super.getSettingsEntities(fkKeyType, entity.getEntityId())
                  .stream()
                  .collect(Collectors.toMap(
                      SettingsEntity::getKey,
                      SettingsEntity::castValue
                  ));
              e.put("id", entity.getEntityId().toString());
              return e;
            }
        )
        .toList();
  }

  protected Optional<PolicyEntity> loadPolicyIfExist(final List<SettingsEntity> dataValues)
      throws ResponseStatusException {
    final String parameterName = "policy_id";
    return dataValues.stream()
        .filter(value -> value.getKey().equals(parameterName))
        .map(SettingsEntity::getValue)
        .filter(Objects::nonNull)
        .filter(value -> !value.isBlank())
        .map(UUID::fromString)
        .map(this.policyRepository::findById)
        .map(moduleEntity -> moduleEntity.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid policy_id, does not exist")))
        .filter(super.hasRightOrganizationIdPolicyPredicate())
        .findFirst();
  }

  protected void addOptionsForPolicyId(final List<Values> values) {
    final String parameterName = "policy_id";

    values.stream()
        .filter(value -> Objects.equals(value.getValueType(), SettingDataType.OPTIONS.name()))
        .filter(value -> Objects.equals(value.getKey(), parameterName))
        .findFirst()
        .ifPresent(value ->
            value.setOptions(this.policyRepository.findByOrganizationId(super.getCurrentOrganization()
                    .getOrganizationId())
                .stream()
                .filter(this.hasRightOrganizationIdPolicyPredicate())
                .map(entity ->
                    OptionRecord.builder()
                        .key(entity.getPolicyId().toString())
                        .value(entity.getSettingsEntity("name").orElseThrow().getValue())
                        .selected(Objects.equals(value.getValue(), entity.getPolicyId().toString()))
                        .build())
                .toList()));
  }

}
