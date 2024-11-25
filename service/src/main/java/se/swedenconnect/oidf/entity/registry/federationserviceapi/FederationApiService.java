/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.EntityRepository;
import se.swedenconnect.oidf.entity.registry.policy.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.policy.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.trustmark.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.entity.registry.trustmark.TrustMarkSubjectRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to collect data to federation services
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class FederationApiService {

  private final ObjectMapper mapper = new ObjectMapper();
  private final JWK signKey;
  private final EntityRepository entityRepository;
  private final PolicyRepository policyRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;

  /**
   * Creating a FederationServiceApiService
   * @param entityRepository EntityRepository
   * @param signKey Key used to sign outgoing JWT
   * @param policyRepository PolicyRepository
   * @param trustMarkSubjectRepository TrustMarkSubjectRepository
   */
  public FederationApiService(final EntityRepository entityRepository, final JWK signKey,
      final PolicyRepository policyRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository) {
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.signKey = signKey;

  }

  /**
   * Getting entity records
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
      return signJsonRecords("entity-record",
          recordEntity.stream().map(EntityEntity::getEntity).toList()).serialize();
    }
    catch (JOSEException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign response", e);
    }
  }

  /**
   * Getting trustmarks
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
        this.trustMarkSubjectRepository.findByIssuerAndTrustmarkId(issuer.getValue(),trustmarkId)
            .stream()
            .filter(trustMarkSubjectEntity -> subject.isEmpty() ||
                trustMarkSubjectEntity.getSubject().equals(subject.get()))
            .toList();

    if (trustmarkSubjectEntities.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Unable to find trustMarkRecord for issuer:'%s',trustmark:'%s',subject:'%s'"
              .formatted(issuer,trustmarkId,subject));
    }
    try {
      return signJsonRecords("trustmark-record",
          trustmarkSubjectEntities.stream().map(TrustMarkSubjectEntity::getTrustmarksubjectJson).toList()).serialize();
    }
    catch (JOSEException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign response", e);
    }

  }

  /**
   * Getting one policyrecord according to policyid
   * @param policyId External policyid, expect UUID format
   * @return Signed JWT containing PolicyRecords
   */
  public String policyRecord(final UUID policyId) {

    final PolicyEntity policyEntity = this.policyRepository.findByExternalId(policyId.toString())
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Unable to find policy for id:'%s'".formatted(policyId.toString())));

    try {
      return signJsonRecords("policy-record", List.of(policyEntity.getPolicy())).serialize();
    }
    catch (JOSEException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign response", e);
    }

  }

  /**
   * Taking a list of json blobs and set it as a claim in JWT.
   * Claim is structured like this:
   * {
   *   "data": [
   *      {
   *        "fields":"From JsonRecords"
   *      }
   *   ]
   * }
   *
   * @param claimName Name of claim in JWT. It will also be set as type in JWT header
   * @param jsonRecords List och string Json blobs.
   * @return SignedJwt With keyid set from signed key.
   * @throws JOSEException If there is a problem with JWT signing
   */
  protected SignedJWT signJsonRecords(final String claimName, final List<String> jsonRecords) throws JOSEException {

    final List<Map<String,Object>> jsonClaims = jsonRecords.stream().map((js) -> {
      try {
        return this.mapper.readValue(js, new TypeReference<Map<String,Object>>() {});
      }
      catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }).toList();


    final RSASSASigner signer = new RSASSASigner(this.signKey.toRSAKey());
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .claim(claimName, Map.of("data",jsonClaims))
        .issueTime(new Date())
        .jwtID(UUID.randomUUID().toString())
        .issuer("oidf-registry")
        .build();

    final JWSAlgorithm alg = signer.supportedJWSAlgorithms().stream().findFirst().orElseThrow();
    final JWSHeader header = new JWSHeader.Builder(alg)
        .type(new JOSEObjectType(claimName + "+jwt"))
        .keyID(this.signKey.getKeyID())
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(signer);
    return jwt;
  }

}
