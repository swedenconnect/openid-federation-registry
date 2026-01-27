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

package se.swedenconnect.oidf.registry.controller;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.dto.oidfservice.EntityRecord;
import se.swedenconnect.oidf.registry.dto.oidfservice.ModuleRecord;
import se.swedenconnect.oidf.registry.dto.oidfservice.ResolverProperties;
import se.swedenconnect.oidf.registry.dto.oidfservice.TrustAnchorProperties;
import se.swedenconnect.oidf.registry.dto.oidfservice.gsonserde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.federationserviceapi.ModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.ResolverModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustAnchorModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustMarkIssuerModuleResponse;
import se.swedenconnect.oidf.registry.fixture.FederationAPIOperations;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.fixture.UseMariaDBContainer;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration test for the OIDF Federation Service API
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@UseMariaDBContainer
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OidfServiceApiControllerIT {

  @Autowired
  private TestRestTemplate restTemplate;
  @LocalServerPort
  private int port;

  @Autowired
  private TestDataOperations testDataOperations;
  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private FederationAPIOperations federationAPIOperations;

  final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader();

  final UUID instanceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  @BeforeEach
  void setUp() {
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost:" + this.port);

    // Configure authentication
    apiClient.setBearerToken(this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM));
    apiClient.setApiKey(JwtTestUtils.OrganisationType.PM.orgId);
    testDataOperations.createTestScenarioWithPolicyAndEntities(apiClient, "http://localhost:" + this.port);

  }

  @Test
  @DisplayName( "Entity record endpoint - should return a JWT with the expected structure")
  void entityRecordSuccess() throws ParseException, com.nimbusds.oauth2.sdk.ParseException {

    final SignedJWT signedJWT = federationAPIOperations.callEntity(instanceId);

    assertEquals(new JOSEObjectType("entity-records+jwt"), signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());
    final String entityRecordJson = signedJWT.getJWTClaimsSet().getClaimAsString("entity_records");
    assertNotNull(entityRecordJson);
    assertFalse(entityRecordJson.isEmpty());

    final List<EntityRecord> entityRecords = this.jsonRegistryLoader.parseEntityRecord(entityRecordJson);
    assertEquals(3, entityRecords.size());
    final Map<EntityID, EntityRecord> entityRecordMap =
        entityRecords.stream()
            .collect(Collectors.toMap(
                EntityRecord::getEntityIdentifier,
                entityRecord -> entityRecord
            ));

    final EntityRecord polisen = entityRecordMap.get(EntityID.parse("https://www.polisen.se/op/sverigeid"));
    assertEquals("https://www.polisen.se/op/sverigeid", polisen.getEntityIdentifier().toString());
    assertEquals("ec_location", polisen.getCrit().getFirst());
    assertNull(polisen.getJwks());
    assertEquals("https://www.pm.se/oidf/www_polisen_se_op_sverigeid/.well-known/openid-federation",
        polisen.getOverrideConfigurationLocation());
    assertEquals("https://www.pm.se/oidf/ta", polisen.getAuthorityHints().getFirst());

  }

  @Test
  @DisplayName( "Submodule record endpoint - should return a JWT with the expected structure")
  void submoduleRecordSuccess() throws ParseException {

    final SignedJWT signedJWT = this.federationAPIOperations.callSubmodule(instanceId);

    assertEquals(new JOSEObjectType("module-records+jwt"), signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());

    final String claim = signedJWT.getJWTClaimsSet().getClaimAsString("module_records");
    assertNotNull(claim);
    assertFalse(claim.isEmpty());

    final ModuleRecord moduleRecord = this.jsonRegistryLoader.parseModuleJson(claim);

    assertNotNull(moduleRecord.getResolvers());
    assertNotNull(moduleRecord.getTrustAnchors());

    final TrustAnchorProperties.SubordinateListingProperty sub = moduleRecord.getTrustAnchors()
        .getFirst()
        .getSubordinates()
        .stream()
        .filter(subordinateListingProperty ->
            subordinateListingProperty.getEntityIdentifier().toString().equals("https://www.polisen.se/op/sverigeid"))
        .findFirst()
        .orElseThrow();
    assertEquals("https://www.pm.se/oidf/www_polisen_se_op_sverigeid/.well-known/openid-federation",
        sub.getOverrideConfigurationLocation());
    assertEquals("ec_location", sub.getCrit().getFirst());

    assertNotNull(moduleRecord.getTrustMarkIssuers());

    assertFalse(moduleRecord.getResolvers().isEmpty(), "Expects resolver to be present in the response.");

    assertFalse(moduleRecord.getTrustAnchors().isEmpty());

    assertFalse(moduleRecord.getTrustMarkIssuers().isEmpty(), "TMI is expected");

  }

  @Test
  @DisplayName( "Entity record endpoint without missing instanceId - should return 404")
  void entityRecordNotFound() {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?instanceid=" + UUID.randomUUID(), String.class);
    if (response.getStatusCode().isError()) {
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

  }

  @Test
  @DisplayName( "Entity record endpoint with missing iss parameter - should return 400")
  void entityRecordBadRequest() {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=f", String.class);
    if (response.getStatusCode().isError()) {
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

  }

}