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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.FederationRegistrationApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.OrganizationApi;
import se.swedenconnect.oidf.registry.api.RegistrationAdminApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.ConfigValueDto;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.Domain;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationJoinRequest;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
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

/**
 * Integration tests that submitting, resubmitting and deleting a registration request each leave an audit record
 * naming the account that made the request. The steps follow the AUD-05 case of the registry's end-to-end suite.
 *
 * @author Per Fredrik Plars
 */
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
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationRequestAuditIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID LOAD_ENTITY_CONFIGURATION_STEP_ID =
      UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG9;
  private static final int WIREMOCK_HTTPS_PORT = 6894;
  private static final int WIREMOCK_HTTP_PORT = 6793;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private AuditEventRepository auditEventRepository;

  private WireMockServer wireMockServer;
  private UUID assignId;

  @BeforeAll
  void setUp() throws Exception {
    this.startWireMock();
    this.setUpReviewFlow();
    this.setUpApplicant();
  }

  @AfterAll
  void tearDown() {
    this.wireMockServer.stop();
  }

  @Test
  @DisplayName("Submitting a registration request, and deleting it, is audited with the account that made it")
  void submittingAndDeletingIsAudited() {
    final Registration submitted = this.submit("submitted");

    final AuditEvent submission = this.event("REGISTRATION_SUBMITTED", submitted.getRegistrationId());
    assertThat(submission.getPrincipal()).isNotBlank()
        .isNotIn("<Registry>", "<NoActiveUserSet>", "<NoPrincipal>");
    assertThat((String) submission.getData().get("newData")).contains(submitted.getEntityIdentifier());
    assertThat(submission.getData()).containsKeys("organizationId", "instanceId");

    new FederationRegistrationApi(this.apiClient(APPLICANT))
        .deleteJoin(TENANT, APPLICANT.orgId, submitted.getRegistrationId());

    final AuditEvent deletion = this.event("REGISTRATION_DELETED", submitted.getRegistrationId());
    assertThat((String) deletion.getData().get("oldData"))
        .as("The deleted request, naming the account that submitted it")
        .contains(submitted.getEntityIdentifier(), submission.getPrincipal());
  }

  @Test
  @DisplayName("Resubmitting a registration request is audited")
  void resubmittingIsAudited() {
    final Registration submitted = this.submit("resubmitted");

    new FederationRegistrationApi(this.apiClient(APPLICANT)).updateRegistration(TENANT, APPLICANT.orgId,
        submitted.getRegistrationId(),
        new RegistrationJoinRequest().entityIdentifier(submitted.getEntityIdentifier()));

    final AuditEvent update = this.event("REGISTRATION_UPDATED", submitted.getRegistrationId());
    assertThat((String) update.getData().get("newData")).contains(submitted.getEntityIdentifier());
  }

  private Registration submit(final String path) {
    return new FederationRegistrationApi(this.apiClient(APPLICANT)).createJoinWithId(TENANT, APPLICANT.orgId,
        this.assignId, new RegistrationJoinRequest().entityIdentifier(entityId(path)));
  }

  private AuditEvent event(final String type, final UUID extId) {
    final List<AuditEvent> found = this.auditEventRepository.find(null, null, type).stream()
        .filter(event -> extId.toString().equals(event.getData().get("extId")))
        .toList();
    assertThat(found).as("%s for %s", type, extId).hasSize(1);
    return found.getFirst();
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private void setUpApplicant() {
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(APPLICANT));
    organizationApi.createOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("TestOrg9 AB"));
    final Domain domain = organizationApi.requestDomain(TENANT, APPLICANT.orgId,
        new DomainRequest().domain("localhost"));
    new RegistrationAdminApi(this.apiClient(OPERATOR)).approveDomain(TENANT, OPERATOR.orgId, domain.getDomainId());
  }

  private void setUpReviewFlow() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());

    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Registration request audit " + UUID.randomUUID())
        .description("Loads the entity configuration and pauses for review")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(LOAD_ENTITY_CONFIGURATION_STEP_ID)
            .name("LoadEntityConfigurationStep")
            .description("Loads the entity configuration, then waits for review")
            .config(List.of(new ConfigValueDto().key("manualreview").value("true"))))));
    this.assignId = flowApi.assignFlow(TENANT, OPERATOR.orgId, trustAnchor.getTrustAnchorId(),
        new AssignFlowRequest().flowId(flow.getFlowId())).getAssignId();
  }

  private void startWireMock() throws Exception {
    this.wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("registration-request-audit", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());
    this.stubEntityConfiguration("submitted");
    this.stubEntityConfiguration("resubmitted");
  }

  private void stubEntityConfiguration(final String path) throws Exception {
    final ECKey signingKey = (ECKey) TestDataOperations.genKey();
    final String entityId = entityId(path);
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
    stubFor(get(urlPathEqualTo("/" + path + "/.well-known/openid-federation"))
        .willReturn(ok(jwt.serialize()).withHeader("Content-Type", "application/entity-statement+jwt")));
  }

  private static String entityId(final String path) {
    return "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path;
  }
}
