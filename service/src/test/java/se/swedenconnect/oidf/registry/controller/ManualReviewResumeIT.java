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
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;

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
 * A registration paused for manual review keeps what the steps before the pause loaded, shows the operator a
 * preview, and records who approved it. Runs against MariaDB so the V31 migration is exercised.
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
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class ManualReviewResumeIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID LOAD_ENTITY_CONFIGURATION_STEP_ID =
      UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG4;
  private static final int WIREMOCK_HTTPS_PORT = 6891;
  private static final int WIREMOCK_HTTP_PORT = 6790;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private RegistrationRepository registrationRepository;
  @Autowired
  private SubordinateRepository subordinateRepository;

  private WireMockServer wireMockServer;
  private ECKey entityKey;
  private UUID trustAnchorId;
  private UUID assignId;

  @BeforeEach
  void setUp() throws Exception {
    this.entityKey = (ECKey) TestDataOperations.genKey();
    this.startWireMock();
    this.setUpReviewFlow();
  }

  @AfterEach
  void tearDown() {
    this.wireMockServer.stop();
  }

  @Test
  @DisplayName("Approving a paused step publishes with the keys loaded before the pause, and records the reviewer")
  void approvalResumesWithTheLoadedContext() throws Exception {
    final ApiClient applicantClient = this.apiClient(APPLICANT);
    final OrganizationApi organizationApi = new OrganizationApi(applicantClient);
    organizationApi.createOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("TestOrg4 AB"));
    final Domain domain = organizationApi.requestDomain(TENANT, APPLICANT.orgId,
        new DomainRequest().domain("localhost"));
    // The domain is decided before the entity: approving the entity's step waits for it
    new RegistrationAdminApi(this.apiClient(OPERATOR)).approveDomain(TENANT, OPERATOR.orgId, domain.getDomainId());

    final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/resume";
    final Registration submitted = new FederationRegistrationApi(applicantClient).createJoinWithId(
        TENANT, APPLICANT.orgId, this.assignId, new RegistrationJoinRequest().entityIdentifier(entityId));
    assertThat(submitted.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.PENDING_APPROVAL);

    final var paused = this.registrationRepository.findById(submitted.getRegistrationId()).orElseThrow();
    final int pendingStep = paused.getPendingStepIndex();
    assertThat(paused.getProcessContext())
        .as("What the step loaded before the pause is saved with the registration")
        .containsKeys(ContextKey.ENTITY_CONFIGURATION_METADATA, ContextKey.ENTITY_CONFIGURATION_JWKS);
    final StepExecutionRecordDto pausedRecord = paused.getStepResults().get(pendingStep);
    assertThat(pausedRecord.status()).isEqualTo("PENDING_APPROVAL");
    assertThat(pausedRecord.preview()).isNotNull();
    assertThat(pausedRecord.preview().sections())
        .extracting(PreviewSection::type)
        .contains(PreviewSection.Type.CHECKS, PreviewSection.Type.FIELDS);

    // The entity changes its keys after review started; the approved registration must publish what was reviewed.
    this.stubEntityConfiguration("resume", (ECKey) TestDataOperations.genKey());

    new RegistrationAdminApi(this.apiClient(OPERATOR))
        .approveStep(TENANT, OPERATOR.orgId, submitted.getRegistrationId(), pendingStep);

    final var approved = this.registrationRepository.findById(submitted.getRegistrationId()).orElseThrow();
    assertThat(approved.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
    assertThat(approved.getProcessContext()).as("Nothing is kept once the registration is no longer paused")
        .isNullOrEmpty();
    assertThat(approved.getReviewedBy()).isNotBlank();
    assertThat(approved.getReviewedAt()).isNotNull();
    assertThat(approved.getStepResults().get(pendingStep)).satisfies(record -> {
      assertThat(record.status()).isEqualTo("SUCCESS");
      assertThat(record.reviewedBy()).isEqualTo(approved.getReviewedBy());
      assertThat(record.reviewedAt()).isNotNull();
    });

    final var subordinate = this.subordinateRepository
        .findByEntityidentifierAndTaIm_TaImId(entityId, this.trustAnchorId).orElseThrow();
    assertThat(subordinate.getJwks().toString()).contains(this.entityKey.getKeyID());
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private void setUpReviewFlow() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT,
        OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    this.trustAnchorId = trustAnchor.getTrustAnchorId();

    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Manual review resume " + UUID.randomUUID())
        .description("Loads the entity configuration and pauses for review")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(LOAD_ENTITY_CONFIGURATION_STEP_ID)
            .name("LoadEntityConfigurationStep")
            .description("Loads the entity configuration, then waits for review")
            .config(List.of(new ConfigValueDto().key("manualreview").value("true"))))));
    this.assignId = flowApi.assignFlow(TENANT, OPERATOR.orgId, this.trustAnchorId,
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
        .notifier(new ConsoleNotifier("manual-review-resume", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());
    this.stubEntityConfiguration("resume", this.entityKey);
  }

  private void stubEntityConfiguration(final String path, final ECKey signingKey) throws Exception {
    final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path;
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
}
