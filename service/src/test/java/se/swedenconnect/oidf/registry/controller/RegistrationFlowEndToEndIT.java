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
 * limitations under the License.
 */
package se.swedenconnect.oidf.registry.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.FederationRegistrationApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.AssignFlowResponse;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowInformation;
import se.swedenconnect.oidf.registry.api.model.RegistrationJoinRequest;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.infrastructure.auth.AuthConstants;
import se.swedenconnect.oidf.registry.registrations.dto.FedRegStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test that:
 * <ol>
 *   <li>orgA (PM) creates a registration flow backed by PredefinedDirectRegisterFlow</li>
 *   <li>orgA creates an intermediate (TrustAnchor) and assigns the flow to it</li>
 *   <li>Ten separate organisations (testOrg1-10) each verify the flow is available and execute it</li>
 *   <li>WireMock (HTTPS) serves a valid entity-statement JWT for each registering entity</li>
 *   <li>Verifies that orgA ends up with 10 approved registrations and 10 subordinates</li>
 * </ol>
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "openid.federation.registry.entity-configuration-loader.enabled=true",
        "openid.federation.registry.entity-configuration-loader.enable-local-ip-address-ranges=true",
        "openid.federation.registry.entity-configuration-loader.trust-bundle-alias=wiremock-trust",
        "spring.ssl.bundle.jks.wiremock-trust.truststore.location=classpath:wiremock-keystore.p12",
        "spring.ssl.bundle.jks.wiremock-trust.truststore.password=Test1234",
        "spring.ssl.bundle.jks.wiremock-trust.truststore.type=PKCS12"
    })
@Testcontainers
@AutoConfigureRestTestClient
class RegistrationFlowEndToEndIT {

  private static final UUID PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID =
      UUID.fromString("AE67B1D8-2DCF-4A8C-9E6B-FC972CC65DEA");

  private static final int WIREMOCK_HTTPS_PORT = 6890;
  private static final int WIREMOCK_HTTP_PORT = 6789;
  private static final int NUMBER_OF_ORGS = 10;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private WireMockServer wireMockServer;

  private ApiClient pmApiClient;
  private UUID taImId;
  private UUID assignId;

  @BeforeEach
  void setUp() throws Exception {
    startWireMock();
    stubEntityStatementsForAllTestOrgs();
    setUpPmInfrastructure();
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void tenOrganisationsRegisterAndOrgASeesAllRegistrationsAndSubordinates() {
    final JwtTestUtils.OrganisationType[] testOrgs = {
        JwtTestUtils.OrganisationType.TESTORG1,
        JwtTestUtils.OrganisationType.TESTORG2,
        JwtTestUtils.OrganisationType.TESTORG3,
        JwtTestUtils.OrganisationType.TESTORG4,
        JwtTestUtils.OrganisationType.TESTORG5,
        JwtTestUtils.OrganisationType.TESTORG6,
        JwtTestUtils.OrganisationType.TESTORG7,
        JwtTestUtils.OrganisationType.TESTORG8,
        JwtTestUtils.OrganisationType.TESTORG9,
        JwtTestUtils.OrganisationType.TESTORG10,
    };

    for (int i = 0; i < NUMBER_OF_ORGS; i++) {
      final JwtTestUtils.OrganisationType org = testOrgs[i];
      final int orgNumber = i + 1;
      final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/testOrg" + orgNumber;

      final ApiClient orgApiClient = buildApiClient(org);
      final FederationRegistrationApi registrationApi = new FederationRegistrationApi(orgApiClient);

      final List<RegistrationFlowInformation> availableFlows = registrationApi.listFlows();
      assertThat(availableFlows)
          .as("testOrg%d should see the flow assigned by PM", orgNumber)
          .anyMatch(f -> assignId.equals(f.getJoinId()));

      final Registration status = registrationApi.createJoinWithId(
          assignId,
          new RegistrationJoinRequest().entityIdentifier(entityId));

      assertThat(status.getStatusFedreg())
          .as("Registration for testOrg%d should be APPROVED immediately", orgNumber)
          .isEqualTo(Registration.StatusFedregEnum.APPROVED);

      final List<Registration> registrations = registrationApi.listRegistrations();
      assertThat(registrations).hasSize(1);
    }

    verifyOrgASeesAllRegistrationsAndSubordinates();
  }

  private void verifyOrgASeesAllRegistrationsAndSubordinates() {
    final RestClient pmRestClient = buildPmRestClient();

    final List<Registration> registrations = pmRestClient.method(HttpMethod.GET)
        .uri("/registration-admin/v1")
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});

