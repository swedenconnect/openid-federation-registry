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
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;

import java.util.Collections;
import java.util.Map;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
public final class EntityToDomain {
  final static ObjectMapper mapper = new ObjectMapper();

  public static TrustAnchor mapTrustAnchor(final ModuleEntity moduleEntity) {
    final TrustAnchor o = new TrustAnchor();
    // TODO
    /*
    o.setEntityId((String) values.get("entityId"));
    o.setActive(values.get("active") != null && (Boolean) values.get("active"));
    o.setTrustMarkIssuer((String) values.get("trustMarkIssuer"));
*/
    return o;
  }

  public static Resolver mapResolver(final ModuleEntity moduleEntity) {
    //TODO MAPP DATA
    final Resolver.ResolverBuilder o = Resolver.builder();
    moduleEntity.getSettingsEntity("active")
        .map(SettingsEntity::getValue)
        .map(Boolean::valueOf)
        .ifPresent(o::active);

    moduleEntity.getSettingsEntity("entityId")
        .map(SettingsEntity::getValue)
        .map(EntityToDomain::toEntityID)
        .ifPresent(o::entityId);

    /*
    final  Map<String, Object> values

    o.setEntityId((String) values.get("entityId"));
    o.setActive(values.get("active") != null && (Boolean) values.get("active"));
    o.setResolveResponseDuration(values.get("resolveResponseDuration") != null
        ? java.time.Duration.parse((String) values.get("resolveResponseDuration"))
        : java.time.Duration.ofHours(1));
    o.setTrustAnchor((String) values.get("trustAnchor"));
    o.setTrustedKeys((String) values.get("trustedKeys"));
    o.setStepRetryDuration(values.get("stepRetryDuration") != null
        ? java.time.Duration.parse((String) values.get("stepRetryDuration"))
        : java.time.Duration.ofMinutes(1));
    */
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
      final FederationEntity.FederationEntityBuilder o = FederationEntity.builder();

      o.subject(entityEntity.getSettingsEntity("subject")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toEntityID).orElseThrow());

      o.issuer(entityEntity.getSettingsEntity("issuer")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toEntityID).orElseThrow());

      o.metadata(entityEntity.getSettingsEntity("metadata")
          .map(SettingsEntity::castValue)
          .map(o1 -> (Map<String, Object>) o1)
          .orElseThrow());

      entityEntity.getModuleByType(FkKeyType.INTERMEDIATE)
          .map(EntityToDomain::mapIntermediate)
          .ifPresent(o::intermediate);

      entityEntity.getModuleByType(FkKeyType.RESOLVER)
          .map(EntityToDomain::mapResolver)
          .ifPresent(o::resolver);

      return o.build();
    }

    if (entityEntity.getEntityType() == EntityKeyType.HOSTED_ENTITY) {
      final HostedEntity.HostedEntityBuilder o = HostedEntity.builder();

      o.subject(entityEntity.getSettingsEntity("subject")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toEntityID).orElseThrow());

      o.issuer(entityEntity.getSettingsEntity("issuer")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toEntityID).orElseThrow());

      o.metadata(entityEntity.getSettingsEntity("metadata")
          .map(SettingsEntity::castValue)
          .map(o1 -> (Map<String, Object>) o1)
          .orElseThrow());

      return o.build();
    }

    if (entityEntity.getEntityType() == EntityKeyType.SUBORDINATE_ENTITY) {

      final SubordinateEntity.SubordinateEntityBuilder o = SubordinateEntity.builder();
      o.subject(entityEntity.getSettingsEntity("subject")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toEntityID).orElseThrow());

      o.issuer(entityEntity.getSettingsEntity("issuer")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toEntityID).orElseThrow());

      entityEntity.getSettingsEntity("jwks")
          .map(SettingsEntity::getValue)
          .map(EntityToDomain::toJwks)
          .ifPresent(o::jwks);

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

    final Trustmark.TrustmarkBuilder o = Trustmark.builder();
    moduleEntity.getSettingsEntity("trustmarkissuerId")
        .map(SettingsEntity::getValue)
        .ifPresent(o::trustmarkissuerId);

    moduleEntity.getSettingsEntity("trustMarkEntityId")
        .map(SettingsEntity::getValue)
        .ifPresent(o::trustMarkEntityId);

    moduleEntity.getSettingsEntity("logoUri")
        .map(SettingsEntity::getValue)
        .ifPresent(o::logoUri);

    moduleEntity.getSettingsEntity("refUri")
        .map(SettingsEntity::getValue)
        .ifPresent(o::refUri);

    moduleEntity.getSettingsEntity("delegation")
        .map(SettingsEntity::getValue)
        .ifPresent(o::delegation);

    return o.build();
  }

  public static Intermediate mapIntermediate(ModuleEntity moduleEntity) {
    final Intermediate.IntermediateBuilder o = Intermediate.builder();
    moduleEntity.getSettingsEntity("active")
        .map(SettingsEntity::getValue)
        .map(Boolean::valueOf)
        .ifPresent(o::active);
    return o.build();
  }

  public static TrustmarkIssuer mapTrustmarkIssuer(ModuleEntity moduleEntity) {
    final TrustmarkIssuer.TrustmarkIssuerBuilder o = TrustmarkIssuer.builder();
    moduleEntity.getSettingsEntity("active")
        .map(SettingsEntity::getValue)
        .map(Boolean::valueOf)
        .ifPresent(o::active);

    moduleEntity.getSettingsEntity("entityId")
        .map(SettingsEntity::getValue)
        .map(EntityToDomain::toEntityID)
        .ifPresent(o::entityId);

    moduleEntity.getSettingsEntity("trustMarkTokenValidityDuration")
        .map(SettingsEntity::getValue)
        .ifPresent(o::trustMarkTokenValidityDuration);

    return o.build();
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
