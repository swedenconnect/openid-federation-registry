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
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * OptionsCRUDHostedEntity is a service class that extends OptionsCRUDAdapter to provide CRUD (Create, Read, Update,
 * Delete) operations specifically for entities classified with the FkKeyType.HOSTED_ENTITY type.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OptionsCRUDHostedEntity extends OptionsCRUDAdapter {

  final static FkKeyType fkKeyType = FkKeyType.HOSTED_ENTITY;
  final static EntityKeyType entityKeyType = EntityKeyType.HOSTED_ENTITY;

  final EntityRepository entityRepository;

  /**
   * Constructor for the OptionsCRUDHostedEntity class. This constructs an instance of OptionsCRUDHostedEntity by
   * initializing its components for handling CRUD operations on hosted entities within the context of the given
   * settings, user-assigned organization, and entity repository.
   *
   * @param settingsRepository the repository used for managing settings entities
   * @param userAssignedOrganization a supplier for retrieving the current user-assigned organization
   * @param entityRepository the repository used for managing entities
   */
  public OptionsCRUDHostedEntity(
      final SettingsRepository settingsRepository,
      final Supplier<OrganizationEntity> userAssignedOrganization,
      final EntityRepository entityRepository) {
    super(settingsRepository, userAssignedOrganization);
    this.entityRepository = entityRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return OptionsCRUDHostedEntity.fkKeyType == fkKeyType;
  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    // Todo add policyoptions
    return this.toRecord(this.getTemplateSettings(fkKeyType));
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final Optional<EntityEntity> entity = this.entityRepository
        .findByEntityIdAndEntityType(id, EntityKeyType.HOSTED_ENTITY);

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

    final EntityEntity savedEntity = this.entityRepository.saveAndFlush(newEntity);
    super.deleteSettings(fkKeyType, savedEntity.getEntityId().toString());
    super.insertSettings(fkKeyType, savedEntity.getEntityId().toString(), validatedInData);
    return this.toRecord(validatedInData);
  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    return null;
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    return null;
  }

  @Override
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    final EntityEntity entity = this.entityRepository
        .findByEntityIdAndEntityType(id, entityKeyType)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));
    super.throwUnauthorizedIfNotMatch(entity.getOrganization().getOrganizationId());

    this.entityRepository.delete(entity);
    return this.toRecord(entity.getSettingsEntityList());
  }

}
