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
package se.swedenconnect.oidf.registry.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.dto.EntityToDto;
import se.swedenconnect.oidf.registry.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.dto.oidfservice.EntityRecord;
import se.swedenconnect.oidf.registry.dto.oidfservice.ModuleRecord;
import se.swedenconnect.oidf.registry.dto.oidfservice.ResolverProperties;
import se.swedenconnect.oidf.registry.dto.oidfservice.TrustAnchorProperties;
import se.swedenconnect.oidf.registry.dto.oidfservice.TrustMarkDelegation;
import se.swedenconnect.oidf.registry.dto.oidfservice.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.registry.dto.oidfservice.TrustMarkProperties;
import se.swedenconnect.oidf.registry.dto.oidfservice.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.registry.dto.oidfservice.gsonserde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.registry.entity.ResolverEntity;
import se.swedenconnect.oidf.registry.entity.SubordinateEntity;
import se.swedenconnect.oidf.registry.entity.TaImEntity;
import se.swedenconnect.oidf.registry.entity.TrustmarkIssuerEntity;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.repository.SubordinateRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to collect data to federation services
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class OidfApiService {

  public static final String TRUST_MARK_SUBJECTS = "trust_mark_subjects";

  private final InstanceRepository instanceRepository;
  private final EntityRepository entityRepository;

  private final String jwkIssuer;
  private final Duration jwkExpiryDuration;
  private final JWTSupport jwtSupport;
  private final FederationMetadataCreator entityResponseFormatter;
  private final JsonRegistryLoader serdeLoader = new JsonRegistryLoader();

  /**
   * Constructs a FederationApiService instance to handle OpenID Connect Federation related operations.
   *
   * @param subordinateRepository EntityRepository
   * @param signKey the JSON Web Key (JWK) used for signing operations
   * @param instanceRepository the repository for managing instances
   * @param entityRepository the repository for managing entity
   * @param jwkIssuer the issuer associated with the JSON Web Key (JWK)
   * @param jwkExpiryDuration jwkExpiryDuration
   */
  public OidfApiService(
      final JWK signKey,
      final SubordinateRepository subordinateRepository,
      final EntityRepository entityRepository,
      final String jwkIssuer,
      final InstanceRepository instanceRepository,
      final Duration jwkExpiryDuration) {
    this.entityRepository = entityRepository;
    this.jwkIssuer = jwkIssuer;
    this.instanceRepository = instanceRepository;
    this.jwkExpiryDuration = jwkExpiryDuration;
    this.jwtSupport = new JWTSupport(signKey);
    this.entityResponseFormatter = new FederationMetadataCreator(subordinateRepository);
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
    if (plainJson) {
      return this.serdeLoader.toJson(this.resolveEntity(instanceId));
    }
    final String claimName = "entity_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.serdeLoader.toJson(this.resolveEntity(instanceId)))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("trustMarkRecord Signed JWT: {}", jwt);

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
  public String moduleRecord(final UUID instanceId, final boolean plainJson) {
    Assert.notNull(instanceId, "instanceId is mandatory");
    if (plainJson) {
      return this.serdeLoader.toJson(this.resolveModules(instanceId));
    }
    final String claimName = "module_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.serdeLoader.toJson(this.resolveModules(instanceId)))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("Submodule Signed JWT: {}", jwt);

    return jwt;
  }

  private ModuleRecord resolveModules(final UUID instanceId) {

    final ModuleRecord subModules = new ModuleRecord();

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceId)));

    final List<EntityEntity> entities = instanceEntity.getOrganizations().stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .filter(entityEntity -> entityEntity.getEntityType().equals(EntityKeyType.FEDERATION_ENTITY))
        .toList();

    final List<TrustMarkIssuerProperties> tmi = entities.stream()
        .map(EntityEntity::getTrustmarkIssuer)
        .filter(Objects::nonNull)
        .filter(TrustmarkIssuerEntity::getActive)
        .map(this::toTrustMarkIssuer)
        .toList();
    subModules.setTrustMarkIssuers(tmi);

    final List<TrustAnchorProperties> taIm = entities.stream()
        .map(EntityEntity::getTrustanchorIntermediate)
        .filter(Objects::nonNull)
        .filter(TaImEntity::getActive)
        .map(this::toTaIm)
        .toList();
    subModules.setTrustAnchors(taIm);

    final List<ResolverProperties> resolverEntities = entities.stream()
        .map(EntityEntity::getResolver)
        .filter(Objects::nonNull)
        .filter(ResolverEntity::getActive)
        .map(this::toResolver)
        .toList();
    subModules.setResolvers(resolverEntities);

    return subModules;
  }

  private List<EntityRecord> resolveEntity(final UUID instanceId) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceId)));

    final List<EntityEntity> entities = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .toList();

    return entities.stream()
        .map(this.entityResponseFormatter::createEntityResponse)
        .filter(Objects::nonNull)
        .toList();

  }

  private TrustAnchorProperties toTaIm(final TaImEntity taImModuleEntity) {
    return TrustAnchorProperties.builder()
        .entityIdentifier(new EntityID(taImModuleEntity.getEntity().getIssuer()))
        //TODO .trustMarkOwners()
        //TODO .trustMarkIssuers()
        .subordinates(taImModuleEntity.getSubordinates()
            .stream()
            .map(this::toSubordinates)
            .filter(Objects::nonNull)
            .toList())
        .build();
  }

  private TrustAnchorProperties.SubordinateListingProperty toSubordinates(final SubordinateEntity subordinateEntity) {
    final TrustAnchorProperties.SubordinateListingProperty sub = new TrustAnchorProperties.SubordinateListingProperty();
    final SubordinateDto subDto = EntityToDto.toDto(subordinateEntity);

    sub.setJwks(this.toJwksSet(subDto.getJwks()));
    sub.setOverrideConfigurationLocation(subDto.getEcLocation());
    sub.setMetadataPolicyCrit(subDto.getMetadataPolicyCrit());
    sub.setCrit(Optional.ofNullable(subDto.getCrit()).orElse(new ArrayList<>(1)));
    //TODO Implement naming constraints
    //sub.setConstraints();
    sub.setEntityIdentifier(new EntityID(subDto.getEntityIdentifier()));
    // if autoresolve is marked true. System tries to get the hosted entity.
    // If not found this subordinate relation is removed since it can not be resolved
    if (subDto.isEcLocationAutomaticResolve()) {
      return this.entityRepository.findByOrgNumberAndEntityKeyTypeAndIssuer(
              subordinateEntity.getTaIm().getOrganization().getOrgNumber(),
              EntityKeyType.HOSTED_ENTITY,
              subordinateEntity.getEntityidentifier())
          .map(EntityToDto::toDtoHosted)
          .map(dto -> {
            sub.setOverrideConfigurationLocation(dto.getEffectiveEcLocation());
            sub.getCrit().add("ec_location");
            return sub;
          })
          .orElse(null);
    }
    return sub;
  }

  private ResolverProperties toResolver(final ResolverEntity resolverEntity) {
    if (resolverEntity == null || resolverEntity.getActive() == null || !resolverEntity.getActive()) {
      return ResolverProperties.builder().build();
    }
    return ResolverProperties.builder()
        .entityIdentifier(resolverEntity.getEntity().getIssuer())
        .resolveResponseDuration(this.toDuration(resolverEntity.getResolveResponseDuration()))

        .trustAnchor(resolverEntity.getTrustAnchor())
        .trustedKeys(this.toJwksSet(resolverEntity.getTrustedKeys()))
        .stepRetryTime(this.toDuration(resolverEntity.getStepRetryDuration()))
        .useCachedValue(resolverEntity.getStepCachedValueThreshold())
        .build();
  }

  private TrustMarkIssuerProperties toTrustMarkIssuer(final TrustmarkIssuerEntity tmiModuleEntity) {
    final List<TrustMarkProperties> trustMarks = tmiModuleEntity.getTrustmarks()
        .stream()
        .map(trustMarkEntity ->
            TrustMarkProperties.builder()
                .trustMarkId(new EntityID(trustMarkEntity.getTrustmarkType()))
                .refUri(trustMarkEntity.getRefUri())
                .logoUri(trustMarkEntity.getLogoUri())
                .delegation(Optional.ofNullable(trustMarkEntity.getDelegation())
                    .map(TrustMarkDelegation::new)
                    .orElse(null))
                .trustMarkSubjects(
                    trustMarkEntity.getTrustmarksubjects()
                        .stream()
                        .filter(tmSubject -> tmSubject.getRevoked() != null)
                        .map(tmSubject -> TrustMarkSubjectProperty.builder()
                            .revoked(tmSubject.getRevoked() != null ? tmSubject.getRevoked() : false)
                            .sub(tmSubject.getSubject() != null ? tmSubject.getSubject() : null)
                            .expires(this.toInstant(tmSubject.getExpires()))
                            .granted(this.toInstant(tmSubject.getGranted()))
                            .build())
                        .toList()
                )
                .build()
        ).toList();

    return TrustMarkIssuerProperties.builder()
        .entityIdentifier(new EntityID(tmiModuleEntity.getEntity().getIssuer()))
        .trustMarkValidityDuration(this.toDuration(tmiModuleEntity.getTrustMarkTokenValidityDuration()))
        .trustMarks(trustMarks)
        .build();
  }


  private JWKSet toJwksSet(final String jwks) {
    try {
      return jwks == null ? null : JWKSet.parse(jwks);
    }
    catch (final java.text.ParseException e) {
      throw new RuntimeException("Unable to create JWKSet", e);
    }
  }

  private Instant toInstant(final OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : offsetDateTime.toInstant();
  }

  private Duration toDuration(final String duration) {
    return duration == null ? null : Duration.parse(duration);
  }
}
