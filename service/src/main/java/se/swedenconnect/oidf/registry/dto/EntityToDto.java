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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for converting between Entity and DTO objects.
 *
 * @author Per Fredrik Plars
 */
public final class EntityToDto {
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Converts EntityEntity to FederationEntityDto.
   *
   * @param entity the entity entity
   * @param includeModules Will include modules
   * @return the federation entity DTO
   */
  public static EntityWithModulesDto toEntityWithModulesDto(final EntityEntity entity,
      final boolean includeModules) {
    final EntityWithModulesDto dto = new EntityWithModulesDto();

    if (entity.getEntityType() == EntityKeyType.FEDERATION_ENTITY) {
      dto.setFederationEntity(EntityToDto.toFederationEntity(entity, includeModules));
    }
    else if (entity.getEntityType() == EntityKeyType.HOSTED_ENTITY) {
      dto.setHostedEntity(EntityToDto.toDtoHosted(entity));
    }
    else if (entity.getEntityType() == EntityKeyType.SUBORDINATE_ENTITY) {
      dto.setSubordinateEntity(EntityToDto.toDtoSubordinate(entity));
    }

    return dto;
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
    dto.setAuthorityhints(readListJson(entity.getAuthorityhints()));
    dto.setCrit(entity.getCrit());

    if (!includeModules) {
      return dto;
    }

    if (entity.getTrustanchorIntermediate() != null) {
      if (entity.getTrustanchorIntermediate().isOfType(TaImEntity.Type.TRUSTANCHOR)) {
        dto.setTrustAnchor(EntityToDto.toDto(entity.getTrustanchorIntermediate()));
      }
      else if (entity.getTrustanchorIntermediate().isOfType(TaImEntity.Type.INTERMEDIATE)) {
        dto.setIntermediate(EntityToDto.toDtoIntermediate(entity.getTrustanchorIntermediate()));
      }
    }

    if (entity.getResolver() != null) {
      dto.setResolver(EntityToDto.toDto(entity.getResolver()));
    }

    if (entity.getTrustmarkIssuer() != null) {
      dto.setTrustmarkIssuer(EntityToDto.toDto(entity.getTrustmarkIssuer()));
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
    dto.setAuthorityhints(readListJson(entityEntity.getAuthorityhints()));

    // Calculation of ec_location will be made if EntityIdentifier differ from entity prefix.
    // Example EntityIdentifier is set to http://www.telia.com/oidf but the entityprefix is set to http://www.sc.se
    // An ec_location will be calculated according to calculatedEcLocation(...
    if (!entityEntity.getIssuer().startsWith(entityEntity.getSubject())) {
      dto.setEffectiveEcLocation(calculatedEcLocation(entityEntity.getSubject(), entityEntity.getIssuer()));
      dto.getCrit().add("ec_location");

    }
    dto.setMetadata(readMapFromJson(entityEntity.getMetadata()));
    return dto;
  }

  /**
   * Converts HostedEntityDto to EntityEntity.
   *
   * @param id the entity ID
   * @param dto the hosted entity DTO
   * @param entityKeyType the entity key type
   * @param organization the organization entity
   * @param policyEntity the policy entity
   * @return the entity entity
   */
  public static EntityEntity toEntity(final java.util.UUID id,
      final HostedEntityDto dto,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization,
      final PolicyEntity policyEntity) {
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    entity.setPolicyEntity(policyEntity);
    updateEntity(entity, dto);
    return entity;
  }

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
    entity.setMetadata(writeMapToJsonPretty(dto.getMetadata()));
  }
  /**
   * Converts EntityEntity to SubordinateEntityDto.
   *
   * @param entityEntity the entity entity
   * @return the subordinate entity DTO
   */
  public static SubordinateEntityDto toDtoSubordinate(final EntityEntity entityEntity) {
    if (entityEntity.getEntityType() != EntityKeyType.SUBORDINATE_ENTITY) {
      throw new IllegalArgumentException("Entity is not a SubordinateEntity");
    }

    final SubordinateEntityDto dto = new SubordinateEntityDto();
    dto.setEntityId(entityEntity.getEntityId());
    dto.setSubject(entityEntity.getSubject());
    dto.setIssuer(entityEntity.getIssuer());
    dto.setJwks(entityEntity.getJwks());
    Optional.ofNullable(entityEntity.getPolicyEntity())
        .ifPresent(policyEntity -> dto.setPolicyId(policyEntity.getPolicyId()));

    Optional.ofNullable(entityEntity.getPolicyEntity())
        .map(PolicyEntity::getPolicy)
        .map(EntityToDto::readMapFromJson)
        .ifPresent(dto::setPolicy);

    dto.setCrit(entityEntity.getCrit());
    dto.setMetadataPolicyCrit(entityEntity.getMetadataPolicyCrit());

    return dto;
  }

  // -------------------------------------------------------------------------
  // Private JSON helper methods
  // -------------------------------------------------------------------------