    assertThat(registrations)
        .as("PM should see 10 registrations under its intermediate")
        .hasSize(NUMBER_OF_ORGS);
    assertThat(registrations)
        .as("All registrations should be APPROVED")
        .allMatch(r -> Registration.StatusFedregEnum.APPROVED.equals(r.getStatusFedreg()));

    final ModulesApi modulesApi = new ModulesApi(pmApiClient);
    final TrustAnchor trustAnchor = modulesApi.getTrustAnchor(taImId);
    assertThat(trustAnchor.getSubordinates())
        .as("PM's TrustAnchor should have 10 subordinates")
        .hasSize(NUMBER_OF_ORGS);
  }

  private void startWireMock() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("registration-e2e", false)));
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());
  }

  private void stubEntityStatementsForAllTestOrgs() throws Exception {
    for (int n = 1; n <= NUMBER_OF_ORGS; n++) {
      final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/testOrg" + n;
      final String jwt = buildEntityStatementJwt(entityId);
      stubFor(get(urlPathEqualTo("/testOrg" + n + "/.well-known/openid-federation"))
          .willReturn(ok(jwt).withHeader("Content-Type", "application/entity-statement+jwt")));
    }
  }

  private void setUpPmInfrastructure() {
    pmApiClient = buildApiClient(JwtTestUtils.OrganisationType.PM);

    final EntitiesApi entitiesApi = new EntitiesApi(pmApiClient);
    final ModulesApi modulesApi = new ModulesApi(pmApiClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(pmApiClient);

    final FederationEntity taEntity = entitiesApi.createFederationEntity(
        FederationEntity.builder()
            .entityIdentifier("https://www.pm.se/oidf/ta/e2e-test")
            .build());

    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(
        TrustAnchor.builder()
            .entityId(taEntity.getEntityId())
            .active(true)
            .build());
    taImId = trustAnchor.getTrustAnchorId();

    final RegistrationFlowDto flow = flowApi.createFlow(
        new RegistrationFlowDto()
            .name("E2E Direct Registration Flow")
            .description("PredefinedDirectRegisterFlow for end-to-end test")
            .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
            .steps(List.of(new StepDto()
                .stepId(PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID)
                .name("PredefinedDirectRegisterFlow")
                .description("Loads entity configuration and publishes subordinate statement"))));

    final AssignFlowResponse assignment = flowApi.assignFlow(
        taImId,
        new AssignFlowRequest().flowId(flow.getFlowId()));
    assignId = assignment.getAssignId();

    log.info("PM infrastructure ready: taImId={}, flowId={}, assignId={}",
        taImId, flow.getFlowId(), assignId);
  }

  private ApiClient buildApiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    client.setApiKey(org.orgId);
    return client;
  }

  private RestClient buildPmRestClient() {
    return RestClient.builder()
        .baseUrl("http://localhost:" + this.port)
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM))
        .defaultHeader(AuthConstants.SELECTED_ORG_NUMBER_HEADER_ATTRIBUTE,
            JwtTestUtils.OrganisationType.PM.orgId)
        .build();
  }

  private String buildEntityStatementJwt(final String entityId) throws Exception {
    final ECKey signingKey = (ECKey) TestDataOperations.genKey();

    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(entityId)
        .subject(entityId)
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .claim("jwks", new JWKSet(signingKey.toPublicJWK()).toJSONObject())
        .claim("metadata", Map.of(
            "openid_relying_party", Map.of(
                "redirect_uris", List.of(entityId + "/callback"),
                "token_endpoint_auth_method", "private_key_jwt")))
        .build();

    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(signingKey.getKeyID())
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new ECDSASigner(signingKey));
    return jwt.serialize();
  }
}