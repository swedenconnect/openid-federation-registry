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

package se.swedenconnect.oidf.registry.entity.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.organization.model.Organization;

import java.util.List;
import java.util.UUID;

/**
 * Utility class for converting DTO objects to Entity objects.
 *
 * @author Per Fredrik Plars
 */
public final class DtoToEntityMapper {
  private static final ObjectMapper mapper = new ObjectMapper();

  private DtoToEntityMapper() {
  }

  /**
   * Converts HostedEntityDto to FederationEntity.
   *
   * @param id the entity ID
   * @param dto the hosted entity DTO
   * @param entityType the entity type
   * @param organization the organization entity
   * @return the federation entity
   */
  public static FederationEntity toEntity(final UUID id,
      final HostedEntityDto dto,
      final EntityType entityType,
      final Organization organization) {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityType);
    entity.setOrganization(organization);
    updateEntity(entity, dto);
    return entity;
  }

  /**
   * Converts FederationEntityDto to FederationEntity.
   *
   * @param id the entity ID
   * @param dto the federation entity DTO
   * @param entityType the entity type
   * @param organization the organization entity
   * @return the federation entity
   */
  public static FederationEntity toEntity(final UUID id,
      final FederationEntityDto dto,
      final EntityType entityType,
      final Organization organization) {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityType);
    entity.setOrganization(organization);
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    entity.setCrit(dto.getCrit());
    entity.setAuthorityhints(dto.getAuthorityhints());
    return entity;
  }

  /**
   * Updates FederationEntity with HostedEntityDto data.
   *
   * @param entity the federation entity
   * @param dto the hosted entity DTO
   */
  public static void updateEntity(final FederationEntity entity, final HostedEntityDto dto) {
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    // Serialize trustMarkSources to JSON string
    entity.setTrustmarksources(writeTrustMarkSourcesToJson(dto.getTrustMarkSources()));
    entity.setMetadata(dto.getMetadata());
  }

  /**
   * Updates FederationEntity with FederationEntityDto data.
   *
   * @param entity the federation entity
   * @param dto the federation entity DTO
   */
  public static void updateEntity(final FederationEntity entity, final FederationEntityDto dto) {
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    entity.setCrit(dto.getCrit());
    entity.setAuthorityhints(dto.getAuthorityhints());
  }

  /**
   * Writes a List of TrustmarkSourceDto to JSON string.
   *
   * @param trustMarkSources the list of trustmark sources
   * @return the JSON string
   * @throws IllegalArgumentException if JSON serialization fails
   */
  private static String writeTrustMarkSourcesToJson(
      final List<se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSourceDto> trustMarkSources) {
    if (trustMarkSources == null || trustMarkSources.isEmpty()) {
      return null;
    }
    try {
      return mapper.writeValueAsString(trustMarkSources);
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize trustMarkSources to JSON", e);
    }
  }
}
