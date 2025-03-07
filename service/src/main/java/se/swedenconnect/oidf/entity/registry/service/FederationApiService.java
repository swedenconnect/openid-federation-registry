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
package se.swedenconnect.oidf.entity.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.entity.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.repository.TrustMarkSubjectRepository;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.INTERMEDIATE;
import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.RESOLVER;
import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.TRUSTANCHOR;
import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.TRUSTMARKISSUER;

/**
 * Service to collect data to federation services
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class FederationApiService {

  private final ObjectMapper mapper;
  private final EntityRepository entityRepository;
  private final PolicyRepository policyRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final InstanceRepository instanceRepository;
  private final String jwkIssuer;
  private final Duration jwkExpiryDuration;
  private final OptionsCRUDTrustMark trustMarkService;
  private final JWTSupport jwtSupport;

  /**
   * Constructs a FederationApiService instance to handle OpenID Connect Federation related operations.
   *
   * @param entityRepository the repository for managing entity records
   * @param signKey the JSON Web Key (JWK) used for signing operations
   * @param policyRepository the repository for managing policy records
   * @param trustMarkSubjectRepository the repository for managing trust mark subject records
   * @param instanceRepository the repository for managing instances
   * @param jwkIssuer the issuer associated with the JSON Web Key (JWK)
   * @param mapper the object mapper for JSON processing
   * @param trustMarkService OptionsCRUDTrustMark
   * @param jwkExpiryDuration jwkExpiryDuration
   */
  public FederationApiService(
      final EntityRepository entityRepository,
      final JWK signKey,
      final PolicyRepository policyRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final String jwkIssuer,
      final ObjectMapper mapper,
      final InstanceRepository instanceRepository,
      final OptionsCRUDTrustMark trustMarkService,
      final Duration jwkExpiryDuration) {
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.jwkIssuer = jwkIssuer;
    this.mapper = mapper;
    this.instanceRepository = instanceRepository;
    this.trustMarkService = trustMarkService;
    this.jwkExpiryDuration = jwkExpiryDuration;
    this.jwtSupport = new JWTSupport(signKey);
  }

  /**
   * Getting entity records
   *
   * @param issuer Issuer id
   * @return SignedJWT with Entitys
   */
  public String entityRecord(final EntityID issuer) {
    Optional.ofNullable(issuer)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issuer is mandatory"));

    final List<EntityEntity> recordEntity = this.entityRepository.findByIssuer(issuer.getValue());
    if (recordEntity.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Unable to find entity for issuer:'%s'".formatted(issuer));
    }
    try {
      final String jwt = this.signJsonRecords("entity-records",
              recordEntity.stream()
                  .map(EntityEntity::getEntity)
                  .toList())
          .serialize();
      log.debug("Entity Signed JWT: {}", jwt);
      return jwt;
    }
    catch (final JOSEException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign response", e);
    }
  }

  /**
   * Getting trustmarks
   *
   * @param issuer Issuer
   * @param trustmarkId Trustmarkid
   * @param subject Subject
   * @return Signed JWT containing list of trustmarks
   */
  public String trustMarkRecord(
      final EntityID issuer,
      final String trustmarkId,
      final Optional<String> subject) {
    Optional.ofNullable(issuer)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issuer is mandatory"));
    Optional.ofNullable(trustmarkId)
        .filter(id -> !id.isBlank())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "trustmarkId is mandatory"));

    final List<TrustMarkSubjectEntity> trustmarkSubjectEntities =
        this.trustMarkSubjectRepository.findByIssuerAndTrustmarkId(issuer.getValue(), trustmarkId)
            .stream()
            .filter(trustMarkSubjectEntity -> subject.isEmpty() ||
                trustMarkSubjectEntity.getSubject().equals(subject.get()))
            .toList();

    if (trustmarkSubjectEntities.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Unable to find trustMarkRecord for issuer:'%s',trustmark:'%s',subject:'%s'"
              .formatted(issuer, trustmarkId, subject));
    }
    try {
      final String jwt = this.signJsonRecords("trustmark-records",
          trustmarkSubjectEntities.stream().map(TrustMarkSubjectEntity::getTrustmarksubjectJson).toList()).serialize();
      log.debug("TrustMark JWT: {}", jwt);
      return jwt;
    }
    catch (final JOSEException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign response", e);
    }

  }

  /**
   * Getting one policyrecord according to policyRecordId
   *
   * @param policyRecordId External policyRecordId, expect UUID format
   * @return Signed JWT containing PolicyRecords
   */
  public String policyRecord(final UUID policyRecordId) {
    Assert.notNull(policyRecordId, "policyRecordId is mandatory");
    final PolicyEntity policyEntity = this.policyRepository.findById(policyRecordId)
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Unable to find policy for id:'%s'".formatted(policyRecordId.toString())));
    try {
      final Map<String, Object> policyClaim =
          this.mapper.readValue(policyEntity.getPolicy(), new TypeReference<Map<String, Object>>() {});

      final String claimName = "policy_record";

      final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
              .claim(claimName, policyClaim)
              .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
              .issuer(this.jwkIssuer))
          .serialize();
      log.debug("Policy Signed JWT: {}", jwt);
      return jwt;
    }
    catch (final JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign response", e);
    }

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
            .claim(claimName, this.resolveSubmodules(instanceId))
            .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
            .issuer(this.jwkIssuer))
        .serialize();
    log.debug("Submodule Signed JWT: {}", jwt);
    return jwt;

  }

  private Map<String, List<Map<String, Object>>> resolveSubmodules(final UUID instanceid) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<ModuleEntity> moduleEntities = instanceEntity.getOrganizations().stream()
        .flatMap(organizationEntity -> organizationEntity.getModule().stream())
        .toList();

    final List<Map<String, Object>> tmi = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTMARKISSUER))
        .map(this::toMapWithTrustMarks)
        .toList();

    final List<Map<String, Object>> ta = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTANCHOR) ||
            FkKeyType.valueOf(moduleEntity.getModuleType()).equals(INTERMEDIATE))
        .map(this::toMap)
        .toList();

    final List<Map<String, Object>> resolver = moduleEntities.stream()
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(RESOLVER))
        .map(this::toMap)
        .toList();

    final Map<String, List<Map<String, Object>>> data = new HashMap<>();
    data.put("trust-mark-issuers", tmi);
    data.put("trust-anchors", ta);
    data.put("resolvers", resolver);
    return data;

  }

  private Map<String, Object> toMap(final ModuleEntity moduleEntity) {
    return moduleEntity.getSettingsEntityList()
        .stream()
        .collect(Collectors.toMap(
            SettingsEntity::getKey,
            SettingsEntity::castValue
        ));
  }

  private Map<String, Object> toMapWithTrustMarks(final ModuleEntity moduleEntity) {
    final Map<String, Object> settingsEntity = moduleEntity.getSettingsEntityList()
        .stream()
        .collect(Collectors.toMap(
            SettingsEntity::getKey,
            SettingsEntity::castValue
        ));

    settingsEntity.put("trust-marks", this.trustMarkService.listByModuleId(moduleEntity.getModuleId()));
    return settingsEntity;
  }

  /**
   * Taking a list of json blobs and set it as a claim in JWT. Claim is structured like this: { "data": [ {
   * "fields":"From JsonRecords" } ] }
   *
   * @param claimName Name of claim in JWT. It will also be set as type in JWT header
   * @param jsonRecords List och string Json blobs.
   * @return SignedJwt With keyid set from signed key.
   * @throws JOSEException If there is a problem with JWT signing
   */
  protected SignedJWT signJsonRecords(final String claimName, final List<String> jsonRecords) throws JOSEException {

    final List<Map<String, Object>> jsonClaimsData = jsonRecords.stream().map((js) -> {
      try {
        return this.mapper.readValue(js, new TypeReference<Map<String, Object>>() {});
      }
      catch (final JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }).toList();

    return this.jwtSupport.signJWT(claimName, builder -> builder
        .claim(claimName.replace('-', '_'), jsonClaimsData)
        .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
        .issuer(this.jwkIssuer));
  }

}
