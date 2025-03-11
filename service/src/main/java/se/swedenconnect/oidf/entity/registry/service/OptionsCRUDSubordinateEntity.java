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
 * Service implementation responsible for managing CRUD operations for subordinate entities. This class extends the
 * {@link OptionsCRUDAdapter} to provide a specific implementation for handling `SUBORDINATE_ENTITY` type entities.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OptionsCRUDSubordinateEntity extends OptionsCRUDAdapter {

  final static FkKeyType fkKeyType = FkKeyType.SUBORDINATE_ENTITY;
  final EntityRepository entityRepository;

  /**
   * Constructs an instance of OptionsCRUDSubordinateEntity, enabling CRUD functionalities for subordinate entities.
   * This service relies on injected dependencies to manage the specific functionalities for organization-related
   * subordinate records.
   *
   * @param settingsRepository the repository used to handle settings entities
   * @param userAssignedOrganization a supplier that provides the organization currently assigned to the user
   * @param entityRepository the repository used to manage subordinate entity records
   */
  public OptionsCRUDSubordinateEntity(
      final SettingsRepository settingsRepository,
      final Supplier<OrganizationEntity> userAssignedOrganization,
      final EntityRepository entityRepository) {
    super(settingsRepository, userAssignedOrganization);
    this.entityRepository = entityRepository;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return OptionsCRUDSubordinateEntity.fkKeyType == fkKeyType;
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
