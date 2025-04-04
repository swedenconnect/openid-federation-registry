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

package se.swedenconnect.oidf.registry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.config.RegistryProperties;
import se.swedenconnect.oidf.registry.federationserviceapi.ModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.ResolverModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustAnchorModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustMarkIssuerModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.records.EntityRecord;
import se.swedenconnect.oidf.registry.fixture.FederationAPIOperations;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing federation api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FederationServiceApiControllerIT {
  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private RegistryProperties registryProperties;

  @Autowired
  private TestDataOperations testDataOperations;

  @Autowired
  private FederationAPIOperations federationAPIOperations;

  /**
   * Test method for verifying the successful creation, retrieval, and validation of a TrustMarkSubjectRecord.
   *
   * @throws ParseException if there is an error in parsing the JWT or its claims.
   */
  @Test
  void trustMarkRecordSuccess() throws ParseException, JsonProcessingException {

    final UUID instanceId = setupTestData();
    final SignedJWT tms = federationAPIOperations.callTrustMark(instanceId);
    final JWTClaimsSet claimsSet = tms.getJWTClaimsSet();
    final List<Object> records = claimsSet.getListClaim("trustmark_records");
    records.stream().forEach(claimMap -> {
      TrustMarkIssuerModuleResponse.TrustMarkResponse.fromJson((Map<String, Object>) claimMap).validate();
    });

  }

  private @NotNull UUID setupTestData() throws JsonProcessingException {
    final JwtTestUtils.OrganisationType org = JwtTestUtils.OrganisationType.PM;

    final UUID policyId = testDataOperations.createPolicies(org);

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder().policyId(policyId)
            .build());

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID resolverId = testDataOperations.createResolver(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        TestDataOperations.defaultResolver(entityId));

    final UUID trustanchor = testDataOperations.createTrustAnchor(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.TrustAnchorTestData.builder().entityId(entityId).build());

    final UUID trustmarkId = testDataOperations.createTrustMark(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.createTrustMarkSubject(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkSubject(trustmarkId));

    testDataOperations.createSubordinateEntity(UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .build());


    return registryProperties.instances().stream().findFirst()
        .map(instanceProperties -> instanceProperties.instanceId().toString())
        .map(UUID::fromString).orElseThrow();
  }

  @Test
  void entityRecordSuccess() throws ParseException, JsonProcessingException {
    final UUID instanceId = setupTestData();

    final SignedJWT signedJWT = federationAPIOperations.callEntity(instanceId);

    assertEquals(new JOSEObjectType("entity-records+jwt"), signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());
    System.out.println(signedJWT.getJWTClaimsSet());
    final List<Object> claim = signedJWT.getJWTClaimsSet().getListClaim("entity_records");
    assertNotNull(claim);
    assertFalse(claim.isEmpty());
    claim.stream().forEach(claimMap -> {
          try {
            EntityRecord.fromJson((Map<String, Object>) claimMap);
          }
          catch (ParseException e) {
            throw new RuntimeException(e);
          }
        });

  }

  @Test
  void submoduleRecordSuccess() throws ParseException, JsonProcessingException {

    final UUID instanceId = setupTestData();
    final SignedJWT signedJWT = this.federationAPIOperations.callSubmodule(instanceId);

    assertEquals(new JOSEObjectType("module-records+jwt"), signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());

    final Map<String, Object> claim = signedJWT.getJWTClaimsSet().getJSONObjectClaim("module_records");
    log.info("Claim:{}", claim.toString());
    assertNotNull(claim);
    assertFalse(claim.isEmpty());
    final ModuleResponse moduleResponse = ModuleResponse.fromJson(claim);
    assertNotNull(moduleResponse.getResolvers());
    assertNotNull(moduleResponse.getTrustAnchors());
    assertNotNull(moduleResponse.getTrustMarkIssuers());

    assertFalse(moduleResponse.getResolvers().isEmpty());
    moduleResponse.getResolvers().forEach(ResolverModuleResponse::validate);

    assertFalse(moduleResponse.getTrustAnchors().isEmpty());
    moduleResponse.getTrustAnchors().forEach(TrustAnchorModuleResponse::validate);

    assertFalse(moduleResponse.getTrustMarkIssuers().isEmpty());
    moduleResponse.getTrustMarkIssuers().forEach(TrustMarkIssuerModuleResponse::validate);

  }

  @Test
  void entityRecordNotFound() {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?instanceid=" + UUID.randomUUID(), String.class);
    if (response.getStatusCode().isError()) {
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

  }

  @Test
  void entityRecordBadRequest() {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=f", String.class);
    if (response.getStatusCode().isError()) {
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

  }


}