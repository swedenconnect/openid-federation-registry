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

package se.swedenconnect.oidf.registry.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.registry.api.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.api.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.api.dto.PolicyDto;
import se.swedenconnect.oidf.registry.api.dto.ResolverDto;
import se.swedenconnect.oidf.registry.api.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.api.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
public final class EntityToDomain {
  final static ObjectMapper mapper = new ObjectMapper();

  public static TrustAnchor mapTrustAnchor(final ModuleEntity moduleEntity) {
    final TrustAnchor.TrustAnchorBuilder o = TrustAnchor.builder();
    o.moduleId(moduleEntity.getModuleId());

    // Read from columns directly
    if (moduleEntity.getEntityIdValue() != null) {
      o.entityId(toEntityID(moduleEntity.getEntityIdValue()));
    }
    if (moduleEntity.getActive() != null) {
      o.active(moduleEntity.getActive());
    }
    if (moduleEntity.getTrustMarkIssuers() != null && !moduleEntity.getTrustMarkIssuers().isBlank()) {
      try {
        final List<String> trustMarkIssuers = mapper.readValue(
            moduleEntity.getTrustMarkIssuers(), new TypeReference<List<String>>() {});
        o.trustMarkIssuer(trustMarkIssuers.stream()
            .map(EntityToDomain::toEntityID)
            .toList());
      }
      catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to parse trustMarkIssuers JSON", e);
      }
    }

