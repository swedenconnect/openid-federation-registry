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

import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

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
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    return null;
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
    return null;
  }
}
