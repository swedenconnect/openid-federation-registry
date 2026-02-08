/*
 * Copyright 2026 Sweden Connect
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

package se.swedenconnect.oidf.registry.entity.service;

import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.dto.EntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;

import java.util.List;
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
   * Retrieves a federation entity along with its optional modules.
   *
   * @param organizationRecord the organization record associated with the entity
   * @param id the unique identifier of the federation entity
   * @param includeModules whether to include associated modules (such as trust anchor, intermediate, resolver,
   *                       and trustmark issuer) in the retrieved entity
   * @return the federation entity with its optional modules if requested
   */
  FederationEntityWithModulesDto getFederationEntity(OrganizationRecord organizationRecord, UUID id,
      boolean includeModules);

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
   * List hosted entitys
   *
   * @param organizationRecord the organization record
   * @param entityIdentifier the entity ID
   * @return the hosted entity
   */
  List<HostedEntityDto> listHostedEntity(OrganizationRecord organizationRecord, String entityIdentifier);


  /**
   * Deletes a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  void deleteHostedEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a subordinate entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  void deleteSubordinateEntity(OrganizationRecord organizationRecord, UUID id);

  /**
   * Lists all entities for the organization, optionally filtered by type and with modules included.
   *
   * @param organizationRecord the organization record
   * @param type optional entity type filter (federation, hosted, subordinate)
   * @param includeModules whether to include modules (trustanchor, intermediate, resolver, trustmarkissuer)
   * @return list of entities with optional modules
   */
  EntityWithModulesDto listEntities(OrganizationRecord organizationRecord,
      String type, boolean includeModules);
}
