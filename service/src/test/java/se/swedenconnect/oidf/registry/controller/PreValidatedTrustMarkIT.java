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
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.FederationRegistrationApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.OrganizationApi;
import se.swedenconnect.oidf.registry.api.RegistrationAdminApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.AssignFlowResponse;
import se.swedenconnect.oidf.registry.api.model.ConfigValueDto;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.PreValidatedTrustMarksRequest;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationJoinRequest;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.api.model.TrustmarkRequest;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

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
 * A trust mark type the tenant operator has pre-approved for an organization enrolls without stopping for manual
 * review: the sub-flow runs in full, the gated step just does not pause. An organization without that
 * pre-approval still waits for an operator on the very same flow.
 *
 * @author Felix Hellman
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // Both applicants register an entity under the single host WireMock can serve, "localhost", and only
        // one organization on a tenant may hold a given domain at a time. Domain enforcement is therefore
        // switched off here; it has its own coverage in DomainEnforcementAndCascadeIT and DomainUniquenessIT.
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
class PreValidatedTrustMarkIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType PRE_VALIDATED = JwtTestUtils.OrganisationType.TESTORG1;
  private static final JwtTestUtils.OrganisationType NOT_PRE_VALIDATED = JwtTestUtils.OrganisationType.TESTORG2;
  private static final UUID PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID =
      UUID.fromString("AE67B1D8-2DCF-4A8C-9E6B-FC972CC65DEA");
  private static final UUID TRUST_MARK_ISSUER_REGISTRATION_STEP_ID =
      UUID.fromString("F1A2B3C4-D5E6-4F7A-8B9C-0D1E2F3A4B5C");
  private static final UUID ADD_TRUST_MARK_SUBJECT_STEP_ID =
      UUID.fromString("3F8A1C2D-7E4B-4F9A-B5D6-8C0E2A3F1B4D");
  private static final int WIREMOCK_HTTPS_PORT = 6890;
  private static final int WIREMOCK_HTTP_PORT = 6789;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private RegistrationRepository registrationRepository;

  private WireMockServer wireMockServer;
  private RegistrationAdminApi operatorAdminApi;
  private UUID assignId;
  private String trustMarkType;

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
  @DisplayName("A pre-validated trust mark enrolls without manual review; an unlisted one still waits for it")
  void preValidatedTrustMarkSkipsManualReview() {
    // The operator can only pre-approve a trust mark for an organization that has bootstrapped its record.
    this.bootstrap(PRE_VALIDATED);
    this.bootstrap(NOT_PRE_VALIDATED);
    this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId, PRE_VALIDATED.orgId,
        new PreValidatedTrustMarksRequest().preValidatedTrustMarks(List.of(this.trustMarkType)));

    final Registration preValidatedRegistration = this.register(PRE_VALIDATED, "prevalidated");
    assertThat(preValidatedRegistration.getStatusFedreg())
        .isEqualTo(Registration.StatusFedregEnum.APPROVED);

    final var autoApprovedChild = this.trustMarkChildOf(preValidatedRegistration);
    assertThat(autoApprovedChild.getStatus())
        .as("The trust mark sub-flow ran to completion without anyone approving a step")
        .isEqualTo(RegistrationStatus.APPROVED);
    assertThat(autoApprovedChild.getPendingStepIndex()).isNull();
    assertThat(autoApprovedChild.getStepResults())
        .as("The auto-approval is visible in the step trail")
        .anySatisfy(step -> assertThat(step.message()).contains("Auto-approved"));

    final Registration plainRegistration = this.register(NOT_PRE_VALIDATED, "notprevalidated");
    final var pendingChild = this.trustMarkChildOf(plainRegistration);
    assertThat(pendingChild.getStatus())
        .as("An organization without the pre-approval still waits for an operator on the same flow")
        .isEqualTo(RegistrationStatus.PENDING_APPROVAL);
    assertThat(pendingChild.getPendingStepIndex()).isNotNull();
  }

  private se.swedenconnect.oidf.registry.registrations.model.Registration trustMarkChildOf(
      final Registration parent) {
    final List<se.swedenconnect.oidf.registry.registrations.model.Registration> children =
        this.registrationRepository.findByParentRegistration_RegistrationId(parent.getRegistrationId());
    assertThat(children)
        .as("The registration requested exactly one trust mark")
        .singleElement()
        .satisfies(child -> {
          assertThat(child.getRegistrationType()).isEqualTo(RegistrationType.TRUST_MARK_SUBORDINATE);
          assertThat(child.getEntityId()).isEqualTo(this.trustMarkType);
        });
    return children.getFirst();
  }

  private void bootstrap(final JwtTestUtils.OrganisationType applicant) {
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(applicant));
    organizationApi.createOrganization(TENANT, applicant.orgId,
        new CreateOrganizationRequest().legalName(applicant.name + " AB"));
  }

  private Registration register(final JwtTestUtils.OrganisationType applicant, final String path) {
    final ApiClient applicantClient = this.apiClient(applicant);
    return new FederationRegistrationApi(applicantClient).createJoinWithId(TENANT, applicant.orgId,
        this.assignId, new RegistrationJoinRequest()
            .entityIdentifier(this.entityId(path))
            .trustmarksRequested(List.of(new TrustmarkRequest()
                .trustmarkIssuer("https://www.pm.se/oidf/tmi/prevalidated")
                .trustmarkType(List.of(this.trustMarkType)))));
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private String entityId(final String path) {
    return "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path;
  }

  private void setUpOperatorInfrastructure() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final EntitiesApi entitiesApi = new EntitiesApi(operatorClient);
    final ModulesApi modulesApi = new ModulesApi(operatorClient);
    final TrustmarksApi trustmarksApi = new TrustmarksApi(operatorClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);

    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());

    final FederationEntity tmiEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/tmi/prevalidated").build());
    final TrustmarkIssuer trustmarkIssuer = modulesApi.createTrustmarkIssuer(TENANT, OPERATOR.orgId,
        new TrustmarkIssuer()
            .entityId(tmiEntity.getEntityId())
            .active(true)
            .trustMarkTokenValidityDuration("PT1H"));

    this.trustMarkType = "https://www.pm.se/oidf/tm/prevalidated";
    final Trustmark trustmark = trustmarksApi.createTrustmark(TENANT, OPERATOR.orgId, new Trustmark()
        .trustmarkissuerId(trustmarkIssuer.getTrustmarkIssuerId())
        .trustmarkType(this.trustMarkType));

    // The trust mark sub-flow stops for manual review unless the applicant is pre-validated for the type.
    final RegistrationFlowDto trustMarkFlow = flowApi.createFlow(TENANT, OPERATOR.orgId,
        new RegistrationFlowDto()
            .name("Trust mark enrollment with manual review " + UUID.randomUUID())
            .description("AddTrustMarkSubjectStep gated on manual review")
            .flowType(RegistrationFlowDto.FlowTypeEnum.TRUST_MARK_ISSUER)
            .steps(List.of(new StepDto()
                .stepId(ADD_TRUST_MARK_SUBJECT_STEP_ID)
                .name("AddTrustMarkSubjectStep")
                .description("Validates the requested trust mark types")
                .config(List.of(new ConfigValueDto().key("manualreview").value("true"))))));
    flowApi.assignFlowToTrustMark(TENANT, OPERATOR.orgId, trustmark.getTrustmarkId(),
        new AssignFlowRequest().flowId(trustMarkFlow.getFlowId()));

    final RegistrationFlowDto registrationFlow = flowApi.createFlow(TENANT, OPERATOR.orgId,
        new RegistrationFlowDto()
            .name("Registration with trust mark enrollment " + UUID.randomUUID())
            .description("PredefinedDirectRegisterFlow followed by trust mark enrollment")
            .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
            .steps(List.of(
                new StepDto()
                    .stepId(PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID)
                    .name("PredefinedDirectRegisterFlow")
                    .description("Loads entity configuration and publishes subordinate statement"),
                new StepDto()
                    .stepId(TRUST_MARK_ISSUER_REGISTRATION_STEP_ID)
                    .name("TrustMarkIssuerRegistrationStep")
                    .description("Dispatches the trust mark sub-flow for each requested type"))));
    final AssignFlowResponse assignment = flowApi.assignFlow(TENANT, OPERATOR.orgId,
        trustAnchor.getTrustAnchorId(), new AssignFlowRequest().flowId(registrationFlow.getFlowId()));
    this.assignId = assignment.getAssignId();
    this.operatorAdminApi = new RegistrationAdminApi(operatorClient);
  }

  private void startWireMock() throws Exception {
    this.wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("pre-validated-trust-mark", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());

    for (final String path : List.of("prevalidated", "notprevalidated")) {
      stubFor(get(urlPathEqualTo("/" + path + "/.well-known/openid-federation"))
          .willReturn(ok(buildEntityStatementJwt(this.entityId(path)))
              .withHeader("Content-Type", "application/entity-statement+jwt")));
    }
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
