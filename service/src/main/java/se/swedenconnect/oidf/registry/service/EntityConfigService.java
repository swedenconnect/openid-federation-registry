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

import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.dto.SubordinateEntityDto;

import java.util.UUID;

/**
 * Service interface for managing Federation/Hosted/Subordinate entities.
 *
 * @author Per Fredrik Plars
 */
public interface EntityConfigService {

  /**
   * Creates a federation entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the federation entity data
   * @return the created federation entity
   */
  FederationEntityDto createFederationEntity(OrganizationRecord organizationRecord,
      UUID id, FederationEntityDto input);

  /**
   * Updates a federation entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the federation entity data
   * @return the updated federation entity
   */
  FederationEntityDto updateFederationEntity(OrganizationRecord organizationRecord,
      UUID id, FederationEntityDto input);

  /**
   * Gets a federation entity by ID.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @return the federation entity
   */
  FederationEntityDto getFederationEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a federation entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  void deleteFederationEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Creates a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the hosted entity data
   * @return the created hosted entity
   */
  HostedEntityDto createHostedEntity(OrganizationRecord organizationRecord,
      UUID id, HostedEntityDto input);

  /**
   * Updates a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the hosted entity data
   * @return the updated hosted entity
   */
  HostedEntityDto updateHostedEntity(OrganizationRecord organizationRecord,
      UUID id, HostedEntityDto input);

  /**
   * Gets a hosted entity by ID.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @return the hosted entity
   */
  HostedEntityDto getHostedEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  void deleteHostedEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Creates a subordinate entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the subordinate entity data
   * @return the created subordinate entity
   */
  SubordinateEntityDto createSubordinateEntity(OrganizationRecord organizationRecord,
      UUID id, SubordinateEntityDto input);

  /**
   * Updates a subordinate entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the subordinate entity data
   * @return the updated subordinate entity
   */
  SubordinateEntityDto updateSubordinateEntity(OrganizationRecord organizationRecord,
      UUID id, SubordinateEntityDto input);

  /**
   * Gets a subordinate entity by ID.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @return the subordinate entity
   */
  SubordinateEntityDto getSubordinateEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a subordinate entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  void deleteSubordinateEntity(OrganizationRecord organizationRecord, UUID id);
}