  /**
   * Reads a Map from JSON string.
   *
   * @param jsonStr the JSON string
   * @return the parsed map, or empty map if jsonStr is null or blank
   */
  private static Map<String, Object> readMapFromJson(final String jsonStr) {
    if (jsonStr == null || jsonStr.isBlank()) {
      return Collections.emptyMap();
    }
    try {
      return mapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
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

  private static List<String> readListJson(final String jsonStr) {
    if (jsonStr == null || jsonStr.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return mapper.readValue(jsonStr, new TypeReference<List<String>>() {});
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to parse trustMarkSources JSON", e);
    }
  }

  private static String writeListJson(final List<String> sources) {
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
  /**
   * Writes a Map to JSON string with pretty printing.
   *
   * @param map the map to serialize
   * @return the JSON string representation with pretty printing
   * @throws IllegalArgumentException if JSON serialization fails
   */
  private static String writeMapToJsonPretty(final Map<String, Object> map) {
    if (map == null) {
      return null;
    }
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize map to JSON", e);
    }
  }

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

    if (policyEntity.getPolicy() != null && !policyEntity.getPolicy().isBlank()) {
      dto.setPolicy(readMapFromJson(policyEntity.getPolicy()));
    }
    else {
      dto.setPolicy(Collections.emptyMap());
    }

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
          .map(EntityToDto::toDto)
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
          .map(EntityToDto::toDto)
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

    // Convert trustmark subjects
    if (trustMarkEntity.getTrustmarksubjects() != null) {
      final List<TrustmarkSubjectDto> subjects = trustMarkEntity.getTrustmarksubjects().stream()
          .map(EntityToDto::toDto)
          .toList();
      dto.setTrustmarkSubjects(subjects);
    }

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

  // -------------------------------------------------------------------------
  // DTO -> Entity mapping
  // -------------------------------------------------------------------------

  /**
   * Converts FederationEntityDto to EntityEntity.
   *
   * @param id the entity ID
   * @param dto the federation entity DTO
   * @param entityKeyType the entity key type
   * @param organization the organization entity
   * @param policyEntity the policy entity
   * @return the entity entity
   */
  public static EntityEntity toEntity(final java.util.UUID id,
      final FederationEntityDto dto,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization,
      final PolicyEntity policyEntity) {
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    entity.setPolicyEntity(policyEntity);
    entity.setIssuer(dto.getEntityIdentifier());
    entity.setSubject(dto.getEntityIdentifier());
    entity.setCrit(dto.getCrit());
    entity.setTrustmarksources(writeListJson(dto.getAuthorityhints()));
    return entity;
  }

  /**
   * Converts SubordinateEntityDto to EntityEntity.
   *
   * @param id the entity ID
   * @param dto the subordinate entity DTO
   * @param entityKeyType the entity key type
   * @param organization the organization entity
   * @return the entity entity
   */
  public static EntityEntity toEntity(final java.util.UUID id,
      final SubordinateEntityDto dto,
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
   * Converts PolicyDto to PolicyEntity.
   *
   * @param id the policy ID
   * @param dto the policy DTO
   * @param organization the organization entity
   * @return the policy entity
   */
  public static PolicyEntity toEntity(final java.util.UUID id,
      final PolicyDto dto,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final PolicyEntity entity = new PolicyEntity();
    entity.setPolicyId(id);
    entity.setOrganization(organization);
    updateEntity(entity, dto);
    return entity;
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
    entity.setAuthorityhints(writeListJson(dto.getAuthorityhints()));
  }



  /**
   * Updates EntityEntity with SubordinateEntityDto data.
   *
   * @param entity the entity entity
   * @param dto the subordinate entity DTO
   */
  public static void updateEntity(final EntityEntity entity, final SubordinateEntityDto dto) {
    entity.setSubject(dto.getSubject());
    entity.setIssuer(dto.getIssuer());
    entity.setJwks(dto.getJwks());
    entity.setMetadataPolicyCrit(dto.getMetadataPolicyCrit());
    entity.setCrit(dto.getCrit());
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
    entity.setPolicy(writeMapToJsonPretty(policy));
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
   * Converts TrustmarkDto to TrustMarkEntity.
   *
   * @param id the trust mark ID
   * @param dto the trustmark DTO
   * @param trustmarkIssuerEntity trustmarkissuer
   * @return the trust mark entity
   */
  public static TrustMarkEntity toEntity(final java.util.UUID id,
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
   * Converts TrustmarkSubjectDto to TrustMarkSubjectEntity.
   *
   * @param id the trust mark subject ID
   * @param dto the trustmark subject DTO
   * @param trustMarkEntity the trust mark entity
   * @return the trust mark subject entity
   */
  public static TrustMarkSubjectEntity toEntity(final java.util.UUID id,
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
   * Converts TrustAnchorDto to TaImEntity.
   *
   * @param id the module ID
   * @param dto the trust anchor DTO
   * @param entityEntity the entity entity
   * @param organization the organization entity
   * @return the TaIm entity
   */
  public static TaImEntity toEntity(final java.util.UUID id,
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
  public static TaImEntity toEntity(final java.util.UUID id,
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
  public static ResolverEntity toEntity(final java.util.UUID id,
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
   * Converts TrustmarkIssuerDto to TrustmarkIssuerEntity.
   *
   * @param id the trustmark issuer ID
   * @param dto the trustmark issuer DTO
   * @param entityEntity the entity entity
   * @return the trustmark issuer entity
   */
  public static TrustmarkIssuerEntity toEntity(final java.util.UUID id,
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
      final String policyJson = subordinateEntity.getPolicy().getPolicy();
      if (policyJson != null && !policyJson.isBlank()) {
        dto.setPolicy(readMapFromJson(policyJson));
      }
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
}

