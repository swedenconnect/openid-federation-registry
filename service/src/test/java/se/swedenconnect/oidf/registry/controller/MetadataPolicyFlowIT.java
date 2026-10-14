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
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A flow that loads the entity configuration, pauses for the operator to review the metadata, and then builds the
 * Subordinate Statement's {@code metadata} and {@code metadata_policy} from the Sweden Connect template.
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
class MetadataPolicyFlowIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID LOAD_ENTITY_CONFIGURATION_STEP_ID =
      UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  private static final UUID METADATA_REVIEW_STEP_ID = UUID.fromString("5D2C8E14-7A3B-4C9F-9E61-2B8F4A7D0C35");
  private static final UUID METADATA_POLICY_CREATION_STEP_ID =
      UUID.fromString("FCF26DE1-93BF-4B80-A5E5-F4C88BDFFFEA");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG5;
  private static final int WIREMOCK_HTTPS_PORT = 6892;
  private static final int WIREMOCK_HTTP_PORT = 6791;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private RegistrationRepository registrationRepository;
  @Autowired
  private SubordinateRepository subordinateRepository;

  private static boolean applicantReady;

  private WireMockServer wireMockServer;
  private UUID trustAnchorId;
  private UUID assignId;
  private RegistrationFlowApi flowApi;

  @BeforeEach
  void setUp() throws Exception {
    this.wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("metadata-policy-flow", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());
    this.stubEntityConfiguration("good", "urn:glue:iso6523:0007:" + APPLICANT.orgId);
    this.stubEntityConfiguration("wrongid", "urn:glue:iso6523:0007:2120000002");
    this.setUpFlow();
    this.setUpApplicant();
  }

  @AfterEach
  void tearDown() {
    this.wireMockServer.stop();
  }

  @Test
  @DisplayName("The reviewed metadata is turned into the metadata and metadata_policy claims of the statement")
  void reviewedMetadataBecomesTheSubordinateStatementClaims() {
    final String entityId = this.entityId("good");
    final Registration submitted = new FederationRegistrationApi(this.apiClient(APPLICANT)).createJoinWithId(
        TENANT, APPLICANT.orgId, this.assignId, new RegistrationJoinRequest().entityIdentifier(entityId));
    assertThat(submitted.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.PENDING_APPROVAL);

    final var paused = this.registrationRepository.findById(submitted.getRegistrationId()).orElseThrow();
    final StepExecutionRecordDto review = paused.getStepResults().get(paused.getPendingStepIndex());
    assertThat(review.stepName()).isEqualTo("MetadataReviewStep");
    assertThat(review.preview().sections())
        .extracting(PreviewSection::label)
        .contains("Source", "Names", "Full metadata");

    new RegistrationAdminApi(this.apiClient(OPERATOR))
        .approveStep(TENANT, OPERATOR.orgId, submitted.getRegistrationId(), paused.getPendingStepIndex());

    assertThat(this.registrationRepository.findById(submitted.getRegistrationId()).orElseThrow().getStatus())
        .isEqualTo(RegistrationStatus.APPROVED);
    final var subordinate = this.subordinateRepository
        .findByEntityidentifierAndTaIm_TaImId(entityId, this.trustAnchorId).orElseThrow();
    assertThat(subordinate.getMetadata()).isEqualTo(Map.of("openid_relying_party", Map.of(
        "organization_name#sv", "Testorganisationen",
        "organization_name#en", "The Test Organization",
        "organization_name", "TestOrg5 AB",
        "organization_identifier", "urn:glue:iso6523:0007:" + APPLICANT.orgId)));
    assertThat(subordinate.getMetadataPolicy()).isEqualTo(Map.of("metadata_policy", Map.of(
        "openid_relying_party", Map.of(
            "client_name#sv", Map.of("value", "Min tjänst"),
            "client_name#en", Map.of("value", "My service"),
            "client_name", Map.of("value", "Min tjänst"),
            "display_name#sv", Map.of("value", "Min tjänst"),
            "display_name#en", Map.of("value", "My service"),
            "display_name", Map.of("value", "Min tjänst")))));
  }

  @Test
  @DisplayName("A failed check after the review declines the registration and says why")
  void failedCheckFailsTheRegistration() {
    final Registration submitted = new FederationRegistrationApi(this.apiClient(APPLICANT)).createJoinWithId(
        TENANT, APPLICANT.orgId, this.assignId, new RegistrationJoinRequest().entityIdentifier(this.entityId("wrongid")));
    final var paused = this.registrationRepository.findById(submitted.getRegistrationId()).orElseThrow();

    new RegistrationAdminApi(this.apiClient(OPERATOR))
        .approveStep(TENANT, OPERATOR.orgId, submitted.getRegistrationId(), paused.getPendingStepIndex());

    final var failed = this.registrationRepository.findById(submitted.getRegistrationId()).orElseThrow();
    assertThat(failed.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(failed.getRejectionReason()).startsWith("Automatically declined");
    assertThat(failed.getPendingStepIndex()).isNull();
    assertThat(failed.getStepResults().getLast()).satisfies(step -> {
      assertThat(step.stepName()).isEqualTo("MetadataPolicyCreationStep");
      assertThat(step.status()).isEqualTo("FAILURE");
      assertThat(step.issues()).anySatisfy(issue -> assertThat(issue.message())
          .contains("organization_identifier does not match"));
    });
  }

  @Test
  @DisplayName("A flow with an unusable metadata policy template is refused when saved")
  void unusableTemplateIsRefused() {
    assertThatThrownBy(() -> this.flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Broken template " + UUID.randomUUID())
        .description("Has no entity types")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(METADATA_POLICY_CREATION_STEP_ID)
            .name("MetadataPolicyCreationStep")
            .description("Builds the claims")
            .config(List.of(new ConfigValueDto().key("template").value("{\"entity_types\": {}}")))))))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> {
          assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(400);
          assertThat(((RestClientResponseException) ex).getResponseBodyAsString())
              .contains("entity_types must name at least one entity type");
        });
  }

  private String entityId(final String path) {
    return "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path;
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private void setUpApplicant() {
    // The database lives as long as the class; the applicant and its domain are set up once.
    if (applicantReady) {
      return;
    }
    applicantReady = true;
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(APPLICANT));
    organizationApi.createOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("TestOrg5 AB"));
    final Domain domain = organizationApi.requestDomain(TENANT, APPLICANT.orgId,
        new DomainRequest().domain("localhost"));
    new RegistrationAdminApi(this.apiClient(OPERATOR)).approveDomain(TENANT, OPERATOR.orgId, domain.getDomainId());
  }

  private void setUpFlow() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT,
        OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    this.trustAnchorId = trustAnchor.getTrustAnchorId();

    this.flowApi = new RegistrationFlowApi(operatorClient);
    final RegistrationFlowDto flow = this.flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Metadata policy " + UUID.randomUUID())
        .description("Loads, reviews and pins the entity's metadata")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(
            new StepDto().stepId(LOAD_ENTITY_CONFIGURATION_STEP_ID).name("LoadEntityConfigurationStep")
                .description("Loads the entity configuration"),
            new StepDto().stepId(METADATA_REVIEW_STEP_ID).name("MetadataReviewStep")
                .description("Operator reviews the metadata"),
            new StepDto().stepId(METADATA_POLICY_CREATION_STEP_ID).name("MetadataPolicyCreationStep")
                .description("Builds metadata and metadata_policy from the default template"))));
    this.assignId = this.flowApi.assignFlow(TENANT, OPERATOR.orgId, this.trustAnchorId,
        new AssignFlowRequest().flowId(flow.getFlowId())).getAssignId();
  }

  private void stubEntityConfiguration(final String path, final String organizationIdentifier) throws Exception {
    final ECKey signingKey = (ECKey) TestDataOperations.genKey();
    final String entityId = this.entityId(path);
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(entityId)
        .subject(entityId)
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .claim("jwks", new JWKSet(signingKey.toPublicJWK()).toJSONObject())
        .claim("metadata", Map.of(
            "openid_relying_party", Map.of(
                "organization_name#sv", "Testorganisationen",
                "organization_name#en", "The Test Organization",
                "organization_identifier", organizationIdentifier,
                "client_name#sv", "Min tjänst",
                "client_name#en", "My service",
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
