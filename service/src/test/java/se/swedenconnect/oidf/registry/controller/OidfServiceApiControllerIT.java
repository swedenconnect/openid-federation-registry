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
import com.nimbusds.jwt.SignedJWT;
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
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.federationserviceapi.ModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.ResolverModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustAnchorModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustMarkIssuerModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.records.EntityRecord;
import se.swedenconnect.oidf.registry.fixture.FederationAPIOperations;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.fixture.UseMariaDBContainer;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
  void entityRecordSuccess() throws ParseException {

    final SignedJWT signedJWT = federationAPIOperations.callEntity(instanceId);

    assertEquals(new JOSEObjectType("entity-records+jwt"), signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());
    System.out.println(signedJWT.getJWTClaimsSet().toJSONObject().toString());
    final List<Object> claim = signedJWT.getJWTClaimsSet().getListClaim("entity_records");
    assertNotNull(claim);
    assertFalse(claim.isEmpty());
    claim.forEach(claimMap -> {
      try {
        EntityRecord.fromJson((Map<String, Object>) claimMap);
      }
      catch (ParseException e) {
        throw new RuntimeException(e);
      }
    });

  }

  @Test
  @DisplayName( "Submodule record endpoint - should return a JWT with the expected structure")
  void submoduleRecordSuccess() throws ParseException {

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

    assertFalse(moduleResponse.getResolvers().isEmpty(), "Expects resolver to be present in the response.");
    moduleResponse.getResolvers().forEach(ResolverModuleResponse::validate);

    assertFalse(moduleResponse.getTrustAnchors().isEmpty());
    moduleResponse.getTrustAnchors().forEach(TrustAnchorModuleResponse::validate);

    assertFalse(moduleResponse.getTrustMarkIssuers().isEmpty(), "TMI is expected");
    moduleResponse.getTrustMarkIssuers().forEach(TrustMarkIssuerModuleResponse::validate);

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