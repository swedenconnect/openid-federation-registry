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
package se.swedenconnect.oidf.registry.federationservice.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.entity.mapper.EntityToDtoMapper;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.model.ModuleRecord;
import se.swedenconnect.oidf.registry.federationservice.model.PolicyRecord;
import se.swedenconnect.oidf.registry.federationservice.model.ConstraintRecord;
import se.swedenconnect.oidf.registry.federationservice.model.NamingConstraints;
import se.swedenconnect.oidf.registry.federationservice.model.ResolverProperties;
import se.swedenconnect.oidf.registry.federationservice.model.TrustAnchorProperties;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkDelegation;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkProperties;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.registry.federationservice.serde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.module.model.Resolver;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIssuer;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.subordinate.dto.ConstraintsDto;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.mapper.SubordinateMapper;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    final Instance instanceEntity = this.instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceId)));

    final List<FederationEntity> entities = instanceEntity.getOrganizations().stream()
        .flatMap(organization -> organization.getEntities().stream())
        .filter(entity -> entity.getEntityType().equals(EntityType.FEDERATION_ENTITY))
        .toList();

    final List<TrustMarkIssuerProperties> tmi = entities.stream()
        .map(FederationEntity::getTrustmarkIssuer)
        .filter(Objects::nonNull)
        .filter(TrustMarkIssuer::getActive)
        .map(this::toTrustMarkIssuer)
        .toList();
    subModules.setTrustMarkIssuers(tmi);

    final List<TrustAnchorProperties> taIm = entities.stream()
        .map(FederationEntity::getTrustanchorIntermediate)
        .filter(Objects::nonNull)
        .filter(TrustAnchorIntermediateModule::getActive)
        .map(this::toTaIm)
        .toList();
    subModules.setTrustAnchors(taIm);

    final List<ResolverProperties> resolverEntities = entities.stream()
        .map(FederationEntity::getResolver)
        .filter(Objects::nonNull)
        .filter(Resolver::getActive)
        .map(this::toResolver)
        .toList();
    subModules.setResolvers(resolverEntities);

    return subModules;
  }

  private List<EntityRecord> resolveEntity(final UUID instanceId) {

    final Instance instanceEntity = this.instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceId)));

    final List<FederationEntity> entities = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organization -> organization.getEntities().stream())
        .toList();

    return entities.stream()
        .map(this.entityResponseFormatter::createEntityResponse)
        .filter(Objects::nonNull)
        .toList();

  }

  TrustAnchorProperties toTaIm(final TrustAnchorIntermediateModule taImModuleEntity) {
    return TrustAnchorProperties.builder()
        .entityIdentifier(new EntityID(taImModuleEntity.getEntity().getIssuer()))
        //.trustMarkOwners()
        .trustMarkIssuers(this.toTrustMarkIssuers(taImModuleEntity))
        .subordinates(taImModuleEntity.getSubordinates()
            .stream()
            .map(this::toSubordinates)
            .filter(Objects::nonNull)
            .toList())
        .build();
  }

  /**
   * Builds the trust_mark_issuers claim: for each trust mark type, the issuers that are trusted to issue it.
   * <p>
   * An entry with auto set takes the trust mark types from the trust marks the issuer has now, so the claim follows
   * the issuer. The issuer has to be an active trust mark issuer of the same organization as the trust anchor. If it
   * is not, for example if it has been removed or deactivated since, the entry is left out with a warning. The other
   * entries list their trust mark types and the issuer can be any entity.
   *
   * @param taImModuleEntity the trust anchor module
   * @return trust mark type to issuers, or {@code null} if there is nothing to export
   */
  private Map<EntityID, List<EntityID>> toTrustMarkIssuers(final TrustAnchorIntermediateModule taImModuleEntity) {
    final List<TrustAnchorIssuer> entries = taImModuleEntity.getTrustMarkIssuers();
    if (entries == null || entries.isEmpty()) {
      return null;
    }

    final Map<EntityID, List<EntityID>> trustMarkIssuers = new LinkedHashMap<>();
    for (final TrustAnchorIssuer entry : entries.stream()
        .sorted(Comparator.comparing(TrustAnchorIssuer::issuer)).toList()) {
      final List<String> trustMarkTypes = entry.auto()
          ? this.trustMarkTypesOf(taImModuleEntity, entry.issuer())
          : entry.trustMarkTypes();
      if (trustMarkTypes.isEmpty()) {
        log.warn("Trust mark issuer {} of trust anchor {} gives no trust mark types and is left out of "
                + "trust_mark_issuers. {}", entry.issuer(), taImModuleEntity.getEntity().getIssuer(),
            entry.auto()
                ? "It is not an active trust mark issuer with trust marks of the same organization."
                : "No trust mark types are listed.");
        continue;
      }
      trustMarkTypes.stream().distinct().sorted().forEach(type -> {
        final List<EntityID> issuers = trustMarkIssuers.computeIfAbsent(new EntityID(type), key -> new ArrayList<>());
        final EntityID issuer = new EntityID(entry.issuer());
        if (!issuers.contains(issuer)) {
          issuers.add(issuer);
        }
      });
    }
    return trustMarkIssuers.isEmpty() ? null : trustMarkIssuers;
  }

  private List<String> trustMarkTypesOf(final TrustAnchorIntermediateModule taImModuleEntity, final String issuer) {
    final Organization organization = taImModuleEntity.getOrganization();
    if (organization == null) {
      return List.of();
    }
    return this.entityRepository
        .findByOrganizationIdAndEntityKeyTypeAndIssuer(organization.getOrganizationId(),
            EntityType.FEDERATION_ENTITY, issuer)
        .map(FederationEntity::getTrustmarkIssuer)
        .filter(tmi -> Boolean.TRUE.equals(tmi.getActive()))
        .map(tmi -> tmi.getTrustmarks().stream()
            .map(TrustMark::getTrustmarkType)
            .filter(Objects::nonNull)
            .distinct()
            .toList())
        .orElse(List.of());
  }

  protected TrustAnchorProperties.SubordinateListingProperty toSubordinates(final Subordinate subordinateEntity) {
    final TrustAnchorProperties.SubordinateListingProperty sub = new TrustAnchorProperties.SubordinateListingProperty();
    final SubordinateDto subDto = SubordinateMapper.toDto(subordinateEntity);

    Map<String, Object> metadataPolicy = subDto.getMetadataPolicy();
    if (metadataPolicy != null && metadataPolicy.containsKey("metadata_policy")) {
      metadataPolicy = (Map<String, Object>) metadataPolicy.get("metadata_policy");
    }
    sub.setPolicy(new PolicyRecord(subDto.getSubordinateId().toString(), metadataPolicy));
    sub.setMetadata(subDto.getMetadata());
    sub.setJwks(this.toJwksSet(subDto.getJwks()));
    sub.setMetadataPolicyCrit(EmptyToNull.list(subDto.getMetadataPolicyCrit()));
    sub.setCrit(EmptyToNull.list(subDto.getCrit()));
    Optional.ofNullable(subDto.getConstraints()).map(this::toConstraintRecord).ifPresent(sub::setConstraints);
    sub.setEntityIdentifier(new EntityID(subDto.getEntityIdentifier()));
    // if autoresolve is marked true. System tries to get the hosted entity.
    // If not found this subordinate relation is removed since it can not be resolved
    if (subDto.getEcLocationAutomaticResolve()) {
      return this.entityRepository.findByEntityTypeAndOptionalIssuer(
              EntityType.HOSTED_ENTITY,
              subordinateEntity.getEntityidentifier())
          .stream().findFirst()
          .map(EntityToDtoMapper::toDtoHosted)
          .map(dto -> {
            sub.setCrit(EmptyToNull.list(dto.getCrit()));
            final String ecLocation = dto.getEffectiveEcLocation();
            sub.setVirtualEntityId(Optional.ofNullable(ecLocation).orElse(dto.getEntityIdentifier()));
            if (ecLocation != null) {
              sub.setOverrideConfigurationLocation(ecLocation + "/.well-known/openid-federation");
            }
            return sub;
          })
          .orElse(null);
    }
    sub.setVirtualEntityId(Optional.ofNullable(subDto.getEcLocation()).orElse(subDto.getEntityIdentifier()));
    return sub;
  }

  private ResolverProperties toResolver(final Resolver resolverEntity) {
    if (resolverEntity == null || resolverEntity.getActive() == null || !resolverEntity.getActive()) {
      return ResolverProperties.builder().build();
    }
    return ResolverProperties.builder()
        .entityIdentifier(resolverEntity.getEntity().getIssuer())
        .resolveResponseDuration(this.toDuration(resolverEntity.getResolveResponseDuration()))

        .trustAnchor(resolverEntity.getTrustAnchor())
        .trustedKeys(this.toJwksSet(resolverEntity.getTrustedKeys()))
        .useCachedValue(resolverEntity.getStepCachedValueThreshold())
        .build();
  }

  private TrustMarkIssuerProperties toTrustMarkIssuer(final TrustMarkIssuer tmiModuleEntity) {
    final List<TrustMarkProperties> trustMarks = tmiModuleEntity.getTrustmarks()
        .stream()
        .map(trustMarkEntity ->
            TrustMarkProperties.builder()
                .trustMarkType(new EntityID(trustMarkEntity.getTrustmarkType()))
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


  private JWKSet toJwksSet(final Map<String,Object> jwks) {
    try {
      return jwks == null ? null : JWKSet.parse(jwks);
    }
    catch (final java.text.ParseException e) {
      throw new RuntimeException("Unable to create JWKSet", e);
    }
  }

  private ConstraintRecord toConstraintRecord(final ConstraintsDto constraints) {
    return ConstraintRecord.builder()
        .maxPathLength(Optional.ofNullable(constraints.getMaxPathLength()).map(Integer::longValue).orElse(null))
        .naming(Optional.ofNullable(constraints.getNamingConstraints())
            .map(naming -> NamingConstraints.builder()
                .permitted(naming.getPermitted())
                .excluded(naming.getExcluded())
                .build())
            .orElse(null))
        .allowedEntityTypes(constraints.getAllowedEntityTypes())
        .build();
  }

  private Instant toInstant(final OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : offsetDateTime.toInstant();
  }

  private Duration toDuration(final String duration) {
    return duration == null || duration.isBlank() ? null : Duration.parse(duration);
  }
}