    return o.build();
  }

  public static Resolver mapResolver(final ModuleEntity moduleEntity) {
    final Resolver.ResolverBuilder o = Resolver.builder();
    o.moduleId(moduleEntity.getModuleId());

    // Read from columns directly
    if (moduleEntity.getEntityIdValue() != null) {
      o.entityId(toEntityID(moduleEntity.getEntityIdValue()));
    }
    if (moduleEntity.getActive() != null) {
      o.active(moduleEntity.getActive());
    }
    if (moduleEntity.getResolveResponseDuration() != null) {
      o.resolveResponseDuration(moduleEntity.getResolveResponseDuration());
    }
    if (moduleEntity.getTrustAnchor() != null) {
      o.trustAnchor(toEntityID(moduleEntity.getTrustAnchor()));
    }
    if (moduleEntity.getTrustedKeys() != null && !moduleEntity.getTrustedKeys().isBlank()) {
      o.trustedKeys(toJwks(moduleEntity.getTrustedKeys()));
    }
    if (moduleEntity.getStepRetryDuration() != null) {
      o.stepRetryDuration(moduleEntity.getStepRetryDuration());
    }
    
    return o.build();
  }

  public static TrustmarkSubject mapTrustmarkSubject(final Map<String, Object> values) {
    final TrustmarkSubject o = new TrustmarkSubject();
    o.setTrustmarkId(toEntityID((String) values.get("trustmarkId")));
    o.setSubject(toEntityID((String) values.get("subject")));
    o.setRevoked(values.get("revoked") != null && (Boolean) values.get("revoked"));
    o.setGranted(values.get("granted") != null
        ? java.time.LocalDateTime.parse((String) values.get("granted"))
        : null);
    o.setExpires(values.get("expires") != null
        ? java.time.LocalDateTime.parse((String) values.get("expires"))
        : null);
    return o;
  }

  public static Entity map(final EntityEntity entityEntity) {

    if (entityEntity.getEntityType() == EntityKeyType.FEDERATION_ENTITY) {
      final FederationEntity.FederationEntityBuilder<?, ?> o = FederationEntity.builder();

      o.entityId(entityEntity.getEntityId());
      o.subject(toEntityID(entityEntity.getSubject()));
      o.issuer(toEntityID(entityEntity.getIssuer()));

      // Read metadata from column
      if (entityEntity.getMetadata() != null && !entityEntity.getMetadata().isBlank()) {
        try {
          o.metadata(mapper.readValue(entityEntity.getMetadata(), new TypeReference<Map<String, Object>>() {}));
        }
        catch (JsonProcessingException e) {
          throw new IllegalArgumentException("Failed to parse metadata JSON", e);
        }
      }

      entityEntity.getModuleByType(FkKeyType.INTERMEDIATE)
          .map(EntityToDomain::mapIntermediate)
          .ifPresent(o::intermediate);

      entityEntity.getModuleByType(FkKeyType.RESOLVER)
          .map(EntityToDomain::mapResolver)
          .ifPresent(o::resolver);

      entityEntity.getModuleByType(FkKeyType.TRUSTANCHOR)
          .map(EntityToDomain::mapTrustAnchor)
          .ifPresent(o::trustAnchor);

      entityEntity.getModuleByType(FkKeyType.TRUSTMARKISSUER)
          .map(EntityToDomain::mapTrustmarkIssuer)
          .ifPresent(o::trustmarkIssuer);

      return o.build();
    }

    if (entityEntity.getEntityType() == EntityKeyType.HOSTED_ENTITY) {
      final HostedEntity.HostedEntityBuilder<?, ?> o = HostedEntity.builder();

      o.entityId(entityEntity.getEntityId());
      o.subject(toEntityID(entityEntity.getSubject()));
      o.issuer(toEntityID(entityEntity.getIssuer()));

      // Read metadata from column
      if (entityEntity.getMetadata() != null && !entityEntity.getMetadata().isBlank()) {
        try {
          o.metadata(mapper.readValue(entityEntity.getMetadata(), new TypeReference<Map<String, Object>>() {}));
        }
        catch (JsonProcessingException e) {
          throw new IllegalArgumentException("Failed to parse metadata JSON", e);
        }
      }

      return o.build();
    }

    if (entityEntity.getEntityType() == EntityKeyType.SUBORDINATE_ENTITY) {

      final SubordinateEntity.SubordinateEntityBuilder<?, ?> o = SubordinateEntity.builder();
      o.entityId(entityEntity.getEntityId());
      o.subject(toEntityID(entityEntity.getSubject()));
      o.issuer(toEntityID(entityEntity.getIssuer()));
      // Read jwks from column
      if (entityEntity.getJwks() != null && !entityEntity.getJwks().isBlank()) {
        o.jwks(toJwks(entityEntity.getJwks()));
      }

      return o.build();
    }

    throw new IllegalArgumentException("Unknown entity type: " + entityEntity.getEntityType());
  }

  public static Policies map(final PolicyEntity policyEntity) {
    if (policyEntity == null) {
      return Policies.builder().policy(Collections.emptyMap()).build();
    }
    try {
      final Policies o = new Policies();
      o.setPolicyId(policyEntity.getPolicyId());
      o.setName(policyEntity.getName());
      o.setPolicy(mapper.readValue(policyEntity.getPolicy(), new TypeReference<Map<String, Object>>() {}));
      return o;
    }
    catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static FederationModule map(final ModuleEntity moduleEntity) {
    return switch (FkKeyType.valueOf(moduleEntity.getModuleType())) {
      case FkKeyType.INTERMEDIATE -> mapIntermediate(moduleEntity);
      case FkKeyType.TRUSTANCHOR -> mapTrustAnchor(moduleEntity);
      case FkKeyType.RESOLVER -> mapResolver(moduleEntity);
      case FkKeyType.TRUSTMARKISSUER -> mapTrustmarkIssuer(moduleEntity);
      default -> throw new IllegalArgumentException("Unknown module type: " + moduleEntity.getModuleType());
    };

  }

  public static Trustmark mapTrustmark(final ModuleEntity moduleEntity) {
    // This method should not be used - Trustmark is stored in TrustMarkEntity, not ModuleEntity
    throw new UnsupportedOperationException(
        "mapTrustmark(ModuleEntity) is deprecated. Use mapTrustmark(TrustMarkEntity) instead.");
  }

  public static Trustmark mapTrustmark(final TrustMarkEntity trustMarkEntity) {
    final Trustmark.TrustmarkBuilder o = Trustmark.builder();
    o.trustmarkId(trustMarkEntity.getTrustmarkId());

    // Read from columns directly
    if (trustMarkEntity.getTrustmarkissuerId() != null) {
      o.trustmarkissuerId(trustMarkEntity.getTrustmarkissuerId());
    }

    if (trustMarkEntity.getTrustMarkEntityId() != null) {
      o.trustMarkEntityId(trustMarkEntity.getTrustMarkEntityId());
    }

    if (trustMarkEntity.getLogoUri() != null) {
      o.logoUri(trustMarkEntity.getLogoUri());
    }

    if (trustMarkEntity.getRefUri() != null) {
      o.refUri(trustMarkEntity.getRefUri());
    }

    if (trustMarkEntity.getDelegation() != null) {
      o.delegation(trustMarkEntity.getDelegation());
    }

    return o.build();
  }

  public static TrustmarkSubject mapTrustmarkSubject(final TrustMarkSubjectEntity trustMarkSubjectEntity) {
    final TrustmarkSubject.TrustmarkSubjectBuilder o = TrustmarkSubject.builder();
    o.trustmarksubjectId(trustMarkSubjectEntity.getTrustmarksubjectId());

    // Read from columns, with fallback to JSON parsing for backward compatibility
    if (trustMarkSubjectEntity.getTrustmarkIdRef() != null) {
      o.trustmarkId(toEntityID(trustMarkSubjectEntity.getTrustmarkIdRef()));
    }

    if (trustMarkSubjectEntity.getSubject() != null) {
      o.subject(toEntityID(trustMarkSubjectEntity.getSubject()));
    }

    if (trustMarkSubjectEntity.getRevoked() != null) {
      o.revoked(trustMarkSubjectEntity.getRevoked());
    }

    if (trustMarkSubjectEntity.getGranted() != null) {
      o.granted(trustMarkSubjectEntity.getGranted());
    }

    if (trustMarkSubjectEntity.getExpires() != null) {
      o.expires(trustMarkSubjectEntity.getExpires());
    }

    return o.build();
  }

  public static Intermediate mapIntermediate(ModuleEntity moduleEntity) {
    final Intermediate.IntermediateBuilder o = Intermediate.builder();

    // Read from columns directly
    if (moduleEntity.getActive() != null) {
      o.active(moduleEntity.getActive());
    }
    
    return o.build();
  }

  public static TrustmarkIssuer mapTrustmarkIssuer(ModuleEntity moduleEntity) {
    final TrustmarkIssuer.TrustmarkIssuerBuilder o = TrustmarkIssuer.builder();

    // Read from columns directly
    if (moduleEntity.getActive() != null) {
      o.active(moduleEntity.getActive());
    }
    if (moduleEntity.getEntityIdValue() != null) {
      o.entityId(toEntityID(moduleEntity.getEntityIdValue()));
    }
    if (moduleEntity.getTrustMarkTokenValidityDuration() != null) {
      o.trustMarkTokenValidityDuration(moduleEntity.getTrustMarkTokenValidityDuration());
    }
    
    return o.build();
  }

  // -------------------------------------------------------------------------
  // DTO -> Domain mapping
  // -------------------------------------------------------------------------

  public static FederationEntity toDomain(final FederationEntityDto dto) {
    return FederationEntity.builder()
        .subject(toEntityID(dto.getSubject()))
        .issuer(toEntityID(dto.getIssuer()))
        .metadata(dto.getMetadata())
        .build();
  }

  public static HostedEntity toDomain(final HostedEntityDto dto) {
    return HostedEntity.builder()
        .subject(toEntityID(dto.getSubject()))
        .issuer(toEntityID(dto.getIssuer()))
        .metadata(dto.getMetadata())
        .build();
  }

  public static SubordinateEntity toDomain(final SubordinateEntityDto dto) {
    final SubordinateEntity.SubordinateEntityBuilder<?, ?> builder = SubordinateEntity.builder()
        .subject(toEntityID(dto.getSubject()))
        .issuer(toEntityID(dto.getIssuer()));

    if (dto.getJwks() != null && !dto.getJwks().isBlank()) {
      builder.jwks(toJwks(dto.getJwks()));
    }
    return builder.build();
  }

  public static Policies toDomain(final PolicyDto dto) {
    return Policies.builder()
        .name(dto.getName())
        .policy(dto.getPolicy())
        .build();
  }

  public static TrustAnchor toDomain(final TrustAnchorDto dto) {
    return TrustAnchor.builder()
        .entityId(toEntityID(dto.getEntityId()))
        .active(Boolean.TRUE.equals(dto.getActive()))
        .trustMarkIssuer(dto.getTrustMarkIssuers()
            .stream()
            .map(EntityToDomain::toEntityID)
            .toList())
        .build();
  }

  public static Resolver toDomain(final ResolverDto dto) {
    final Resolver.ResolverBuilder builder = Resolver.builder()
        .entityId(toEntityID(dto.getEntityId()))
        .active(Boolean.TRUE.equals(dto.getActive()));

    if (dto.getResolveResponseDuration() != null) {
      builder.resolveResponseDuration(dto.getResolveResponseDuration());
    }
    if (dto.getTrustAnchor() != null) {
      builder.trustAnchor(toEntityID(dto.getTrustAnchor()));
    }
    if (dto.getTrustedKeys() != null && !dto.getTrustedKeys().isBlank()) {
      builder.trustedKeys(toJwks(dto.getTrustedKeys()));
    }
    if (dto.getStepRetryDuration() != null) {
      builder.stepRetryDuration(dto.getStepRetryDuration());
    }
    return builder.build();
  }

  public static Trustmark toDomain(final TrustmarkDto dto) {
    return Trustmark.builder()
        .trustmarkissuerId(dto.getTrustmarkissuerId())
        .trustMarkEntityId(dto.getTrustMarkEntityId())
        .logoUri(dto.getLogoUri())
        .refUri(dto.getRefUri())
        .delegation(dto.getDelegation())
        .build();
  }

  public static TrustmarkSubject toDomain(final TrustmarkSubjectDto dto) {
    final TrustmarkSubject.TrustmarkSubjectBuilder builder = TrustmarkSubject.builder()
        .trustmarkId(toEntityID(dto.getTrustmarkId()))
        .subject(toEntityID(dto.getSubject()))
        .revoked(Boolean.TRUE.equals(dto.getRevoked()));

    if (dto.getGranted() != null) {
      builder.granted(dto.getGranted());
    }
    if (dto.getExpires() != null) {
      builder.expires(dto.getExpires());
    }
    return builder.build();
  }

  // -------------------------------------------------------------------------
  // Domain -> JPA mapping
  // -------------------------------------------------------------------------

  public static EntityEntity toEntityEntity(final java.util.UUID id,
      final FederationEntity domain,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization,
      final PolicyEntity policyEntity) {

    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    entity.setPolicyEntity(policyEntity);
    entity.setSubject(domain.getSubject().toString());
    entity.setIssuer(domain.getIssuer().toString());

    // Save metadata to column
    if (domain.getMetadata() != null) {
      try {
        entity.setMetadata(mapper.writeValueAsString(domain.getMetadata()));
      }
      catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to serialize metadata to JSON", e);
      }
    }

    return entity;
  }

  public static EntityEntity toEntityEntity(final java.util.UUID id,
      final HostedEntity domain,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization,
      final PolicyEntity policyEntity) {

    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    entity.setPolicyEntity(policyEntity);

    // Save metadata to column
    if (domain.getMetadata() != null) {
      try {
        entity.setMetadata(mapper.writeValueAsString(domain.getMetadata()));
      }
      catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to serialize metadata to JSON", e);
      }
    }

    return entity;
  }

  public static EntityEntity toEntityEntity(final java.util.UUID id,
      final SubordinateEntity domain,
      final EntityKeyType entityKeyType,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization,
      final PolicyEntity policyEntity) {

    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(id);
    entity.setEntityType(entityKeyType);
    entity.setOrganization(organization);
    entity.setPolicyEntity(policyEntity);

    // Save jwks to column
    if (domain.getJwks() != null) {
      entity.setJwks(domain.getJwks().toString());
    }

    return entity;
  }

  public static PolicyEntity toPolicyEntity(final java.util.UUID id,
      final Policies policies,
      final se.swedenconnect.oidf.registry.entity.OrganizationEntity organization) {
    final PolicyEntity entity = new PolicyEntity();
    entity.setPolicyId(id);
    entity.setOrganization(organization);
    entity.setName(policies.getName());
    try {
      entity.setPolicy(mapper.writeValueAsString(
          policies.getPolicy() != null ? policies.getPolicy() : java.util.Collections.emptyMap()));
    }
    catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
    return entity;
  }

  public static TrustMarkEntity toTrustMarkEntity(final java.util.UUID id,
      final Trustmark trustmark,
      final ModuleEntity moduleEntity) {
    final TrustMarkEntity entity = TrustMarkEntity.builder()
        .trustmarkId(id)
        .module(moduleEntity)
        .trustmarkissuerId(trustmark.getTrustmarkissuerId())
        .trustMarkEntityId(trustmark.getTrustMarkEntityId())
        .logoUri(trustmark.getLogoUri())
        .refUri(trustmark.getRefUri())
        .delegation(trustmark.getDelegation())
        .build();
    return entity;
  }

  public static TrustMarkSubjectEntity toTrustMarkSubjectEntity(final java.util.UUID id,
      final TrustmarkSubject subject,
      final TrustMarkEntity trustMarkEntity) {
    final TrustMarkSubjectEntity entity = TrustMarkSubjectEntity.builder()
        .trustmarksubjectId(id)
        .trustMark(trustMarkEntity)
        .trustmarkIdRef(subject.getTrustmarkId() != null ? subject.getTrustmarkId().getValue() : null)
        .subject(subject.getSubject() != null ? subject.getSubject().getValue() : null)
        .revoked(subject.getRevoked())
        .granted(subject.getGranted())
        .expires(subject.getExpires())
        .build();
    return entity;
  }

  private static EntityID toEntityID(final String value) {
    try {
      return EntityID.parse(value);
    }
    catch (final ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static JWKSet toJwks(final String value) {
    try {
      return JWKSet.parse(value);
    }
    catch (java.text.ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
