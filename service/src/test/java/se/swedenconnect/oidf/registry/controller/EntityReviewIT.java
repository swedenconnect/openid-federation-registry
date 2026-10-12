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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
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
import se.swedenconnect.oidf.registry.api.model.ConfigValueDto;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
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
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
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
 * Reviewing an entity in one session: its domain, then its trust mark request, then its own registration, with
 * deferral, a domain shared by two entities, and a domain rejection ending the review.
 *
 * <p>The applicant's one domain, {@code localhost}, is shared by every entity here. The tests run in order: the first
 * rejects the domain, and the later ones claim it again, which re-opens it for review.</p>
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EntityReviewIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID TRUST_MARK_ISSUER_REGISTRATION_STEP_ID =
      UUID.fromString("F1A2B3C4-D5E6-4F7A-8B9C-0D1E2F3A4B5C");
  private static final UUID LOAD_ENTITY_CONFIGURATION_STEP_ID =
      UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  private static final UUID HOSTED_ENTITY_REGISTRATION_STEP_ID =
      UUID.fromString("C3F1A820-5D7B-4E9A-B034-1F6D9A3C7E82");
  private static final UUID METADATA_REVIEW_STEP_ID = UUID.fromString("5D2C8E14-7A3B-4C9F-9E61-2B8F4A7D0C35");
  private static final UUID ADD_TRUST_MARK_SUBJECT_STEP_ID =
      UUID.fromString("3F8A1C2D-7E4B-4F9A-B5D6-8C0E2A3F1B4D");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  /** Also an operator of the tenant: continues a review the other operator started. */
  private static final JwtTestUtils.OrganisationType OTHER_OPERATOR = JwtTestUtils.OrganisationType.SKATT;
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG6;
  private static final int WIREMOCK_HTTPS_PORT = 6893;
  private static final int WIREMOCK_HTTP_PORT = 6792;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  private static UUID assignId;
  private static UUID assignIdWithHostedStep;
  private static String trustMarkIssuerEntityId;
  private static String trustMarkType;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private RegistrationRepository registrationRepository;

  private WireMockServer wireMockServer;

  @BeforeEach
  void setUp() throws Exception {
    this.wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(WIREMOCK_HTTP_PORT)
        .httpsPort(WIREMOCK_HTTPS_PORT)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")
        .notifier(new ConsoleNotifier("entity-review", false)));
    this.wireMockServer.start();
    configureFor("localhost", this.wireMockServer.port());
    for (final String path : List.of("rejected", "first", "second", "third")) {
      this.stubEntityConfiguration(path);
    }
    if (assignId == null) {
      this.setUpFlows();
      new OrganizationApi(this.apiClient(APPLICANT)).createOrganization(TENANT, APPLICANT.orgId,
          new CreateOrganizationRequest().legalName("TestOrg6 AB"));
    }
  }

  @AfterEach
  void tearDown() {
    this.wireMockServer.stop();
  }

  @Test
  @Order(1)
  @DisplayName("Rejecting the domain in a review rejects the entity and ends the review")
  void rejectingTheDomainEndsTheReview() {
    this.claimDomain();
    final UUID entity = this.register("rejected").getRegistrationId();

    this.start(OPERATOR, entity);
    final Map<String, Object> domain = this.items(this.review(entity)).getFirst();
    final Map<String, Object> after = this.decide(OPERATOR, entity, domain, "REJECTED", "Not your domain");

    assertThat(after.get("registrationStatus")).isEqualTo("REJECTED");
    assertThat(after.get("reviewState")).isEqualTo("COMPLETED");
    assertThat(((Map<?, ?>) after.get("review")).get("status")).isEqualTo("COMPLETED");
  }

  @Test
  @Order(2)
  @DisplayName("An entity is reviewed domain first, then trust mark, then its own step, across operators")
  void reviewInOrder() {
    this.claimDomain();
    final UUID entity = this.register("first").getRegistrationId();

    assertThat(this.summaries()).anySatisfy(summary -> {
      assertThat(summary.get("registrationId")).isEqualTo(entity.toString());
      assertThat(summary.get("reviewState")).isEqualTo("NOT_STARTED");
      assertThat(summary).containsEntry("pendingDomains", 1).containsEntry("pendingTrustMarks", 1)
          .containsEntry("pendingEntitySteps", 1);
    });
    assertThatThrownBy(() -> this.decide(OPERATOR, entity, this.items(this.review(entity)).getFirst(), "APPROVED",
        null)).satisfies(status(409));

    List<Map<String, Object>> items = this.items(this.start(OPERATOR, entity));
    assertThat(items).extracting(item -> item.get("type") + ":" + item.get("state"))
        .containsExactly("DOMAIN:PENDING", "TRUST_MARK:WAITING", "ENTITY:WAITING");
    assertThat(items.get(2)).containsEntry("title", "MetadataReviewStep");
    assertThatThrownBy(() -> this.decide(OPERATOR, entity, this.items(this.review(entity)).get(1), "APPROVED", null))
        .satisfies(status(409));

    items = this.items(this.decide(OPERATOR, entity, items.getFirst(), "DEFERRED", null));
    assertThat(items.getFirst()).containsEntry("state", "PENDING").containsEntry("deferred", true);
    assertThat(this.summaries()).anySatisfy(summary -> assertThat(summary)
        .containsEntry("registrationId", entity.toString()).containsEntry("reviewState", "IN_REVIEW"));

    // Another operator of the tenant continues the review
    items = this.items(this.start(OTHER_OPERATOR, entity));
    items = this.items(this.decide(OTHER_OPERATOR, entity, items.getFirst(), "APPROVED", null));
    assertThat(items).extracting(item -> item.get("type") + ":" + item.get("state"))
        .containsExactly("DOMAIN:APPROVED", "TRUST_MARK:PENDING", "ENTITY:WAITING");

    items = this.items(this.decide(OTHER_OPERATOR, entity, items.get(1), "APPROVED", null));
    assertThat(items.get(2)).containsEntry("state", "PENDING");
    assertThat((Map<?, ?>) items.get(2).get("preview")).isNotNull();

    final Map<String, Object> done = this.decide(OTHER_OPERATOR, entity, items.get(2), "APPROVED", null);
    assertThat(done.get("registrationStatus")).isEqualTo("APPROVED");
    assertThat(done.get("reviewState")).isEqualTo("COMPLETED");
    assertThat(this.registrationRepository.findById(entity).orElseThrow().getStatus())
        .isEqualTo(RegistrationStatus.APPROVED);
  }

  @Test
  @Order(3)
  @DisplayName("A domain decided in one entity's review shows as decided in another's, which is still not started")
  void sharedDomainIsDecidedOnce() {
    final UUID second = this.register("second").getRegistrationId();

    final Map<String, Object> secondReview = this.review(second);
    assertThat(secondReview.get("reviewState")).isEqualTo("NOT_STARTED");
    assertThat(this.items(secondReview).getFirst())
        .containsEntry("type", "DOMAIN")
        .containsEntry("state", "APPROVED")
        .containsEntry("decidedInReviewOf", "https://localhost:" + WIREMOCK_HTTPS_PORT + "/first");
  }

  @Test
  @Order(4)
  @DisplayName("Steps that will not run for the entity are not listed as upcoming")
  void upcomingStepsOnlyThoseThatApply() {
    final UUID entity = new FederationRegistrationApi(this.apiClient(APPLICANT)).createJoinWithId(TENANT,
        APPLICANT.orgId, assignIdWithHostedStep, new RegistrationJoinRequest()
            .entityIdentifier("https://localhost:" + WIREMOCK_HTTPS_PORT + "/third")).getRegistrationId();

    assertThat(this.items(this.review(entity)))
        .filteredOn(item -> "ENTITY".equals(item.get("type")))
        .extracting(item -> item.get("title") + ":" + item.get("state"))
        .containsExactly("LoadEntityConfigurationStep:PENDING", "MetadataReviewStep:UPCOMING");
  }

  // --- helpers ---

  private static java.util.function.Consumer<Throwable> status(final int expected) {
    return ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(expected);
  }

  private void claimDomain() {
    new OrganizationApi(this.apiClient(APPLICANT)).requestDomain(TENANT, APPLICANT.orgId,
        new DomainRequest().domain("localhost"));
  }

  private Registration register(final String path) {
    return new FederationRegistrationApi(this.apiClient(APPLICANT)).createJoinWithId(TENANT, APPLICANT.orgId,
        assignId, new RegistrationJoinRequest()
            .entityIdentifier("https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path)
            .trustmarksRequested(List.of(new TrustmarkRequest()
                .trustmarkIssuer(trustMarkIssuerEntityId)
                .trustmarkType(List.of(trustMarkType)))));
  }

  private RestClient reviews(final JwtTestUtils.OrganisationType org) {
    return RestClient.builder()
        .baseUrl("http://localhost:%d/registration-admin/v1/%s/%s/entity-reviews".formatted(this.port, TENANT,
            org.orgId))
        .defaultHeader("Authorization", "Bearer " + this.jwtTestUtils.createJwt(org))
        .build();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> summaries() {
    return this.reviews(OPERATOR).get().retrieve().body(List.class);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> review(final UUID entity) {
    return this.reviews(OPERATOR).get().uri("/{id}", entity).retrieve().body(Map.class);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> start(final JwtTestUtils.OrganisationType org, final UUID entity) {
    return this.reviews(org).post().uri("/{id}/start", entity).retrieve().body(Map.class);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> decide(final JwtTestUtils.OrganisationType org, final UUID entity,
      final Map<String, Object> item, final String decision, final String reason) {
    final Map<String, Object> request = new HashMap<>();
    request.put("itemType", item.get("type"));
    request.put("itemId", item.get("itemId"));
    request.put("stepIndex", item.get("stepIndex"));
    request.put("decision", decision);
    request.put("reason", reason);
    return this.reviews(org).post().uri("/{id}/decisions", entity).contentType(MediaType.APPLICATION_JSON)
        .body(request).retrieve().body(Map.class);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> items(final Map<String, Object> review) {
    return (List<Map<String, Object>>) review.get("items");
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private void setUpFlows() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final EntitiesApi entitiesApi = new EntitiesApi(operatorClient);
    final ModulesApi modulesApi = new ModulesApi(operatorClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);

    trustMarkIssuerEntityId = "https://www.pm.se/oidf/tmi/review/" + UUID.randomUUID();
    trustMarkType = "https://www.pm.se/oidf/tm/review/" + UUID.randomUUID();
    final FederationEntity tmiEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier(trustMarkIssuerEntityId).build());
    final TrustmarkIssuer trustmarkIssuer = modulesApi.createTrustmarkIssuer(TENANT, OPERATOR.orgId,
        new TrustmarkIssuer().entityId(tmiEntity.getEntityId()).active(true).trustMarkTokenValidityDuration("PT1H"));
    final Trustmark trustmark = new TrustmarksApi(operatorClient).createTrustmark(TENANT, OPERATOR.orgId,
        new Trustmark().trustmarkissuerId(trustmarkIssuer.getTrustmarkIssuerId()).trustmarkType(trustMarkType));
    final RegistrationFlowDto trustMarkFlow = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Review trust mark " + UUID.randomUUID())
        .description("AddTrustMarkSubjectStep gated on manual review")
        .flowType(RegistrationFlowDto.FlowTypeEnum.TRUST_MARK_ISSUER)
        .steps(List.of(new StepDto().stepId(ADD_TRUST_MARK_SUBJECT_STEP_ID).name("AddTrustMarkSubjectStep")
            .description("Validates the requested trust mark types")
            .config(List.of(new ConfigValueDto().key("manualreview").value("true"))))));
    flowApi.assignFlowToTrustMark(TENANT, OPERATOR.orgId, trustmark.getTrustmarkId(),
        new AssignFlowRequest().flowId(trustMarkFlow.getFlowId()));

    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Entity review " + UUID.randomUUID())
        .description("Trust marks, then the entity configuration, then metadata review")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(
            new StepDto().stepId(TRUST_MARK_ISSUER_REGISTRATION_STEP_ID).name("TrustMarkIssuerRegistrationStep")
                .description("Dispatches the trust mark sub-flow"),
            new StepDto().stepId(LOAD_ENTITY_CONFIGURATION_STEP_ID).name("LoadEntityConfigurationStep")
                .description("Loads the entity configuration"),
            new StepDto().stepId(METADATA_REVIEW_STEP_ID).name("MetadataReviewStep")
                .description("Operator reviews the metadata"))));
    assignId = flowApi.assignFlow(TENANT, OPERATOR.orgId, trustAnchor.getTrustAnchorId(),
        new AssignFlowRequest().flowId(flow.getFlowId())).getAssignId();

    // Both entity sources are in the flow and both pause; a fetched entity only ever runs the first
    final RegistrationFlowDto withHostedStep = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Entity review with hosted step " + UUID.randomUUID())
        .description("Fetched or hosted, then metadata review")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(
            new StepDto().stepId(LOAD_ENTITY_CONFIGURATION_STEP_ID).name("LoadEntityConfigurationStep")
                .description("Loads the entity configuration")
                .config(List.of(new ConfigValueDto().key("manualreview").value("true"))),
            new StepDto().stepId(HOSTED_ENTITY_REGISTRATION_STEP_ID).name("HostedEntityRegistrationStep")
                .description("Hosts the entity")
                .config(List.of(new ConfigValueDto().key("manualreview").value("true"))),
            new StepDto().stepId(METADATA_REVIEW_STEP_ID).name("MetadataReviewStep")
                .description("Operator reviews the metadata"))));
    assignIdWithHostedStep = flowApi.assignFlow(TENANT, OPERATOR.orgId, trustAnchor.getTrustAnchorId(),
        new AssignFlowRequest().flowId(withHostedStep.getFlowId())).getAssignId();
  }

  private void stubEntityConfiguration(final String path) throws Exception {
    final ECKey signingKey = (ECKey) TestDataOperations.genKey();
    final String entityId = "https://localhost:" + WIREMOCK_HTTPS_PORT + "/" + path;
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(entityId)
        .subject(entityId)
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .claim("jwks", new JWKSet(signingKey.toPublicJWK()).toJSONObject())
        .claim("metadata", Map.of("openid_relying_party", Map.of(
            "client_name#sv", "Min tjänst",
            "client_name#en", "My service",
            "redirect_uris", List.of(entityId + "/callback"))))
        .build();
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(signingKey.getKeyID())
        .build(), claims);
    jwt.sign(new ECDSASigner(signingKey));
    stubFor(get(urlPathEqualTo("/" + path + "/.well-known/openid-federation"))
        .willReturn(ok(jwt.serialize()).withHeader("Content-Type", "application/entity-statement+jwt")));
  }
}
