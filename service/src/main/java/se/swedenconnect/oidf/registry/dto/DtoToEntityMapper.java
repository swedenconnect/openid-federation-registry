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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
   * Converts HostedEntityDto to EntityEntity.
   *
   * @param id the entity ID
   * @param dto the hosted entity DTO
   * @param entityKeyType the entity key type
   * @param organization the organization entity
   * @return the entity entity
   */
  public static EntityEntity toEntity(final UUID id,
      final HostedEntityDto dto,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    updateEntity(entity, dto);
    return entity;
  }

  /**
   * Converts FederationEntityDto to EntityEntity.
   *
   * @param id the entity ID
   * @param dto the federation entity DTO
   * @param entityKeyType the entity key type
   * @param organization the organization entity
   * @return the entity entity
   */
  public static EntityEntity toEntity(final UUID id,
      final FederationEntityDto dto,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    entity.setCrit(dto.getCrit());
    entity.setAuthorityhints(dto.getAuthorityhints());
    return entity;
  }

  /**
   * Converts PolicyDto to PolicyEntity.
   *
   * @param id the policy ID
   * @param dto the policy DTO
   * @param organization the organization entity
   * @return the policy entity
   */
  public static PolicyEntity toEntity(final UUID id,
      final PolicyDto dto,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final PolicyEntity entity = new PolicyEntity();
    entity.setPolicyId(id);
    entity.setOrganization(organization);
    updateEntity(entity, dto);
    return entity;
  }

  /**
   * Converts TrustAnchorDto to TaImEntity.
   *
   * @param id the module ID
   * @param dto the trust anchor DTO
   * @param entityEntity the entity entity
   * @param organization the organization entity
   * @return the TaIm entity
   */
  public static TaImEntity toEntity(final UUID id,
      final TrustAnchorDto dto,
      final EntityEntity entityEntity,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final TaImEntity module = new TaImEntity();
    module.setTaImId(id);
    module.setModuleType(TaImEntity.Type.TRUSTANCHOR);
    module.setEntity(entityEntity);
    module.setOrganization(organization);
    module.setActive(dto.getActive());
    module.setTrustMarkIssuers(dto.getTrustMarkIssuers());

    return module;
  }

  /**
   * Converts IntermediateDto to TaImEntity.
   *
   * @param id the module ID
   * @param dto the intermediate DTO
   * @param entityEntity the entity entity
   * @param organization the organization entity
   * @return the TaIm entity
   */
  public static TaImEntity toEntity(final UUID id,
      final IntermediateDto dto,
      final EntityEntity entityEntity,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final TaImEntity module = new TaImEntity();
    module.setTaImId(id);
    module.setModuleType(TaImEntity.Type.INTERMEDIATE);
    module.setEntity(entityEntity);
    module.setOrganization(organization);
    module.setActive(dto.getActive());

    return module;
  }

  /**
   * Converts ResolverDto to ResolverEntity.
   *
   * @param id the resolver ID
   * @param dto the resolver DTO
   * @param entityEntity the entity entity
   * @return the resolver entity
   */
  public static ResolverEntity toEntity(final UUID id,
      final ResolverDto dto,
      final EntityEntity entityEntity) {
    return ResolverEntity.builder()
        .resolverId(id)
        .entity(entityEntity)
        .active(dto.getActive())
        .resolveResponseDuration(dto.getResolveResponseDuration())
        .trustAnchor(dto.getTrustAnchor())
        .trustedKeys(dto.getTrustedKeys())
        .stepRetryDuration(dto.getStepRetryDuration())
        .stepCachedValueThreshold(dto.getStepCachedValueThreshold())
        .build();
  }

  /**
   * Converts TrustmarkDto to TrustMarkEntity.
   *
   * @param id the trust mark ID
   * @param dto the trustmark DTO
   * @param trustmarkIssuerEntity trustmarkissuer
   * @return the trust mark entity
   */
  public static TrustMarkEntity toEntity(final UUID id,
      final TrustmarkDto dto,
      final TrustmarkIssuerEntity trustmarkIssuerEntity) {
    return TrustMarkEntity.builder()
        .trustmarkId(id)
        .trustmarkIssuer(trustmarkIssuerEntity)
        .trustmarkType(dto.getTrustmarkType())
        .logoUri(dto.getLogoUri())
        .refUri(dto.getRefUri())
        .delegation(dto.getDelegation())
        .build();
  }

  /**
   * Converts TrustmarkSubjectDto to TrustMarkSubjectEntity.
   *
   * @param id the trust mark subject ID
   * @param dto the trustmark subject DTO
   * @param trustMarkEntity the trust mark entity
   * @return the trust mark subject entity
   */
  public static TrustMarkSubjectEntity toEntity(final UUID id,
      final TrustmarkSubjectDto dto,
      final TrustMarkEntity trustMarkEntity) {
    return TrustMarkSubjectEntity.builder()
        .trustmarksubjectId(id)
        .trustMark(trustMarkEntity)
        .subject(dto.getSubject())
        .revoked(dto.getRevoked())
        .granted(dto.getGranted())
        .expires(dto.getExpires())
        .build();
  }

  /**
   * Converts TrustmarkIssuerDto to TrustmarkIssuerEntity.
   *
   * @param id the trustmark issuer ID
   * @param dto the trustmark issuer DTO
   * @param entityEntity the entity entity
   * @return the trustmark issuer entity
   */
  public static TrustmarkIssuerEntity toEntity(final UUID id,
      final TrustmarkIssuerDto dto,
      final EntityEntity entityEntity) {
    return TrustmarkIssuerEntity.builder()
        .trustmarkIssuerId(id)
        .entity(entityEntity)
        .active(dto.getActive())
        .trustMarkTokenValidityDuration(dto.getTrustMarkTokenValidityDuration())
        .build();
  }

  /**
   * Converts SubordinateDto to SubordinateEntity.
   *
   * @param id the subordinate ID
   * @param dto the subordinate DTO
   * @param taIm the TaIm entity
   * @return the subordinate entity
   */
  public static SubordinateEntity toEntity(final UUID id,
      final SubordinateDto dto,
      final TaImEntity taIm) {
    final SubordinateEntity entity = new SubordinateEntity();
    entity.setSubordinateId(id);
    entity.setTaIm(taIm);
    entity.setJwks(dto.getJwks());
    entity.setEntityidentifier(dto.getEntityIdentifier());

    // Convert crit and metadataPolicyCrit from lists to comma-separated strings
    if (dto.getCrit() != null && !dto.getCrit().isEmpty()) {
      entity.setCrit(String.join(",", dto.getCrit()));
    }

    if (dto.getMetadataPolicyCrit() != null && !dto.getMetadataPolicyCrit().isEmpty()) {
      entity.setMetadataPolicyCrit(String.join(",", dto.getMetadataPolicyCrit()));
    }

    if (!entity.isEcLocationAutomatic()) {
      entity.setEcLocation(dto.getEcLocation());
    }
    entity.setEcLocationAutomatic(dto.isEcLocationAutomaticResolve());
    return entity;
  }

  // -------------------------------------------------------------------------
  // Update methods
  // -------------------------------------------------------------------------

  /**
   * Updates EntityEntity with HostedEntityDto data.
   *
   * @param entity the entity entity
   * @param dto the hosted entity DTO
   */
  public static void updateEntity(final EntityEntity entity, final HostedEntityDto dto) {
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    // Serialize trustMarkSources to JSON string
    entity.setTrustmarksources(writeTrustMarkSourcesToJson(dto.getTrustMarkSources()));
    entity.setMetadata(dto.getMetadata());
  }

  /**
   * Updates EntityEntity with FederationEntityDto data.
   *
   * @param entity the entity entity
   * @param dto the federation entity DTO
   */
  public static void updateEntity(final EntityEntity entity, final FederationEntityDto dto) {
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    entity.setCrit(dto.getCrit());
    entity.setAuthorityhints(dto.getAuthorityhints());
  }

  /**
   * Updates PolicyEntity with PolicyDto data.
   *
   * @param entity the policy entity
   * @param dto the policy DTO
   */
  public static void updateEntity(final PolicyEntity entity, final PolicyDto dto) {
    entity.setName(dto.getName());
    final Map<String, Object> policy = dto.getPolicy() != null ? dto.getPolicy() : Collections.emptyMap();
    entity.setPolicy(policy);
  }

  /**
   * Updates TaImEntity with TrustAnchorDto data.
   *
   * @param module the TaIm entity
   * @param dto the trust anchor DTO
   */
  public static void updateIntermediate(final TaImEntity module, final TrustAnchorDto dto) {
    module.setActive(dto.getActive());
    module.setTrustMarkIssuers(dto.getTrustMarkIssuers());
  }

  /**
   * Updates TaImEntity with IntermediateDto data.
   *
   * @param module the TaIm entity
   * @param dto the intermediate DTO
   */
  public static void updateIntermediate(final TaImEntity module, final IntermediateDto dto) {
    module.setActive(dto.getActive());
  }

  /**
   * Updates ResolverEntity with ResolverDto data.
   *
   * @param entity the resolver entity
   * @param dto the resolver DTO
   */
  public static void updateEntity(final ResolverEntity entity, final ResolverDto dto) {
    entity.setActive(dto.getActive());
    entity.setResolveResponseDuration(dto.getResolveResponseDuration());
    entity.setTrustAnchor(dto.getTrustAnchor());
    entity.setTrustedKeys(dto.getTrustedKeys());
    entity.setStepRetryDuration(dto.getStepRetryDuration());
  }

  /**
   * Updates TrustMarkEntity with TrustmarkDto data.
   *
   * @param entity the trust mark entity
   * @param dto the trustmark DTO
   */
  public static void updateEntity(final TrustMarkEntity entity, final TrustmarkDto dto) {

    entity.setTrustmarkType(dto.getTrustmarkType());
    entity.setLogoUri(dto.getLogoUri());
    entity.setRefUri(dto.getRefUri());
    entity.setDelegation(dto.getDelegation());
  }

  /**
   * Updates TrustMarkSubjectEntity with TrustmarkSubjectDto data.
   *
   * @param entity the trust mark subject entity
   * @param dto the trustmark subject DTO
   */
  public static void updateEntity(final TrustMarkSubjectEntity entity, final TrustmarkSubjectDto dto) {
    entity.setSubject(dto.getSubject());
    entity.setRevoked(dto.getRevoked());
    entity.setGranted(dto.getGranted());
    entity.setExpires(dto.getExpires());
  }

  /**
   * Updates TrustmarkIssuerEntity with TrustmarkIssuerDto data.
   *
   * @param entity the trustmark issuer entity
   * @param dto the trustmark issuer DTO
   */
  public static void updateEntity(final TrustmarkIssuerEntity entity, final TrustmarkIssuerDto dto) {
    entity.setActive(dto.getActive());
    entity.setTrustMarkTokenValidityDuration(dto.getTrustMarkTokenValidityDuration());
  }

  /**
   * Updates SubordinateEntity with SubordinateDto data.
   *
   * @param entity the subordinate entity
   * @param dto the subordinate DTO
   */
  public static void updateEntity(final SubordinateEntity entity, final SubordinateDto dto) {
    entity.setJwks(dto.getJwks());
    entity.setEntityidentifier(dto.getEntityIdentifier());

    // Convert crit and metadataPolicyCrit from lists to comma-separated strings
    if (dto.getCrit() != null && !dto.getCrit().isEmpty()) {
      entity.setCrit(String.join(",", dto.getCrit()));
    }
    else {
      entity.setCrit(null);
    }

    if (dto.getMetadataPolicyCrit() != null && !dto.getMetadataPolicyCrit().isEmpty()) {
      entity.setMetadataPolicyCrit(String.join(",", dto.getMetadataPolicyCrit()));
    }
    else {
      entity.setMetadataPolicyCrit(null);
    }

    entity.setEcLocation(dto.getEcLocation());
    entity.setEcLocationAutomatic(dto.isEcLocationAutomaticResolve());
  }

  // -------------------------------------------------------------------------
  // Private JSON helper methods
  // -------------------------------------------------------------------------

  /**
   * Writes a List of TrustmarkSourceDto to JSON string.
   *
   * @param sources the list of trustmark sources to serialize
   * @return the JSON string representation
   * @throws IllegalArgumentException if JSON serialization fails
   */
  private static String writeTrustMarkSourcesToJson(final List<TrustmarkSourceDto> sources) {
    if (sources == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(sources);
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize trustMarkSources to JSON", e);
    }
  }
}
