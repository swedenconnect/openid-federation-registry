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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.OrganizationApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.AssignFlowResponse;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the number behind the badge on the Registrations tab, {@code GET .../entity-reviews/count}. It must be the
 * number of rows the review list shows: one per entity that waits for a decision, and one per pending domain that no
 * such entity waits for. See issue #408, where the badge summed registrations and domains and so counted an entity's
 * own pending domain twice and every trust mark request on its own.
 * <p>
 * The registration is written straight through {@link RegistrationRepository}, the way
 * {@link FederationRegistrationCrossOrganizationIT} does it: the registration pipeline is not under test here, only
 * what the count makes of the registrations and domains it finds.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class ReviewCountIT {

  private static final String TENANT = "Swedenconnect";
  /** The Swedenconnect instance of the test configuration, which {@code OPERATOR} operates. */
  private static final UUID INSTANCE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG1;
  private static final JwtTestUtils.OrganisationType DOMAIN_ONLY_APPLICANT = JwtTestUtils.OrganisationType.TESTORG2;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private FlowAssignmentRepository flowAssignmentRepository;
  @Autowired
  private OrganizationRepository organizationRepository;
  @Autowired
  private RegistrationRepository registrationRepository;

  private FlowAssignment flowAssignment;

  @BeforeEach
  void setUp() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final String taEntityId = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier(taEntityId).build());
    final TrustAnchor trustAnchor = new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(operatorClient);
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, OPERATOR.orgId, new RegistrationFlowDto()
        .name("Review count fixture flow")
        .description("Fixture flow for ReviewCountIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(UUID.randomUUID())
            .name("MinimalStep")
            .description("Test step"))));
    final AssignFlowResponse assignment = flowApi.assignFlow(TENANT, OPERATOR.orgId, trustAnchor.getTrustAnchorId(),
        new AssignFlowRequest().flowId(flow.getFlowId()));

    this.flowAssignment = this.flowAssignmentRepository.findById(assignment.getAssignId()).orElseThrow();
  }

  @Test
  @DisplayName("An entity waiting for its own pending domain is one item, not one per pending thing")
  void anEntityAndItsPendingDomainAreOneItem() {
    final long countBefore = this.count(OPERATOR, "admin");
    final int rowsBefore = this.summaries().size();
    final String domain = "entity.example.com";

    this.claimDomain(APPLICANT, "TestOrg1 AB", domain);
    this.pendingRegistration(APPLICANT, "https://sp." + domain);

    assertThat(this.summaries())
        .as("one new row, reporting the pending domain that the naive sum counted a second time")
        .hasSize(rowsBefore + 1)
        .anySatisfy(summary -> assertThat(summary).containsEntry("pendingDomains", 1));

    assertThat(this.count(OPERATOR, "admin"))
        .as("one more item, the entity: its own pending domain is not counted again on its own")
        .isEqualTo(countBefore + 1);
  }

  @Test
  @DisplayName("A pending domain no entity waits for is counted on its own")
  void aStandaloneDomainIsCountedOnItsOwn() {
    final long before = this.count(OPERATOR, "admin");

    this.claimDomain(DOMAIN_ONLY_APPLICANT, "TestOrg2 AB", "standalone.example.com");

    assertThat(this.count(OPERATOR, "admin"))
        .as("the domain is reviewed on its own, so it is counted on its own")
        .isEqualTo(before + 1);
  }

  @Test
  @DisplayName("An operator with read rights gets the count, which the domain count refused")
  void readOnlyOperatorGetsTheCount() {
    this.claimDomain(APPLICANT, "TestOrg1 AB", "readonly.example.com");
    this.pendingRegistration(APPLICANT, "https://sp.readonly.example.com");

    assertThat(this.count(OPERATOR, "read")).isEqualTo(this.count(OPERATOR, "admin"));
  }

  // --- fixture ---

  /** Bootstraps the organization, unless this test already did, and claims a domain for it. */
  private void claimDomain(final JwtTestUtils.OrganisationType applicant, final String legalName,
      final String domain) {
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(applicant));
    if (this.organizationRepository.findByInstance_InstanceIdAndOrgNumber(INSTANCE_ID, applicant.orgId)
        .isEmpty()) {
      organizationApi.createOrganization(TENANT, applicant.orgId,
          new CreateOrganizationRequest().legalName(legalName));
    }
    organizationApi.requestDomain(TENANT, applicant.orgId, new DomainRequest().domain(domain));
  }

  /** A registration of the applicant's own, waiting for the operator to decide it. */
  private void pendingRegistration(final JwtTestUtils.OrganisationType applicant, final String entityId) {
    final Organization organization = this.organizationRepository
        .findByInstance_InstanceIdAndOrgNumber(INSTANCE_ID, applicant.orgId).orElseThrow();
    this.registrationRepository.save(Registration.builder()
        .registrationId(UUID.randomUUID())
        .flowAssignment(this.flowAssignment)
        .organization(organization)
        .entityId(entityId)
        .registrationType(RegistrationType.SUBORDINATE)
        .status(RegistrationStatus.PENDING_APPROVAL)
        .pendingStepIndex(0)
        .stepResults(List.of())
        .build());
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> summaries() {
    return this.reviews(OPERATOR, "admin").get().retrieve().body(List.class);
  }

  @SuppressWarnings("unchecked")
  private long count(final JwtTestUtils.OrganisationType org, final String right) {
    final Map<String, Number> body =
        this.reviews(org, right).get().uri("/count").retrieve().body(Map.class);
    return body.get("count").longValue();
  }

  private RestClient reviews(final JwtTestUtils.OrganisationType org, final String right) {
    return RestClient.builder()
        .baseUrl("http://localhost:%d/registration-admin/v1/%s/%s/entity-reviews".formatted(this.port, TENANT,
            org.orgId))
        .defaultHeader("Authorization", "Bearer " + this.jwtTestUtils.createJwt(org, "swedenconnect", right))
        .build();
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }
}
