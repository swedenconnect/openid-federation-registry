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
import se.swedenconnect.oidf.registry.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
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

  /**
   * Maps ModuleEntity to TrustAnchor domain object.
   *
   * @param moduleEntity the module entity
   * @return the trust anchor domain object
   */
  public static TrustAnchor mapTrustAnchor(final ModuleEntity moduleEntity) {
    final TrustAnchor.TrustAnchorBuilder o = TrustAnchor.builder();
    o.moduleId(moduleEntity.getModuleId());

    // Read from columns directly
    o.entityId(moduleEntity.getEntity().getEntityId());

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
      catch (final JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to parse trustMarkIssuers JSON", e);
      }
    }

    return o.build();
  }

  /**
   * Maps ModuleEntity to Resolver domain object.
   *
   * @param moduleEntity the module entity
   * @return the resolver domain object
   */
  public static Resolver mapResolver(final ModuleEntity moduleEntity) {
    final Resolver.ResolverBuilder o = Resolver.builder();
    o.moduleId(moduleEntity.getModuleId());

    // Read from columns directly

    o.entityId(moduleEntity.getEntity().getEntityId());

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



  /**
   * Maps EntityEntity to Entity domain object.
   *
   * @param entityEntity the entity entity
   * @return the entity domain object
   */
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
        catch (final JsonProcessingException e) {
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
        catch (final JsonProcessingException e) {
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

  /**
   * Maps PolicyEntity to Policies domain object.
   *
   * @param policyEntity the policy entity
   * @return the policies domain object
   */
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
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Maps ModuleEntity to FederationModule domain object.
   *
   * @param moduleEntity the module entity
   * @return the federation module domain object
   */
  public static FederationModule map(final ModuleEntity moduleEntity) {
    return switch (FkKeyType.valueOf(moduleEntity.getModuleType())) {
      case FkKeyType.INTERMEDIATE -> mapIntermediate(moduleEntity);
      case FkKeyType.TRUSTANCHOR -> mapTrustAnchor(moduleEntity);
      case FkKeyType.RESOLVER -> mapResolver(moduleEntity);
      case FkKeyType.TRUSTMARKISSUER -> mapTrustmarkIssuer(moduleEntity);
      default -> throw new IllegalArgumentException("Unknown module type: " + moduleEntity.getModuleType());
    };

  }



  /**
   * Maps TrustMarkSubjectEntity to TrustmarkSubject domain object.
   *
   * @param trustMarkSubjectEntity the trust mark subject entity
   * @return the trustmark subject domain object
   */
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

  /**
   * Maps ModuleEntity to Intermediate domain object.
   *
   * @param moduleEntity the module entity
   * @return the intermediate domain object
   */
  public static Intermediate mapIntermediate(final ModuleEntity moduleEntity) {
    final Intermediate.IntermediateBuilder o = Intermediate.builder();

    // Read from columns directly
    if (moduleEntity.getActive() != null) {
      o.active(moduleEntity.getActive());
    }
    
    return o.build();
  }

  /**
   * Maps ModuleEntity to TrustmarkIssuer domain object.
   *
   * @param moduleEntity the module entity
   * @return the trustmark issuer domain object
   */
  public static TrustmarkIssuer mapTrustmarkIssuer(final ModuleEntity moduleEntity) {
    final TrustmarkIssuer.TrustmarkIssuerBuilder o = TrustmarkIssuer.builder();

    // Read from columns directly
    if (moduleEntity.getActive() != null) {
      o.active(moduleEntity.getActive());
    }
    o.entityId(moduleEntity.getEntity().getEntityId());

    if (moduleEntity.getTrustMarkTokenValidityDuration() != null) {
      o.trustMarkTokenValidityDuration(moduleEntity.getTrustMarkTokenValidityDuration());
    }
    
    return o.build();
  }

  // -------------------------------------------------------------------------
  // DTO -> Domain mapping
  // -------------------------------------------------------------------------


  /**
   * Converts TrustmarkSubjectDto to TrustmarkSubject domain object.
   *
   * @param dto the trustmark subject DTO
   * @return the trustmark subject domain object
   */
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




  /**
   * Converts Policies domain object to PolicyEntity.
   *
   * @param id the policy ID
   * @param policies the policies domain object
   * @param organization the organization entity
   * @return the policy entity
   */
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
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
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
    catch (final java.text.ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
