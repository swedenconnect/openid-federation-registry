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

package se.swedenconnect.oidf.registry.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.entity.ResolverEntity;
import se.swedenconnect.oidf.registry.entity.SubordinateEntity;
import se.swedenconnect.oidf.registry.entity.TaImEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.entity.TrustmarkIssuerEntity;

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
   * Converts an EntityEntity object to a FederationEntityWithModulesDto object. Transfers relevant properties from the
   * source entity, including entity details and its associated modules and resolvers, into the target DTO.
   *
   * @param entity the EntityEntity object containing the data to be converted
   * @param includeModules If modules should be included
   * @return a FederationEntityWithModulesDto object populated with data from the provided entity
   */
  public static FederationEntityWithModulesDto toFederationEntity(final EntityEntity entity,
      final boolean includeModules) {
    final FederationEntityWithModulesDto dto = new FederationEntityWithModulesDto();
    dto.setEntityId(entity.getEntityId());
    dto.setEntityIdentifier(entity.getIssuer());
    dto.setAuthorityhints(entity.getAuthorityhints());
    dto.setCrit(entity.getCrit());

    if (!includeModules) {
      return dto;
    }

    if (entity.getTrustanchorIntermediate() != null) {
      if (entity.getTrustanchorIntermediate().isOfType(TaImEntity.Type.TRUSTANCHOR)) {
        dto.setTrustAnchor(EntityToDtoMapper.toDto(entity.getTrustanchorIntermediate()));
      }
      else if (entity.getTrustanchorIntermediate().isOfType(TaImEntity.Type.INTERMEDIATE)) {
        dto.setIntermediate(EntityToDtoMapper.toDtoIntermediate(entity.getTrustanchorIntermediate()));
      }
    }

    if (entity.getResolver() != null) {
      dto.setResolver(EntityToDtoMapper.toDto(entity.getResolver()));
    }

    if (entity.getTrustmarkIssuer() != null) {
      dto.setTrustmarkIssuer(EntityToDtoMapper.toDto(entity.getTrustmarkIssuer()));
    }
    return dto;
  }

  /**
   * Converts EntityEntity to HostedEntityDto.
   *
   * @param entityEntity the entity entity
   * @return the hosted entity DTO
   */
  public static HostedEntityDto toDtoHosted(final EntityEntity entityEntity) {
    if (entityEntity.getEntityType() != EntityKeyType.HOSTED_ENTITY) {
      throw new IllegalArgumentException("Entity is not a HostedEntity");
    }
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityId(entityEntity.getEntityId());
    dto.setEntityIdentifier(entityEntity.getIssuer());
    dto.setCrit(Optional.ofNullable(entityEntity.getCrit()).map(ArrayList::new).orElse(new ArrayList<>(1)));
    dto.setTrustMarkSources(readTrustMarkSourcesFromJson(entityEntity.getTrustmarksources()));
    dto.setAuthorityhints(entityEntity.getAuthorityhints());

    // Calculation of ec_location will be made if EntityIdentifier differ from entity prefix.
    // Example EntityIdentifier is set to http://www.telia.com/oidf but the entityprefix is set to http://www.sc.se
    // An ec_location will be calculated according to calculatedEcLocation(...
    if (!entityEntity.getIssuer().startsWith(entityEntity.getSubject())) {
      dto.setEffectiveEcLocation(calculatedEcLocation(entityEntity.getSubject(), entityEntity.getIssuer()));
      dto.getCrit().add("ec_location");

    }
    dto.setMetadata(entityEntity.getMetadata());
    return dto;
  }

  /**
   * Converts PolicyEntity to PolicyDto.
   *
   * @param policyEntity the policy entity
   * @return the policy DTO
   */
  public static PolicyDto toDto(final PolicyEntity policyEntity) {
    final PolicyDto dto = new PolicyDto();
    dto.setPolicyId(policyEntity.getPolicyId());
    dto.setName(policyEntity.getName());
    dto.setPolicy(policyEntity.getPolicy());

    return dto;
  }

  /**
   * Converts TaImEntity to TrustAnchorDto.
   *
   * @param moduleEntity the TaIm entity
   * @return the trust anchor DTO
   */
  public static TrustAnchorDto toDto(final TaImEntity moduleEntity) {
    if (!moduleEntity.isOfType(TaImEntity.Type.TRUSTANCHOR)) {
      throw new IllegalArgumentException("Module is not a TrustAnchor");
    }
    final TrustAnchorDto dto = new TrustAnchorDto();
    dto.setTrustAnchorId(moduleEntity.getTaImId());
    dto.setEntityId(moduleEntity.getEntity().getEntityId());
    dto.setActive(moduleEntity.getActive());
    dto.setTrustMarkIssuers(moduleEntity.getTrustMarkIssuers());

    // Add subordinates
    if (moduleEntity.getSubordinates() != null) {
      final List<SubordinateDto> subordinates = moduleEntity.getSubordinates().stream()
          .map(EntityToDtoMapper::toDto)
          .toList();
      dto.setSubordinates(subordinates);
    }

    return dto;
  }

  /**
   * Converts TaImEntity to IntermediateDto.
   *
   * @param moduleEntity the TaIm entity
   * @return the trust anchor DTO
   */
  public static IntermediateDto toDtoIntermediate(final TaImEntity moduleEntity) {
    if (!moduleEntity.isOfType(TaImEntity.Type.INTERMEDIATE)) {
      throw new IllegalArgumentException("Module is not a INTERMEDIATE");
    }

    final IntermediateDto dto = new IntermediateDto();
    dto.setIntermediateId(moduleEntity.getTaImId());
    dto.setEntityId(moduleEntity.getEntity().getEntityId());
    dto.setActive(moduleEntity.getActive());

    // Add subordinates
    if (moduleEntity.getSubordinates() != null) {
      final List<SubordinateDto> subordinates = moduleEntity.getSubordinates().stream()
          .map(EntityToDtoMapper::toDto)
          .toList();
      dto.setSubordinates(subordinates);
    }

    return dto;
  }

  /**
   * Converts ResolverEntity to ResolverDto.
   *
   * @param resolverEntity the resolver entity
   * @return the resolver DTO
   */
  public static ResolverDto toDto(final ResolverEntity resolverEntity) {
    final ResolverDto dto = new ResolverDto();
    dto.setResolverId(resolverEntity.getResolverId());
    dto.setEntityId(resolverEntity.getEntity().getEntityId());
    dto.setActive(resolverEntity.getActive());
    dto.setResolveResponseDuration(resolverEntity.getResolveResponseDuration());
    dto.setTrustAnchor(resolverEntity.getTrustAnchor());
    dto.setTrustedKeys(resolverEntity.getTrustedKeys());
    dto.setStepRetryDuration(resolverEntity.getStepRetryDuration());
    return dto;
  }

  /**
   * Converts TrustMarkEntity to TrustmarkDto.
   *
   * @param trustMarkEntity the trust mark entity
   * @return the trustmark DTO
   */
  public static TrustmarkDto toDto(final TrustMarkEntity trustMarkEntity) {
    final TrustmarkDto dto = new TrustmarkDto();
    dto.setTrustmarkId(trustMarkEntity.getTrustmarkId());
    dto.setTrustmarkissuerId(trustMarkEntity.getTrustmarkIssuer().getTrustmarkIssuerId());
    dto.setTrustmarkType(trustMarkEntity.getTrustmarkType());
    dto.setLogoUri(trustMarkEntity.getLogoUri());
    dto.setRefUri(trustMarkEntity.getRefUri());
    dto.setDelegation(trustMarkEntity.getDelegation());
    return dto;
  }

  /**
   * Converts TrustMarkEntity to TrustmarkWithSubjectsDto including trustmark subjects.
   *
   * @param trustMarkEntity the trust mark entity
   * @return the trustmark with subjects DTO
   */
  public static TrustmarkWithSubjectsDto toDtoWithSubjects(final TrustMarkEntity trustMarkEntity) {
    final TrustmarkWithSubjectsDto dto = new TrustmarkWithSubjectsDto();
    dto.setTrustmarkId(trustMarkEntity.getTrustmarkId());
    dto.setTrustmarkissuerId(trustMarkEntity.getTrustmarkIssuer().getTrustmarkIssuerId());
    dto.setTrustmarkType(trustMarkEntity.getTrustmarkType());
    dto.setLogoUri(trustMarkEntity.getLogoUri());
    dto.setRefUri(trustMarkEntity.getRefUri());
    dto.setDelegation(trustMarkEntity.getDelegation());


    Optional.ofNullable(trustMarkEntity.getTrustmarksubjects())
        .map(trustMarkSubjectEntities ->
            trustMarkSubjectEntities.stream().map(EntityToDtoMapper::toDto)
                .toList())
        .ifPresent(dto::setTrustmarkSubjects);

    return dto;
  }

  /**
   * Converts TrustMarkEntity to TrustmarkWithSubjectsDto with empty subjects list.
   *
   * @param trustMarkEntity the trust mark entity
   * @return the trustmark with empty subjects DTO
   */
  public static TrustmarkWithSubjectsDto toDtoWithSubjectsEmpty(final TrustMarkEntity trustMarkEntity) {
    final TrustmarkWithSubjectsDto dto = new TrustmarkWithSubjectsDto();
    dto.setTrustmarkId(trustMarkEntity.getTrustmarkId());
    dto.setTrustmarkissuerId(trustMarkEntity.getTrustmarkIssuer().getTrustmarkIssuerId());
    dto.setTrustmarkType(trustMarkEntity.getTrustmarkType());
    dto.setLogoUri(trustMarkEntity.getLogoUri());
    dto.setRefUri(trustMarkEntity.getRefUri());
    dto.setDelegation(trustMarkEntity.getDelegation());
    dto.setTrustmarkSubjects(Collections.emptyList());
    return dto;
  }

  /**
   * Converts TrustMarkSubjectEntity to TrustmarkSubjectDto.
   *
   * @param trustMarkSubjectEntity the trust mark subject entity
   * @return the trustmark subject DTO
   */
  public static TrustmarkSubjectDto toDto(final TrustMarkSubjectEntity trustMarkSubjectEntity) {
    final TrustmarkSubjectDto dto = new TrustmarkSubjectDto();
    dto.setTrustmarksubjectId(trustMarkSubjectEntity.getTrustmarksubjectId());
    dto.setTrustmarkId(trustMarkSubjectEntity.getTrustmarkId());
    dto.setSubject(trustMarkSubjectEntity.getSubject());
    dto.setRevoked(trustMarkSubjectEntity.getRevoked());
    dto.setGranted(trustMarkSubjectEntity.getGranted());
    dto.setExpires(trustMarkSubjectEntity.getExpires());
    return dto;
  }

  /**
   * Converts TrustmarkIssuerEntity to TrustmarkIssuerDto.
   *
   * @param entity the trustmark issuer entity
   * @return the trustmark issuer DTO
   */
  public static TrustmarkIssuerDto toDto(final TrustmarkIssuerEntity entity) {
    final TrustmarkIssuerDto dto = new TrustmarkIssuerDto();
    dto.setTrustmarkIssuerId(entity.getTrustmarkIssuerId());
    dto.setEntityId(entity.getEntity().getEntityId());
    dto.setActive(entity.getActive());
    dto.setTrustMarkTokenValidityDuration(entity.getTrustMarkTokenValidityDuration());
    return dto;
  }

  /**
   * Converts SubordinateEntity to SubordinateDto.
   *
   * @param subordinateEntity the subordinate entity
   * @return the subordinate DTO
   */
  public static SubordinateDto toDto(final SubordinateEntity subordinateEntity) {
    final SubordinateDto dto = new SubordinateDto();
    dto.setSubordinateId(subordinateEntity.getSubordinateId());
    dto.setTaImId(subordinateEntity.getTaIm().getTaImId());

    if (subordinateEntity.getPolicy() != null) {
      dto.setPolicyId(subordinateEntity.getPolicy().getPolicyId());
      // Set policy as read-only field
      dto.setPolicy(subordinateEntity.getPolicy().getPolicy());
    }

    dto.setJwks(subordinateEntity.getJwks());
    dto.setEntityIdentifier(subordinateEntity.getEntityidentifier());

    // Convert crit and metadataPolicyCrit from comma-separated strings to lists
    if (subordinateEntity.getCrit() != null && !subordinateEntity.getCrit().isEmpty()) {
      dto.setCrit(List.of(subordinateEntity.getCrit().split(",")));
    }

    if (subordinateEntity.getMetadataPolicyCrit() != null && !subordinateEntity.getMetadataPolicyCrit().isEmpty()) {
      dto.setMetadataPolicyCrit(List.of(subordinateEntity.getMetadataPolicyCrit().split(",")));
    }

    dto.setEcLocation(subordinateEntity.getEcLocation());
    dto.setEcLocationAutomaticResolve(subordinateEntity.isEcLocationAutomatic());

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
    return calculatedEcLocation + encodedId + "/.well-known/openid-federation";
  }

  /**
   * Reads a List of TrustmarkSourceDto from JSON string.
   *
   * @param jsonStr the JSON string
   * @return the parsed list of trustmark sources
   * @throws IllegalArgumentException if JSON parsing fails
   */
  private static List<TrustmarkSourceDto> readTrustMarkSourcesFromJson(final String jsonStr) {
    if (jsonStr == null || jsonStr.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return mapper.readValue(jsonStr, new TypeReference<List<TrustmarkSourceDto>>() {});
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to parse trustMarkSources JSON", e);
    }
  }
}
