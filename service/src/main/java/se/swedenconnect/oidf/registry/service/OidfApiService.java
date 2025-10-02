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
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.dto.OidfServiceSubModules;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.registry.entity.FkKeyType.INTERMEDIATE;
import static se.swedenconnect.oidf.registry.entity.FkKeyType.RESOLVER;
import static se.swedenconnect.oidf.registry.entity.FkKeyType.TRUSTANCHOR;
import static se.swedenconnect.oidf.registry.entity.FkKeyType.TRUSTMARKISSUER;

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
  private final String jwkIssuer;
  private final Duration jwkExpiryDuration;
  private final JWTSupport jwtSupport;
  private final EntityResponseFormatter entityResponseFormatter = new EntityResponseFormatter();

  /**
   * Constructs a FederationApiService instance to handle OpenID Connect Federation related operations.
   *
   * @param signKey the JSON Web Key (JWK) used for signing operations
   * @param policyRepository the repository for managing policy records
   * @param instanceRepository the repository for managing instances
   * @param jwkIssuer the issuer associated with the JSON Web Key (JWK)
   * @param jwkExpiryDuration jwkExpiryDuration
   */
  public OidfApiService(
      final JWK signKey,
      final PolicyRepository policyRepository,
      final String jwkIssuer,
      final InstanceRepository instanceRepository,
      final Duration jwkExpiryDuration) {
    this.policyRepository = policyRepository;
    this.jwkIssuer = jwkIssuer;
    this.instanceRepository = instanceRepository;
    this.jwkExpiryDuration = jwkExpiryDuration;
    this.jwtSupport = new JWTSupport(signKey);
  }

  /**
   * Generates a signed JWT containing entity records for a specific instance ID. This method fetches the entity records
   * associated with the issuer from the repository, signs the resulting records, and returns the signed JWT string.
   *
   * @param instanceId the unique identifier of the instance for which the entity records are retrieved
   * @return a signed JSON Web Token (JWT) string containing the entity records
   * @throws ResponseStatusException if the issuer is not set, if no entity records are found, or if signing fails
   */
  public String entityRecord(final UUID instanceId) {
    Assert.notNull(instanceId, "InstanceId is mandatory");
    final String claimName = "entity_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.resolveEntity(instanceId))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("trustMarkRecord Signed JWT: {}", jwt);
    return jwt;

  }

  /**
   * Generates a signed JSON Web Token (JWT) representing the trust mark record for a specific instance.
   *
   * @param instanceId a unique identifier of the instance for which the trust mark record is requested
   * @return a JWT string containing the signed trust mark record data
   * @throws ResponseStatusException if any required input is missing, no records are found, or an error occurs
   *     during token signing
   */
  public String trustMarkRecord(final UUID instanceId) {
    Assert.notNull(instanceId, "instanceId is mandatory");
    final String claimName = "trustmark_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.resolveTrustmarks(instanceId))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("trustMarkRecord Signed JWT: {}", jwt);
    return jwt;

  }

  /**
   * Retrieves submodule records using the provided instance identifier.
   *
   * @param instanceId the unique identifier of the instance for which the submodule records are requested;
   * @return a signed JWT string containing claims for the submodule record.
   * @throws IllegalArgumentException if the instanceId is null.
   * @throws ResponseStatusException if an error occurs during the signing of the response.
   */
  public String submoduleRecord(final UUID instanceId) {
    Assert.notNull(instanceId, "instanceId is mandatory");
    final String claimName = "module_records";
    final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
            .claim(claimName, this.resolveSubmodulesV2(instanceId))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("Submodule Signed JWT: {}", jwt);
    return jwt;
  }

  private OidfServiceSubModules resolveSubmodulesV2(final UUID instanceid) {

    final OidfServiceSubModules.OidfServiceSubModulesBuilder subModules = OidfServiceSubModules.builder();

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<ModuleEntity> moduleEntities = instanceEntity.getOrganizations().stream()
        .flatMap(organizationEntity -> organizationEntity.getModule().stream())
        .toList();

    final List<OidfServiceSubModules.TrustMarkIssuer> tmi = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTMARKISSUER))
        .map(this::toTrustMarkIssuer)
        .toList();
    subModules.trustMarkIssuers(tmi);

    final List<OidfServiceSubModules.TrustAnchor> taIm = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTANCHOR) ||
            FkKeyType.valueOf(moduleEntity.getModuleType()).equals(INTERMEDIATE))
        .map(this::toTaIm)
        .toList();
    subModules.trustAnchors(taIm);

    final List<OidfServiceSubModules.Resolver> res = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(RESOLVER))
        .map(this::toResolver)
        .toList();
    subModules.resolvers(res);

    return subModules.build();

  }

  private List<Map<String, Object>> resolveTrustmarks(final UUID instanceid) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<ModuleEntity> trustmarkIssuersModules = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organizationEntity -> organizationEntity.getModule().stream())
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTMARKISSUER))
        .toList();

    return trustmarkIssuersModules.stream()
        .map(moduleEntity -> this.listTrustMarksByModuleId(moduleEntity, true))
        .flatMap(List::stream)
        .toList();
  }

  private List<Map<String, Object>> resolveEntity(final UUID instanceid) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<EntityEntity> trustmarkIssuersModules = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organizationEntity -> organizationEntity.getEntities().stream())
        .toList();

    return trustmarkIssuersModules.stream().map(this::toMapEntity).toList();
  }

  private Map<String, Object> toMapEntity(final EntityEntity entity) {

    final Map<String, Object> settingsEntity = new HashMap<>(this.entityResponseFormatter.createEntityResponse(entity));

    settingsEntity.put(POLICY_RECORD, Collections.emptyMap());
    Optional.ofNullable(settingsEntity.remove(POLICY_ID))
        .filter(key -> !key.toString().isBlank())
        .flatMap(policyId -> this.policyRepository.findById(UUID.fromString(policyId.toString())))
        .ifPresent(policy ->
            settingsEntity.put(POLICY_RECORD,
                policy.getSettingsEntity(POLICY_ATT).map(SettingsEntity::castValue).orElseThrow()));
    return settingsEntity;
  }

  private Map<String, Object> toMapEntity(final ModuleEntity moduleEntity) {
    final Map<String, Object> module = moduleEntity.getSettingsEntityList()
        .stream()
        .collect(Collectors.toMap(
            SettingsEntity::getKey,
            SettingsEntity::castValue
        ));
    module.put(ENTITY_IDENTIFIER, moduleEntity.getEntity().getSubject());
    return module;
  }

  private OidfServiceSubModules.TrustAnchor toTaIm(final ModuleEntity taImModuleEntity) {
    if (!taImModuleEntity.getSettingsEntity("active")
        .map(settingsEntity -> (Boolean) settingsEntity.castValue()).orElse(true)) {
      return OidfServiceSubModules.TrustAnchor.builder().build();
    }

    return OidfServiceSubModules.TrustAnchor.builder()
        .entityIdentifier(taImModuleEntity.getEntity().getSubject())
        .trustMarkIssuer(taImModuleEntity.getSettingsEntity("trust_mark_issuer")
            .map(SettingsEntity::getValue)
            .orElse(null))
        .build();
  }

  private OidfServiceSubModules.Resolver toResolver(final ModuleEntity resolverModuleEntity) {
    if (!resolverModuleEntity.getSettingsEntity("active")
        .map(settingsEntity -> (Boolean) settingsEntity.castValue()).orElse(true)) {
      return OidfServiceSubModules.Resolver.builder().build();
    }

    return OidfServiceSubModules.Resolver.builder()
        .entityIdentifier(resolverModuleEntity.getEntity().getSubject())
        .resolveResponseDuration(resolverModuleEntity.getSettingsEntity("resolve_response_duration")
            .map(SettingsEntity::getValue)
            .orElse(null))
        .trustAnchors(resolverModuleEntity.getSettingsEntity("trust_anchor")
            .map(SettingsEntity::getValue)
            .orElseThrow())
        .trustedKeys(resolverModuleEntity.getSettingsEntity("trusted_keys")
            .map(SettingsEntity::getValue)
            .orElseThrow())
        .stepRetryTime(resolverModuleEntity.getSettingsEntity("step_retry_duration")
            .map(SettingsEntity::getValue)
            .orElse(null))
        .build();
  }

  private OidfServiceSubModules.TrustMarkIssuer toTrustMarkIssuer(final ModuleEntity tmiModuleEntity) {
    if (!tmiModuleEntity.getSettingsEntity("active")
        .map(settingsEntity -> (Boolean) settingsEntity.castValue()).orElse(true)) {
      return OidfServiceSubModules.TrustMarkIssuer.builder().build();
    }
    final List<OidfServiceSubModules.TrustMarkIssuer.TrustMark> trustMarks = tmiModuleEntity.getTrustmarks()
        .stream()
        .map(trustMarkEntity ->
            OidfServiceSubModules.TrustMarkIssuer.TrustMark.builder()
                .trustMarkEntityId(trustMarkEntity.getSettingsEntity("trust_mark_entity_id")
                    .orElseThrow().getValue())
                .ref(trustMarkEntity.getSettingsEntity("ref")
                    .map(SettingsEntity::getValue)
                    .orElse(null))
                .logoUri(trustMarkEntity.getSettingsEntity("logoUri")
                    .map(SettingsEntity::getValue)
                    .orElse(null))
                .delegation(trustMarkEntity.getSettingsEntity("delegation")
                    .map(SettingsEntity::getValue)
                    .orElse(null))
                .trustMarkSubjects(
                    trustMarkEntity.getTrustmarksubjects()
                        .stream()
                        .filter(tmSubject -> tmSubject.getSettingsEntity("revoked").isPresent())
                        .map(tmSubject -> OidfServiceSubModules
                            .TrustMarkIssuer.TrustMark.TrustMarkSubject.builder()
                            .revoked(tmSubject.getSettingsEntity("revoked")
                                .map(SettingsEntity::getValue)
                                .map(Boolean::valueOf)
                                .orElse(false))
                            .subject(tmSubject.getSettingsEntity("subject")
                                .map(SettingsEntity::getValue)
                                .orElseThrow())
                            .expires(tmSubject.getSettingsEntity("expires")
                                .map(SettingsEntity::getValue)
                                .map(Instant::parse)
                                .map(Instant::toString)
                                .orElse(null))
                            .granted(tmSubject.getSettingsEntity("granted")
                                .map(SettingsEntity::getValue)
                                .map(Instant::parse)
                                .map(Instant::toString)
                                .orElse(null))
                            .build())
                        .toList()
                )
                .build()
        ).toList();

    return OidfServiceSubModules.TrustMarkIssuer.builder()
        .entityIdentifier(tmiModuleEntity.getEntity().getSubject())
        .trustMarkTokenValidityDuration(
            tmiModuleEntity.getSettingsEntity("trust_mark_token_validity_duration")
                .orElseThrow().getValue())
        .trustMarks(trustMarks)
        .build();
  }

  private Map<String, Object> toMapWithTrustMarks(final ModuleEntity moduleEntity) {
    final Map<String, Object> trustmarkissuer = this.toMapEntity(moduleEntity);
    trustmarkissuer.put(TRUST_MARKS, this.listTrustMarksByModuleId(moduleEntity, true)
        .stream()
        .toList());
    return trustmarkissuer;
  }

  private List<Map<String, Object>> listTrustMarksByModuleId(final ModuleEntity moduleEntity,
      final boolean includeSubjects) {
    return moduleEntity.getTrustmarks()
        .stream()
        .map(trustMarkEntity -> {
              final Map<String, Object> e =
                  trustMarkEntity.getSettingsEntityList()
                      .stream()
                      .collect(Collectors.toMap(
                          SettingsEntity::getKey,
                          SettingsEntity::castValue
                      ));

          if (includeSubjects) {
            e.put(TRUST_MARK_SUBJECTS,
                    trustMarkEntity.getTrustmarksubjects()
                        .stream()
                        .map(trustMarkSubjectEntity ->
                            trustMarkSubjectEntity.getSettingsEntityList()
                                .stream()
                                .collect(Collectors.toMap(
                                    SettingsEntity::getKey,
                                    SettingsEntity::castValue
                                )))
                        .toList());
              }
              return e;
            }
        )
        .toList();
  }

}
