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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.dto.OidfServiceHostedEntities;
import se.swedenconnect.oidf.registry.dto.OidfServiceSubModules;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
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
import java.util.UUID;

import static se.swedenconnect.oidf.registry.entity.FkKeyType.INTERMEDIATE;
import static se.swedenconnect.oidf.registry.entity.FkKeyType.TRUSTANCHOR;

/**
 * Service to collect data to federation services
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class OidfApiService {

  public static final String ENTITY_IDENTIFIER = "entity_identifier";
  public static final String POLICY_ID = "policy_id";
  public static final String POLICY_RECORD = "policy_record";
  public static final String TRUST_MARKS = "trust_marks";
  public static final String TRUST_MARK_ISSUERS = "trust_mark_issuers";
  public static final String TRUST_MARK_SUBJECTS = "trust_mark_subjects";
  public static final String TRUST_ANCHORS = "trust_anchors";
  public static final String RESOLVERS = "resolvers";
  public static final String HOSTED_RECORD_ATT = "hosted_record";
  public static final String METADATA_ATT = "metadata";
  public static final String FEDERATION_ENTITY_ATT = "federation_entity";
  public static final String POLICY_ATT = "policy";

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

    final List<TaImEntity> moduleEntities = instanceEntity.getOrganizations().stream()
        .flatMap(organizationEntity -> organizationEntity.getModule().stream())
        .toList();

    final List<EntityEntity> entities = instanceEntity.getOrganizations().stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .toList();

    final List<OidfServiceSubModules.TrustMarkIssuer> tmi = entities.stream()
        .map(EntityEntity::getTrustmarkIssuer)
        .map(this::toTrustMarkIssuer)
        .toList();
    subModules.trustMarkIssuers(tmi);

    final List<OidfServiceSubModules.TrustAnchor> taIm = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTANCHOR) ||
            FkKeyType.valueOf(moduleEntity.getModuleType()).equals(INTERMEDIATE))
        .map(this::toTaIm)
        .toList();
    subModules.trustAnchors(taIm);

    final List<OidfServiceSubModules.Resolver> resolverEntities = entities.stream()
        .map(EntityEntity::getResolver)
        .map(this::toResolver)
        .toList();
    subModules.resolvers(resolverEntities);

    return subModules.build();

  }

  private OidfServiceHostedEntities resolveEntityV2(final UUID instanceid) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));


    final List<EntityEntity> trustmarkIssuersModules = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .toList();

    final OidfServiceHostedEntities.OidfServiceHostedEntitiesBuilder hostedEntitys =
        OidfServiceHostedEntities.builder();

    hostedEntitys.entityRecords(trustmarkIssuersModules.stream().map(this::toMapEntityV2).toList());

    return hostedEntitys.build();
  }

  private OidfServiceHostedEntities.Record toMapEntityV2(final EntityEntity entity) {
    final OidfServiceHostedEntities.Record.RecordBuilder hostedEntitys =
        this.entityResponseFormatter.createEntityResponseV2(entity);

    return hostedEntitys.build();
  }

  private static final ObjectMapper mapper = new ObjectMapper();

  private OidfServiceSubModules.TrustAnchor toTaIm(final TaImEntity taImModuleEntity) {
    if (taImModuleEntity.getActive() == null || !taImModuleEntity.getActive()) {
      return OidfServiceSubModules.TrustAnchor.builder().build();
    }

    // Read trust mark issuers from JSON column
    String trustMarkIssuer = null;
    if (taImModuleEntity.getTrustMarkIssuers() != null && !taImModuleEntity.getTrustMarkIssuers().isBlank()) {
      try {
        final List<String> issuers = mapper.readValue(taImModuleEntity.getTrustMarkIssuers(),
            new TypeReference<List<String>>() {});
        if (!issuers.isEmpty()) {
          trustMarkIssuer = issuers.get(0); // Use first issuer
        }
      }
      catch (final Exception e) {
        // Ignore parsing errors
      }
    }

    return OidfServiceSubModules.TrustAnchor.builder()
        .entityIdentifier(taImModuleEntity.getEntity().getSubject())
        .trustMarkIssuer(trustMarkIssuer)
        .build();
  }

  private OidfServiceSubModules.Resolver toResolver(final ResolverEntity resolverEntity) {
    if (resolverEntity.getActive() == null || !resolverEntity.getActive()) {
      return OidfServiceSubModules.Resolver.builder().build();
    }

    return OidfServiceSubModules.Resolver.builder()
        .entityIdentifier(resolverEntity.getEntity().getSubject())
        .resolveResponseDuration(resolverEntity.getResolveResponseDuration())
        .trustAnchors(resolverEntity.getTrustAnchor() != null
            ? resolverEntity.getTrustAnchor()
            : null)
        .trustedKeys(resolverEntity.getTrustedKeys() != null
            ? resolverEntity.getTrustedKeys()
            : null)
        .stepRetryTime(resolverEntity.getStepRetryDuration())
        .stepCachedValueThreshold(null) // This field is not stored in columns yet
        .build();
  }

  private OidfServiceSubModules.TrustMarkIssuer toTrustMarkIssuer(final TrustmarkIssuerEntity tmiModuleEntity) {
    if (tmiModuleEntity.getActive() == null || !tmiModuleEntity.getActive()) {
      return OidfServiceSubModules.TrustMarkIssuer.builder().build();
    }
    final List<OidfServiceSubModules.TrustMarkIssuer.TrustMark> trustMarks = tmiModuleEntity.getTrustmarks()
        .stream()
        .map(trustMarkEntity ->
            OidfServiceSubModules.TrustMarkIssuer.TrustMark.builder()
                .trustMarkIssuerId(tmiModuleEntity.getEntity().getSubject())
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
        .entityIdentifier(tmiModuleEntity.getEntity().getSubject())
        .trustMarkTokenValidityDuration(tmiModuleEntity.getTrustMarkTokenValidityDuration())
        .trustMarks(trustMarks)
        .build();
  }

}
