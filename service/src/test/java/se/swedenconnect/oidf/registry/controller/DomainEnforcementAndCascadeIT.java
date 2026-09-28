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
import se.swedenconnect.oidf.registry.api.OrganizationApi;
import se.swedenconnect.oidf.registry.api.RegistrationAdminApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.AssignFlowResponse;
import se.swedenconnect.oidf.registry.api.model.ConfigValueDto;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.Domain;
import se.swedenconnect.oidf.registry.api.model.DomainRejectionResult;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationJoinRequest;
import se.swedenconnect.oidf.registry.api.model.RejectRegistrationRequest;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.api.model.TrustmarkRequest;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the two halves of domain enforcement: a registration request is only accepted for a host the
 * registering organization has claimed, and rejecting a claimed domain carries the registrations that depended
 * on it — and only those — down with it.
 *
 * <p>Both halves run against the same WireMock-served entity statements as {@code RegistrationFlowEndToEndIT}.
 * The cases that turn on telling several distinct hosts apart insert registrations straight through the
 * repository instead — WireMock can only serve {@code localhost}, and that is exactly the one thing those cases
 * cannot make do with.
 *
 * @author Felix Hellman
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
@Testcontainers
@AutoConfigureRestTestClient
class DomainEnforcementAndCascadeIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID SWEDENCONNECT_INSTANCE_ID =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final UUID PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID =
      UUID.fromString("AE67B1D8-2DCF-4A8C-9E6B-FC972CC65DEA");
  private static final UUID TRUST_MARK_ISSUER_REGISTRATION_STEP_ID =
      UUID.fromString("F1A2B3C4-D5E6-4F7A-8B9C-0D1E2F3A4B5C");
  private static final UUID LOAD_ENTITY_CONFIGURATION_STEP_ID =
      UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  private static final UUID ADD_TRUST_MARK_SUBJECT_STEP_ID =
      UUID.fromString("3F8A1C2D-7E4B-4F9A-B5D6-8C0E2A3F1B4D");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
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
  private OrganizationRepository organizationRepository;
  @Autowired
  private FlowAssignmentRepository flowAssignmentRepository;
  @Autowired
  private RegistrationRepository registrationRepository;

  private WireMockServer wireMockServer;
  private RegistrationAdminApi operatorAdminApi;
  private UUID assignId;
  private UUID trustMarkReviewAssignId;
  // Unique per test method: @BeforeEach provisions fresh entities, and a federation entity identifier is
  // globally unique.
  private String trustMarkIssuerEntityId;
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
  @DisplayName("A registration for a host the organization has not claimed is refused")
  void registrationForAnUnclaimedHostIsRefused() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG1;
    final ApiClient applicantClient = this.apiClient(applicant);
    final OrganizationApi organizationApi = new OrganizationApi(applicantClient);
    organizationApi.createOrganization(TENANT, applicant.orgId,
        new CreateOrganizationRequest().legalName("TestOrg1 AB"));

    final FederationRegistrationApi registrationApi = new FederationRegistrationApi(applicantClient);
    final String entityId = this.stubbedEntityId("unclaimed");

    assertThatThrownBy(() -> registrationApi.createJoinWithId(TENANT, applicant.orgId, this.assignId,
        new RegistrationJoinRequest().entityIdentifier(entityId)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> {
          final RestClientResponseException response = (RestClientResponseException) ex;
          assertThat(response.getStatusCode().value()).isEqualTo(400);
          assertThat(response.getResponseBodyAsString())
              .contains("Entity identifier host 'localhost' is not a registered domain of organization "
                  + applicant.orgId);
        });

    final Domain claimed = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("localhost"));
    assertThat(claimed.getStatus())
        .as("A claim awaiting review is already enough to register under")
        .isEqualTo(Domain.StatusEnum.PENDING);

    final Registration registration = registrationApi.createJoinWithId(TENANT, applicant.orgId, this.assignId,
        new RegistrationJoinRequest().entityIdentifier(entityId));
    assertThat(registration.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.APPROVED);

    // Only one organization on the tenant holds a domain at a time, and WireMock can serve no host but
    // 'localhost'. Withdrawing the claim hands the host back, so the other localhost case in this class is
    // free to claim it whichever order the two run in.
    organizationApi.deleteDomain(TENANT, applicant.orgId, claimed.getDomainId());
  }

  @Test
  @DisplayName("Rejecting a domain rejects the registrations that depended on it, and nothing else")
  void rejectingADomainCascadesToDependentRegistrationsOnly() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG2;
    final ApiClient applicantClient = this.apiClient(applicant);
    final OrganizationApi organizationApi = new OrganizationApi(applicantClient);
    organizationApi.createOrganization(TENANT, applicant.orgId,
        new CreateOrganizationRequest().legalName("TestOrg2 AB"));

    final Domain doomed = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("doomed.example.com"));
    final Domain kept = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("kept.example.com"));
    this.operatorAdminApi.approveDomain(TENANT, OPERATOR.orgId, kept.getDomainId());

    final Organization applicantOrganization = this.organizationRepository
        .findByInstance_InstanceIdAndOrgNumber(SWEDENCONNECT_INSTANCE_ID, applicant.orgId)
        .orElseThrow();
    final FlowAssignment flowAssignment = this.flowAssignmentRepository.findById(this.assignId).orElseThrow();

    final se.swedenconnect.oidf.registry.registrations.model.Registration underDoomedDomain =
        this.pendingRegistration(applicantOrganization, flowAssignment, "https://sp.doomed.example.com/oidf",
            RegistrationType.SUBORDINATE, null);
    final se.swedenconnect.oidf.registry.registrations.model.Registration trustMarkChild =
        this.pendingRegistration(applicantOrganization, flowAssignment, "https://tm.example.com/tm/x",
            RegistrationType.TRUST_MARK_SUBORDINATE, underDoomedDomain);
    final se.swedenconnect.oidf.registry.registrations.model.Registration underKeptDomain =
        this.pendingRegistration(applicantOrganization, flowAssignment, "https://sp.kept.example.com/oidf",
            RegistrationType.SUBORDINATE, null);

    final DomainRejectionResult result = this.operatorAdminApi.rejectDomain(TENANT, OPERATOR.orgId,
        doomed.getDomainId(), new RejectRegistrationRequest().rejectionReason("Domain not controlled by you"));

    assertThat(result.getStatus()).isEqualTo(DomainRejectionResult.StatusEnum.REJECTED);
    assertThat(result.getCascadedRegistrationIds())
        .containsExactlyInAnyOrder(underDoomedDomain.getRegistrationId(), trustMarkChild.getRegistrationId());

    final var rejected = this.registrationRepository.findById(underDoomedDomain.getRegistrationId()).orElseThrow();
    assertThat(rejected.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(rejected.getRejectionReason()).isEqualTo("Not Accepted Domain");
    assertThat(rejected.getReviewedAt()).isNotNull();
    assertThat(rejected.getReviewedBy()).isNotNull();

    final var rejectedChild = this.registrationRepository.findById(trustMarkChild.getRegistrationId()).orElseThrow();
    assertThat(rejectedChild.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(rejectedChild.getRejectionReason()).isEqualTo("Not Accepted Domain");

    final var untouched = this.registrationRepository.findById(underKeptDomain.getRegistrationId()).orElseThrow();
    assertThat(untouched.getStatus())
        .as("A registration still covered by another registered domain is left alone")
        .isEqualTo(RegistrationStatus.PENDING_APPROVAL);
    assertThat(untouched.getRejectionReason()).isNull();
  }

  @Test
  @DisplayName("A registration under a host covered by a second, still-registered domain survives the rejection")
  void registrationCoveredByASecondDomainSurvives() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG3;
    final ApiClient applicantClient = this.apiClient(applicant);
    final OrganizationApi organizationApi = new OrganizationApi(applicantClient);
    organizationApi.createOrganization(TENANT, applicant.orgId,
        new CreateOrganizationRequest().legalName("TestOrg3 AB"));

    // Two domains covering the very same host: sp.overlap.example.com is a subdomain of both.
    final Domain broad = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("overlap.example.com"));
    final Domain narrow = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("sp.overlap.example.com"));
    this.operatorAdminApi.approveDomain(TENANT, OPERATOR.orgId, narrow.getDomainId());

    final Organization applicantOrganization = this.organizationRepository
        .findByInstance_InstanceIdAndOrgNumber(SWEDENCONNECT_INSTANCE_ID, applicant.orgId)
        .orElseThrow();
    final FlowAssignment flowAssignment = this.flowAssignmentRepository.findById(this.assignId).orElseThrow();
    final se.swedenconnect.oidf.registry.registrations.model.Registration registration =
        this.pendingRegistration(applicantOrganization, flowAssignment, "https://sp.overlap.example.com/oidf",
            RegistrationType.SUBORDINATE, null);

    final DomainRejectionResult result = this.operatorAdminApi.rejectDomain(TENANT, OPERATOR.orgId,
        broad.getDomainId(), new RejectRegistrationRequest().rejectionReason("Too broad"));

    assertThat(result.getCascadedRegistrationIds()).isEmpty();
    assertThat(this.registrationRepository.findById(registration.getRegistrationId()).orElseThrow().getStatus())
        .isEqualTo(RegistrationStatus.PENDING_APPROVAL);
  }

  @Test
  @DisplayName("A domain rejection carries a pending registration's trust mark enrollment down with it")
  void rejectingADomainCascadesToTrustMarkChildren() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG4;
    final ApiClient applicantClient = this.apiClient(applicant);
    final OrganizationApi organizationApi = new OrganizationApi(applicantClient);
    organizationApi.createOrganization(TENANT, applicant.orgId,
        new CreateOrganizationRequest().legalName("TestOrg4 AB"));
    final Domain claimed = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("localhost"));

    // Both the registration and its trust mark enrollment stop for manual review, so both are still in flight
    // when the domain they were accepted under is rejected.
    final Registration registration = new FederationRegistrationApi(applicantClient).createJoinWithId(
        TENANT, applicant.orgId, this.trustMarkReviewAssignId, new RegistrationJoinRequest()
            .entityIdentifier(this.stubbedEntityId("cascadetm"))
            .trustmarksRequested(List.of(new TrustmarkRequest()
                .trustmarkIssuer(this.trustMarkIssuerEntityId)
                .trustmarkType(List.of(this.trustMarkType)))));
    assertThat(registration.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.PENDING_APPROVAL);

    final var parent = this.registrationRepository.findById(registration.getRegistrationId()).orElseThrow();
    final var child = this.registrationRepository
        .findByParentRegistration_RegistrationId(parent.getRegistrationId());
    assertThat(child).singleElement().satisfies(tmReg -> {
      assertThat(tmReg.getRegistrationType()).isEqualTo(RegistrationType.TRUST_MARK_SUBORDINATE);
      assertThat(tmReg.getStatus()).isEqualTo(RegistrationStatus.PENDING_APPROVAL);
    });

    final DomainRejectionResult result = this.operatorAdminApi.rejectDomain(TENANT, OPERATOR.orgId,
        claimed.getDomainId(), new RejectRegistrationRequest().rejectionReason("Domain not controlled by you"));

    assertThat(result.getCascadedRegistrationIds())
        .containsExactlyInAnyOrder(parent.getRegistrationId(), child.getFirst().getRegistrationId());

    final var rejectedParent = this.registrationRepository.findById(parent.getRegistrationId()).orElseThrow();
    assertThat(rejectedParent.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(rejectedParent.getRejectionReason()).isEqualTo("Not Accepted Domain");

    final var rejectedChild = this.registrationRepository
        .findById(child.getFirst().getRegistrationId()).orElseThrow();
    assertThat(rejectedChild.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(rejectedChild.getRejectionReason()).isEqualTo("Not Accepted Domain");
  }

  private se.swedenconnect.oidf.registry.registrations.model.Registration pendingRegistration(
      final Organization organization, final FlowAssignment flowAssignment, final String entityId,
      final RegistrationType type,
      final se.swedenconnect.oidf.registry.registrations.model.Registration parent) {
    return this.registrationRepository.save(
        se.swedenconnect.oidf.registry.registrations.model.Registration.builder()
            .registrationId(UUID.randomUUID())
            .flowAssignment(flowAssignment)
            .organization(organization)
            .entityId(entityId)
            .registrationType(type)
            .parentRegistration(parent)
            .status(RegistrationStatus.PENDING_APPROVAL)
            .build());
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private String stubbedEntityId(final String path) {
    return "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path;
  }

  private void setUpOperatorInfrastructure() throws Exception {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final EntitiesApi entitiesApi = new EntitiesApi(operatorClient);
    final ModulesApi modulesApi = new ModulesApi(operatorClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);

    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Domain enforcement fixture flow " + UUID.randomUUID())
        .description("PredefinedDirectRegisterFlow for DomainEnforcementAndCascadeIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID)
            .name("PredefinedDirectRegisterFlow")
            .description("Loads entity configuration and publishes subordinate statement"))));
    final AssignFlowResponse assignment = flowApi.assignFlow(TENANT, OPERATOR.orgId,
        trustAnchor.getTrustAnchorId(), new AssignFlowRequest().flowId(flow.getFlowId()));
    this.assignId = assignment.getAssignId();
    this.operatorAdminApi = new RegistrationAdminApi(operatorClient);

    this.setUpTrustMarkReviewFlow(operatorClient, trustAnchor);
  }

  /**
   * A second flow on the same intermediate whose registration stops for manual review only after the trust mark
   * sub-flow has been dispatched — so a rejection can find both a pending registration and a pending trust mark
   * enrollment beneath it.
   */
  private void setUpTrustMarkReviewFlow(final ApiClient operatorClient, final TrustAnchor trustAnchor) {
    final EntitiesApi entitiesApi = new EntitiesApi(operatorClient);
    final ModulesApi modulesApi = new ModulesApi(operatorClient);
    final TrustmarksApi trustmarksApi = new TrustmarksApi(operatorClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);

    this.trustMarkIssuerEntityId = "https://www.pm.se/oidf/tmi/cascade/" + UUID.randomUUID();
    this.trustMarkType = "https://www.pm.se/oidf/tm/cascade/" + UUID.randomUUID();
    final FederationEntity tmiEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier(this.trustMarkIssuerEntityId).build());
    final TrustmarkIssuer trustmarkIssuer = modulesApi.createTrustmarkIssuer(TENANT, OPERATOR.orgId,
        new TrustmarkIssuer()
            .entityId(tmiEntity.getEntityId())
            .active(true)
            .trustMarkTokenValidityDuration("PT1H"));
    final Trustmark trustmark = trustmarksApi.createTrustmark(TENANT, OPERATOR.orgId, new Trustmark()
        .trustmarkissuerId(trustmarkIssuer.getTrustmarkIssuerId())
        .trustmarkType(this.trustMarkType));

    final RegistrationFlowDto trustMarkFlow = flowApi.createFlow(TENANT, OPERATOR.orgId,
        new RegistrationFlowDto()
            .name("Cascade trust mark enrollment " + UUID.randomUUID())
            .description("AddTrustMarkSubjectStep gated on manual review")
            .flowType(RegistrationFlowDto.FlowTypeEnum.TRUST_MARK_ISSUER)
            .steps(List.of(new StepDto()
                .stepId(ADD_TRUST_MARK_SUBJECT_STEP_ID)
                .name("AddTrustMarkSubjectStep")
                .description("Validates the requested trust mark types")
                .config(List.of(new ConfigValueDto().key("manualreview").value("true"))))));
    flowApi.assignFlowToTrustMark(TENANT, OPERATOR.orgId, trustmark.getTrustmarkId(),
        new AssignFlowRequest().flowId(trustMarkFlow.getFlowId()));

    final RegistrationFlowDto reviewFlow = flowApi.createFlow(TENANT, OPERATOR.orgId,
        new RegistrationFlowDto()
            .name("Cascade registration with manual review " + UUID.randomUUID())
            .description("Dispatches the trust mark sub-flow, then stops for manual review")
            .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
            .steps(List.of(
                new StepDto()
                    .stepId(TRUST_MARK_ISSUER_REGISTRATION_STEP_ID)
                    .name("TrustMarkIssuerRegistrationStep")
                    .description("Dispatches the trust mark sub-flow for each requested type"),
                new StepDto()
                    .stepId(LOAD_ENTITY_CONFIGURATION_STEP_ID)
                    .name("LoadEntityConfigurationStep")
                    .description("Loads the entity configuration, after manual review")
                    .config(List.of(new ConfigValueDto().key("manualreview").value("true"))))));
    this.trustMarkReviewAssignId = flowApi.assignFlow(TENANT, OPERATOR.orgId,
        trustAnchor.getTrustAnchorId(), new AssignFlowRequest().flowId(reviewFlow.getFlowId())).getAssignId();
  }

  private void startWireMock() throws Exception {
    this.wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("domain-enforcement", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());

    for (final String path : List.of("unclaimed", "cascadetm")) {
      stubFor(get(urlPathEqualTo("/" + path + "/.well-known/openid-federation"))
          .willReturn(ok(buildEntityStatementJwt(this.stubbedEntityId(path)))
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
