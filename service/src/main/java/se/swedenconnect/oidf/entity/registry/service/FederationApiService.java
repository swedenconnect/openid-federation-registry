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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
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
   * Generates a signed JWT containing entity records for a specific instance ID.
   * This method fetches the entity records associated with the issuer from the repository,
   * signs the resulting records, and returns the signed JWT string.
   *
   * @param instanceId the unique identifier of the instance for which the entity records are retrieved
   * @return a signed JSON Web Token (JWT) string containing the entity records
   * @throws ResponseStatusException if the issuer is not set, if no entity records are found, or if signing fails
   */
  public String entityRecord(final UUID instanceId) {
    Optional.ofNullable(instanceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId is mandatory"));

    final List<EntityEntity> recordEntity = null; //TODO //this.entityRepository.findByIssuer(issuer.getValue());
    if (recordEntity.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Unable to find entity for issuer:'%s'".formatted(instanceId));
    }

    try {
      final String jwt = this.signJsonRecords("entity-records",
              recordEntity.stream()
                  .map(entityEntity -> "Not implemented yet")
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
   * Generates a signed JSON Web Token (JWT) representing the trust mark record for a specific instance.
   *
   * @param instanceId a unique identifier of the instance for which the trust mark record is requested
   * @return a JWT string containing the signed trust mark record data
   * @throws ResponseStatusException if any required input is missing, no records are found, or an error occurs
   * during token signing
   */
  @Transactional(readOnly = true)
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

    final Map<String, Object> policyClaim = Map.of("not", "implemented");
    //this.mapper.readValue(policyEntity.getPolicy(), new TypeReference<Map<String, Object>>() {});

      final String claimName = "policy_record";

      final String jwt = this.jwtSupport.signJWT(claimName, builder -> builder
              .claim(claimName, policyClaim)
              .expirationTime(new Date(System.currentTimeMillis() + this.jwkExpiryDuration.toMillis()))
              .issuer(this.jwkIssuer))
          .serialize();
      log.debug("Policy Signed JWT: {}", jwt);
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
        .map(this::toMap)
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

  private List<Map<String, Object>> resolveTrustmarks(final UUID instanceid) {

    final InstanceEntity instanceEntity = this.instanceRepository.findById(instanceid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No instance found for:%s".formatted(instanceid)));

    final List<ModuleEntity> trustmarkIssuersModules = instanceEntity.getOrganizations()
        .stream()
        .flatMap(organizationEntity -> organizationEntity.getModule().stream())
        .filter(moduleEntity -> FkKeyType.valueOf(moduleEntity.getModuleType()).equals(TRUSTMARKISSUER))
        .toList();

    return trustmarkIssuersModules.stream().map(this::toMapWithTrustMarks).toList();
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
        .filter(settingsEntity1 -> settingsEntity1.getKey().equals("entity-identifier"))
        .collect(Collectors.toMap(
            SettingsEntity::getKey,
            SettingsEntity::castValue
        ));

    settingsEntity.put("trust-marks",
        this.trustMarkService.listByModuleId(moduleEntity.getModuleId(), true));
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
