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

import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.dto.OidfServiceHostedEntities;
import se.swedenconnect.oidf.registry.dto.OidfServiceSubModules;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.registry.entity.ResolverEntity;
import se.swedenconnect.oidf.registry.entity.TaImEntity;
import se.swedenconnect.oidf.registry.entity.TrustmarkIssuerEntity;
import se.swedenconnect.oidf.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.repository.ResolverRepository;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service to collect data to federation services
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class OidfApiService {

  public static final String TRUST_MARK_SUBJECTS = "trust_mark_subjects";

  private final PolicyRepository policyRepository;
  private final InstanceRepository instanceRepository;
  private final ResolverRepository resolverRepository;
  private final String jwkIssuer;
  private final Duration jwkExpiryDuration;
  private final JWTSupport jwtSupport;
  private final FederationMetadataCreator entityResponseFormatter = new FederationMetadataCreator();

  /**
   * Constructs a FederationApiService instance to handle OpenID Connect Federation related operations.
   *
   * @param signKey the JSON Web Key (JWK) used for signing operations
   * @param policyRepository the repository for managing policy records
   * @param instanceRepository the repository for managing instances
   * @param resolverRepository the repository for managing resolvers
   * @param jwkIssuer the issuer associated with the JSON Web Key (JWK)
   * @param jwkExpiryDuration jwkExpiryDuration
   */
  public OidfApiService(
      final JWK signKey,
      final PolicyRepository policyRepository,
      final String jwkIssuer,
      final InstanceRepository instanceRepository,
      final ResolverRepository resolverRepository,
      final Duration jwkExpiryDuration) {
    this.policyRepository = policyRepository;
    this.jwkIssuer = jwkIssuer;
    this.instanceRepository = instanceRepository;
    this.resolverRepository = resolverRepository;
    this.jwkExpiryDuration = jwkExpiryDuration;
    this.jwtSupport = new JWTSupport(signKey);
  }

  /**
   * Generates a signed JWT containing entity records for a specific instance ID.
   *
   * @param instanceId the unique identifier of the instance for which the entity records are retrieved
   * @param plainJson whether to return plain JSON
   * @return a signed JSON Web Token (JWT) string containing the entity records
   */
  @Transactional(readOnly = true)
  public String entityRecord(final UUID instanceId, final boolean plainJson) {
    Assert.notNull(instanceId, "InstanceId is mandatory");
    final String claimName = "entity_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.resolveEntityV2(instanceId).getEntityRecords())
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("trustMarkRecord Signed JWT: {}", jwt);
    if (plainJson) {
      return this.jwtSupport.toPrettyJson(jwt);
    }
    return jwt;
  }

  /**
   * Retrieves submodule records using the provided instance identifier.
   *
   * @param instanceId the unique identifier of the instance for which the submodule records are requested
   * @param plainJson whether to return plain JSON
   * @return a signed JWT string containing claims for the submodule record
   */
  @Transactional(readOnly = true)
  public String submoduleRecord(final UUID instanceId, final boolean plainJson) {
    Assert.notNull(instanceId, "instanceId is mandatory");
    final String claimName = "module_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.resolveSubmodulesV2(instanceId))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("Submodule Signed JWT: {}", jwt);
    if (plainJson) {
      return this.jwtSupport.toPrettyJson(jwt);
    }
    return jwt;
  }

  private OidfServiceSubModules resolveSubmodulesV2(final UUID instanceid) {

    final OidfServiceSubModules.OidfServiceSubModulesBuilder subModules = OidfServiceSubModules.builder();

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<EntityEntity> entities = instanceEntity.getOrganizations().stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .toList();

    final List<OidfServiceSubModules.TrustMarkIssuer> tmi = entities.stream()
        .map(EntityEntity::getTrustmarkIssuer)
        .filter(Objects::nonNull)
        .map(this::toTrustMarkIssuer)
        .toList();
    subModules.trustMarkIssuers(tmi);

    final List<OidfServiceSubModules.TrustAnchor> taIm = entities.stream()
        .map(EntityEntity::getTrustanchorIntermediate)
        .filter(Objects::nonNull)
        .map(this::toTaIm)
        .toList();
    subModules.trustAnchors(taIm);

    final List<OidfServiceSubModules.Resolver> resolverEntities = entities.stream()
        .map(EntityEntity::getResolver)
        .filter(Objects::nonNull)
        .map(this::toResolver)
        .toList();
    subModules.resolvers(resolverEntities);

    return subModules.build();

  }

  private OidfServiceHostedEntities resolveEntityV2(final UUID instanceid) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<EntityEntity> entities = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .toList();

    return OidfServiceHostedEntities.builder()
        .entityRecords(entities.stream()
            .flatMap(entityEntity ->
                this.entityResponseFormatter.createEntityResponseV2(entityEntity).stream())
            .toList())
        .build();
  }

  private OidfServiceSubModules.TrustAnchor toTaIm(final TaImEntity taImModuleEntity) {
    if (taImModuleEntity == null || taImModuleEntity.getActive() == null || !taImModuleEntity.getActive()) {
      return OidfServiceSubModules.TrustAnchor.builder().build();
    }
    return OidfServiceSubModules.TrustAnchor.builder()
        .entityIdentifier(taImModuleEntity.getEntity().getIssuer())
        .trustMarkIssuer(
            taImModuleEntity.getTrustMarkIssuers() != null ? null : taImModuleEntity.getTrustMarkIssuers().getFirst())
        .active(taImModuleEntity.getActive())
        .build();
  }

  private OidfServiceSubModules.Resolver toResolver(final ResolverEntity resolverEntity) {
    if (resolverEntity == null || resolverEntity.getActive() == null || !resolverEntity.getActive()) {
      return OidfServiceSubModules.Resolver.builder().build();
    }
    return OidfServiceSubModules.Resolver.builder()
        .entityIdentifier(resolverEntity.getEntity().getIssuer())
        .resolveResponseDuration(resolverEntity.getResolveResponseDuration())
        .trustAnchors(resolverEntity.getTrustAnchor() != null
            ? resolverEntity.getTrustAnchor()
            : null)
        .trustedKeys(resolverEntity.getTrustedKeys() != null
            ? resolverEntity.getTrustedKeys()
            : null)
        .stepRetryTime(resolverEntity.getStepRetryDuration())
        .stepCachedValueThreshold(
            resolverEntity.getStepCachedValueThreshold()) // This field is not stored in columns yet
        .build();
  }

  private OidfServiceSubModules.TrustMarkIssuer toTrustMarkIssuer(final TrustmarkIssuerEntity tmiModuleEntity) {
    if (tmiModuleEntity == null || tmiModuleEntity.getActive() == null || !tmiModuleEntity.getActive()) {
      return OidfServiceSubModules.TrustMarkIssuer.builder().build();
    }
    final List<OidfServiceSubModules.TrustMarkIssuer.TrustMark> trustMarks = tmiModuleEntity.getTrustmarks()
        .stream()
        .map(trustMarkEntity ->
            OidfServiceSubModules.TrustMarkIssuer.TrustMark.builder()
                .trustMarkIssuerId(tmiModuleEntity.getEntity().getIssuer())
                .trustMarkId(trustMarkEntity.getTrustMarkEntityId() != null
                    ? trustMarkEntity.getTrustMarkEntityId()
                    : null)
                .ref(trustMarkEntity.getRefUri())
                .logoUri(trustMarkEntity.getLogoUri())
                .delegation(trustMarkEntity.getDelegation())
                .trustMarkSubjects(
                    trustMarkEntity.getTrustmarksubjects()
                        .stream()
                        .filter(tmSubject -> tmSubject.getRevoked() != null)
                        .map(tmSubject -> OidfServiceSubModules
                            .TrustMarkIssuer.TrustMark.TrustMarkSubject.builder()
                            .revoked(tmSubject.getRevoked() != null ? tmSubject.getRevoked() : false)
                            .subject(tmSubject.getSubject() != null ? tmSubject.getSubject() : null)
                            .expires(tmSubject.getExpires() != null
                                ? tmSubject.getExpires().toString()
                                : null)
                            .granted(tmSubject.getGranted() != null
                                ? tmSubject.getGranted().toString()
                                : null)
                            .build())
                        .toList()
                )
                .build()
        ).toList();

    return OidfServiceSubModules.TrustMarkIssuer.builder()
        .entityIdentifier(tmiModuleEntity.getEntity().getIssuer())
        .trustMarkTokenValidityDuration(tmiModuleEntity.getTrustMarkTokenValidityDuration())
        .trustMarks(trustMarks)
        .build();
  }

}
