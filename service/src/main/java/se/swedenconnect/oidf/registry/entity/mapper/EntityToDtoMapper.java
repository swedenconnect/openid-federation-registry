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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.module.mapper.ModuleToDtoMapper;
import se.swedenconnect.oidf.registry.module.model.ModuleType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for converting Entity objects to DTO objects.
 *
 * @author Per Fredrik Plars
 */
public final class EntityToDtoMapper {
  private static final ObjectMapper mapper = new ObjectMapper();

  private EntityToDtoMapper() {
  }

  /**
   * Converts a FederationEntity object to a FederationEntityWithModulesDto object. Transfers relevant properties from
   * the source entity, including entity details and its associated modules and resolvers, into the target DTO.
   *
   * @param entity the FederationEntity object containing the data to be converted
   * @param includeModules If modules should be included
   * @return a FederationEntityWithModulesDto object populated with data from the provided entity
   */
  public static FederationEntityWithModulesDto toFederationEntity(final FederationEntity entity,
      final boolean includeModules) {
    final FederationEntityWithModulesDto dto = new FederationEntityWithModulesDto();
    dto.setEntityId(entity.getEntityId());
    dto.setEntityIdentifier(entity.getIssuer());
    dto.setAuthorityhints(entity.getAuthorityhints());
    dto.setCrit(entity.getCrit());
    dto.setSigningKeyId(entity.getSigningKeyIds());

    if (!includeModules) {
      return dto;
    }

    if (entity.getTrustanchorIntermediate() != null) {
      if (entity.getTrustanchorIntermediate().isOfType(ModuleType.TRUSTANCHOR)) {
        dto.setTrustAnchor(ModuleToDtoMapper.toDto(entity.getTrustanchorIntermediate()));
      }
      else if (entity.getTrustanchorIntermediate().isOfType(ModuleType.INTERMEDIATE)) {
        dto.setIntermediate(ModuleToDtoMapper.toDtoIntermediate(entity.getTrustanchorIntermediate()));
      }
    }

    if (entity.getResolver() != null) {
      dto.setResolver(ModuleToDtoMapper.toDto(entity.getResolver()));
    }

    if (entity.getTrustmarkIssuer() != null) {
      dto.setTrustmarkIssuer(ModuleToDtoMapper.toDto(entity.getTrustmarkIssuer()));
    }
    return dto;
  }

  /**
   * Converts FederationEntity to HostedEntityDto.
   *
   * @param entity the federation entity
   * @return the hosted entity DTO
   */
  public static HostedEntityDto toDtoHosted(final FederationEntity entity) {
    if (entity.getEntityType() != EntityType.HOSTED_ENTITY) {
      throw new IllegalArgumentException("Entity is not a HostedEntity");
    }
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityId(entity.getEntityId());
    dto.setEntityIdentifier(entity.getIssuer());
    dto.setCrit(Optional.ofNullable(entity.getCrit()).map(ArrayList::new).orElse(new ArrayList<>(1)));
    dto.setTrustMarkSources(readTrustMarkSourcesFromJson(entity.getTrustmarksources()));
    dto.setAuthorityhints(entity.getAuthorityhints());
    dto.setSigningKeyId(entity.getSigningKeyIds());

    // Calculation of ec_location will be made if EntityIdentifier differ from entity prefix.
    // Example EntityIdentifier is set to http://www.telia.com/oidf but the entityprefix is set to http://www.sc.se
    // An ec_location will be calculated according to calculatedEcLocation(...
    // Use a trailing-slash-normalized prefix to avoid false matches where the entityPrefix string
    // is a raw prefix of the entityIdentifier but not an actual URL path ancestor.
    // E.g. prefix="https://example.com/oidf" must NOT match "https://example.com/oidf-other".
    if (entity.getSubject() != null) {
      // Determine whether the entityIdentifier is "under" the registry's entity-prefix.
      // We check two conditions:
      //   1. Exact match: issuer == subject (locally-hosted entity whose ID is the prefix itself)
      //   2. Path-prefix match: issuer starts with subject + "/" (entity under registry URL space)
      // The trailing-slash normalization prevents false positives where subject="https://host/oidf"
      // would incorrectly match issuer="https://host/oidf-other" via raw startsWith.
      final String subjectPathPrefix = entity.getSubject().endsWith("/")
          ? entity.getSubject()
          : entity.getSubject() + "/";
      final boolean isUnderRegistryPrefix = entity.getIssuer().equals(entity.getSubject())
          || entity.getIssuer().startsWith(subjectPathPrefix);
      if (!isUnderRegistryPrefix) {
        dto.setEffectiveEcLocation(calculatedEcLocation(entity.getSubject(), entity.getIssuer()));
        dto.getCrit().add("ec_location");
      }
    }
    dto.setMetadata(entity.getMetadata());
    return dto;
  }

  // -------------------------------------------------------------------------
  // Private helper methods
  // -------------------------------------------------------------------------

  private static String calculatedEcLocation(final String baseUrl, final String subject) {
    final String id = subject
        .replace("https://", "")
        .replace("http://", "")
        .replace(".", "_")
        .replace("/", "_");
    final String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
    final String calculatedEcLocation = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    return calculatedEcLocation + encodedId;
  }

  /**
   * Reads a List of TrustmarkSourceDto from JSON string.
   *
   * @param jsonStr the JSON string
   * @return the parsed list of trustmark sources
   * @throws IllegalArgumentException if JSON parsing fails
   */
  private static List<se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSourceDto> readTrustMarkSourcesFromJson(
      final String jsonStr) {
    if (jsonStr == null || jsonStr.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return mapper.readValue(jsonStr,
          new TypeReference<List<se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSourceDto>>() {});
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to parse trustMarkSources JSON", e);
    }
  }
}
