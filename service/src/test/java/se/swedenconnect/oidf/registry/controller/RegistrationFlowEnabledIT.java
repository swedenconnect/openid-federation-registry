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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClientResponseException;
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
import se.swedenconnect.oidf.registry.api.model.FlowSummaryDto;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationJoinRequest;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A flow can be taken out of service without being deleted. Disabling it hides it from applicants browsing joinable
 * flows and refuses new registrations with a 409; re-enabling it restores both. Domain enforcement is switched off so
 * the test can focus on the flag alone.
 *
 * @author Felix Hellman
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "openid.federation.registry.registration.require-registered-domain=false",
        "openid.federation.registry.entity-configuration-loader.enabled=true",
        "openid.federation.registry.entity-configuration-loader.enable-local-ip-address-ranges=true",
        "openid.federation.registry.entity-configuration-loader.trust-bundle-alias=wiremock-trust",
        "spring.ssl.bundle.jks.wiremock-trust.truststore.location=classpath:wiremock-keystore.p12",
        "spring.ssl.bundle.jks.wiremock-trust.truststore.password=Test1234",
        "spring.ssl.bundle.jks.wiremock-trust.truststore.type=PKCS12"
    })
@Testcontainers
@AutoConfigureRestTestClient
class RegistrationFlowEnabledIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID =
      UUID.fromString("AE67B1D8-2DCF-4A8C-9E6B-FC972CC65DEA");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG1;
  private static final int WIREMOCK_HTTPS_PORT = 6890;
  private static final int WIREMOCK_HTTP_PORT = 6789;
  private static final String ENTITY_PATH = "toggled";

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private WireMockServer wireMockServer;
  private RegistrationFlowApi flowApi;
  private UUID flowId;
  private UUID assignId;

  @BeforeEach
  void setUp() throws Exception {
    this.startWireMock();
    this.setUpOperatorInfrastructure();
  }

  @AfterEach
  void tearDown() {
    this.wireMockServer.stop();
  }

  @Test
  @DisplayName("A flow is created enabled, disappears and refuses registrations while disabled, and comes back")
  void disablingHidesTheFlowAndRefusesRegistrations() {
    final FederationRegistrationApi registrationApi = new FederationRegistrationApi(this.apiClient(APPLICANT));
    final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + ENTITY_PATH;

    assertThat(this.flowApi.getFlow(TENANT, OPERATOR.orgId, this.flowId).getEnabled())
        .as("A flow created without the flag is enabled")
        .isTrue();
    assertThat(this.summaryOfFixtureFlow().getEnabled()).isTrue();
    assertThat(registrationApi.listFlows()).anyMatch(f -> this.assignId.equals(f.getJoinId()));

    this.setFlowEnabled(false);

    assertThat(this.flowApi.getFlow(TENANT, OPERATOR.orgId, this.flowId).getEnabled()).isFalse();
    assertThat(this.summaryOfFixtureFlow().getEnabled())
        .as("The operator's own list still shows the flow, marked disabled")
        .isFalse();
    assertThat(registrationApi.listFlows())
        .as("An applicant is not offered a disabled flow")
        .noneMatch(f -> this.assignId.equals(f.getJoinId()));

    assertThatThrownBy(() -> registrationApi.createJoinWithId(TENANT, APPLICANT.orgId, this.assignId,
        new RegistrationJoinRequest().entityIdentifier(entityId)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> {
          final RestClientResponseException response = (RestClientResponseException) ex;
          assertThat(response.getStatusCode().value()).isEqualTo(409);
          assertThat(response.getResponseBodyAsString()).contains("Registration flow is disabled");
        });

    assertThat(registrationApi.listRegistrations(TENANT, APPLICANT.orgId))
        .as("The refusal happens before anything is created")
        .isEmpty();

    this.setFlowEnabled(true);

    assertThat(registrationApi.listFlows())
        .as("Re-enabling puts the flow back in front of applicants")
        .anyMatch(f -> this.assignId.equals(f.getJoinId()));

    final Registration registration = registrationApi.createJoinWithId(TENANT, APPLICANT.orgId, this.assignId,
        new RegistrationJoinRequest().entityIdentifier(entityId));
    assertThat(registration.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.APPROVED);
  }

  private FlowSummaryDto summaryOfFixtureFlow() {
    return this.flowApi.listFlows1(TENANT, OPERATOR.orgId).stream()
        .filter(f -> this.flowId.equals(f.getFlowId()))
        .findFirst()
        .orElseThrow();
  }

  private void setFlowEnabled(final boolean enabled) {
    this.flowApi.updateFlow(TENANT, OPERATOR.orgId, this.flowId, this.fixtureFlowDto().enabled(enabled));
  }

  private RegistrationFlowDto fixtureFlowDto() {
    return new RegistrationFlowDto()
        .name("Enabled-flag fixture flow")
        .description("PredefinedDirectRegisterFlow for RegistrationFlowEnabledIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID)
            .name("PredefinedDirectRegisterFlow")
            .description("Loads entity configuration and publishes subordinate statement")));
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private void setUpOperatorInfrastructure() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());

    this.flowApi = new RegistrationFlowApi(operatorClient);
    final RegistrationFlowDto flow = this.flowApi.createFlow(TENANT, OPERATOR.orgId, this.fixtureFlowDto());
    this.flowId = flow.getFlowId();
    assertThat(flow.getEnabled())
        .as("The create response already reports the flow as enabled")
        .isTrue();

    final AssignFlowResponse assignment = this.flowApi.assignFlow(TENANT, OPERATOR.orgId,
        trustAnchor.getTrustAnchorId(), new AssignFlowRequest().flowId(this.flowId));
    this.assignId = assignment.getAssignId();
  }

  private void startWireMock() throws Exception {
    this.wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("registration-flow-enabled", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());

    final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + ENTITY_PATH;
    stubFor(get(urlPathEqualTo("/" + ENTITY_PATH + "/.well-known/openid-federation"))
        .willReturn(ok(buildEntityStatementJwt(entityId))
            .withHeader("Content-Type", "application/entity-statement+jwt")));
  }

  private static String buildEntityStatementJwt(final String entityId) throws Exception {
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
