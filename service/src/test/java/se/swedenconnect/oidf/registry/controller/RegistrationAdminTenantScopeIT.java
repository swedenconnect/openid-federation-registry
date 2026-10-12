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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.RegistrationAdminApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.AssignFlowResponse;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.RejectRegistrationRequest;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationStepRepository;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.util.List;
import java.util.function.Consumer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Registration review is a tenant-wide operator duty. Any organization listed in the tenant's
 * {@code operator_organizations} reviews every registration submitted on that tenant, whichever organization owns the
 * intermediate. An operator acting on another tenant, and an organization that operates no tenant, must not reach the
 * registration at all: a foreign registration ID answers 404 (or 403 for a non-operator), exactly as a nonexistent
 * one, so registration IDs cannot be probed.
 *
 * @author Felix Hellman
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class RegistrationAdminTenantScopeIT {

  private static final String TENANT = "Swedenconnect";
  /** Operates ENA as well as Swedenconnect (operator_organizations in application.yml). */
  private static final String OTHER_TENANT = "ENA";
  private static final JwtTestUtils.OrganisationType ALICE = JwtTestUtils.OrganisationType.PM;
  /** Operates Swedenconnect but owns no intermediate. */
  private static final JwtTestUtils.OrganisationType CHARLIE = JwtTestUtils.OrganisationType.SKATT;
  /** Operates no tenant. */
  private static final JwtTestUtils.OrganisationType MEMBER = JwtTestUtils.OrganisationType.TESTORG1;
  private static final UUID PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID =
      UUID.fromString("AE67B1D8-2DCF-4A8C-9E6B-FC972CC65DEA");

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private FlowAssignmentRepository flowAssignmentRepository;
  @Autowired
  private TaImRepository taImRepository;
  @Autowired
  private RegistrationRepository registrationRepository;
  @Autowired
  private RegistrationStepRepository registrationStepRepository;

  private RegistrationAdminApi aliceAdminApi;
  private RegistrationAdminApi charlieAdminApi;
  private RegistrationAdminApi otherTenantAdminApi;
  private RegistrationAdminApi memberAdminApi;
  private FlowAssignment aliceFlowAssignment;
  private Organization aliceOrganization;

  @BeforeEach
  void setUp() {
    final ApiClient aliceClient = buildApiClient(ALICE);
    this.aliceAdminApi = new RegistrationAdminApi(aliceClient);
    this.charlieAdminApi = new RegistrationAdminApi(buildApiClient(CHARLIE));
    this.otherTenantAdminApi = new RegistrationAdminApi(aliceClient);
    this.memberAdminApi = new RegistrationAdminApi(buildApiClient(MEMBER));

    // Alice provisions the intermediate and flow. Charlie owns no intermediate: as a tenant operator he reviews
    // registrations on every intermediate of the tenant.
    final EntitiesApi entitiesApi = new EntitiesApi(aliceClient);
    final ModulesApi modulesApi = new ModulesApi(aliceClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(aliceClient);

    final String taEntityId = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, ALICE.orgId,
        FederationEntity.builder().entityIdentifier(taEntityId).build());
    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(TENANT, ALICE.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, ALICE.orgId, new RegistrationFlowDto()
        .name("Tenant scope fixture flow")
        .description("Fixture flow for RegistrationAdminTenantScopeIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(PREDEFINED_DIRECT_REGISTER_FLOW_STEP_ID)
            .name("PredefinedDirectRegisterFlow")
            .description("Loads entity configuration and publishes subordinate statement"))));
    final AssignFlowResponse assignment = flowApi.assignFlow(TENANT, ALICE.orgId, trustAnchor.getTrustAnchorId(),
        new AssignFlowRequest().flowId(flow.getFlowId()));

    // Fetched directly through the repositories (not the REST layer) so the fixture registrations
    // below can be persisted straight to the database.
    this.aliceFlowAssignment = this.flowAssignmentRepository.findById(assignment.getAssignId()).orElseThrow();
    final TrustAnchorIntermediateModule aliceTaIm =
        this.taImRepository.findById(trustAnchor.getTrustAnchorId()).orElseThrow();
    this.aliceOrganization = aliceTaIm.getOrganization();
  }

  private ApiClient buildApiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private se.swedenconnect.oidf.registry.registrations.model.Registration pendingAliceRegistration(
      final String entityId, final int pendingStepIndex) {
    // approveStep() re-slices reg.getStepResults() up to stepIndex to merge it with the resumed
    // steps' results, so a real registration reaching PENDING_APPROVAL at stepIndex always has at
    // least that many stored already — placeholders satisfy that precondition for the fixture.
    final List<se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto> placeholderStepResults =
        java.util.stream.IntStream.range(0, pendingStepIndex)
            .mapToObj(i -> new se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto(
                "placeholder-" + i, "SUCCESS", null, List.of(), List.of()))
            .toList();
    final se.swedenconnect.oidf.registry.registrations.model.Registration registration =
        se.swedenconnect.oidf.registry.registrations.model.Registration.builder()
            .registrationId(UUID.randomUUID())
            .flowAssignment(this.aliceFlowAssignment)
            .organization(this.aliceOrganization)
            .entityId(entityId)
            .registrationType(RegistrationType.SUBORDINATE)
            .status(RegistrationStatus.PENDING_APPROVAL)
            .pendingStepIndex(pendingStepIndex)
            .stepResults(placeholderStepResults)
            .build();
    return this.registrationRepository.save(registration);
  }

  private int pastTheEndOfTheFlow() {
    // One past the end of the assembled step list: approving it resumes with nothing left to run, which isolates
    // the assertion to who may approve rather than to any particular step's behavior.
    return this.registrationStepRepository.preDefaultSteps().size()
        + 1 // the one PredefinedDirectRegisterFlow MID step configured on Alice's flow
        + this.registrationStepRepository.postDefaultSteps().size();
  }

  private static Consumer<Throwable> status(final int expected) {
    return ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(expected);
  }

  @Test
  @DisplayName("An operator of the tenant sees a registration on an intermediate it does not own")
  void operatorOfTheTenantSeesEveryRegistration() {
    final UUID registrationId = this.pendingAliceRegistration("https://alice.example/sp/get", 0).getRegistrationId();

    assertThat(this.charlieAdminApi.getById1(TENANT, CHARLIE.orgId, registrationId).getRegistrationId())
        .isEqualTo(registrationId);
    assertThat(this.charlieAdminApi.listRegistrations1(TENANT, CHARLIE.orgId))
        .extracting(Registration::getRegistrationId)
        .contains(registrationId);
  }

  @Test
  @DisplayName("An operator of the tenant can reject a registration on an intermediate it does not own")
  void operatorOfTheTenantCanReject() {
    final UUID registrationId =
        this.pendingAliceRegistration("https://alice.example/sp/reject", 0).getRegistrationId();

    final Registration rejected = this.charlieAdminApi.reject(TENANT, CHARLIE.orgId, registrationId,
        new RejectRegistrationRequest().rejectionReason("Not part of the federation"));

    assertThat(rejected.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.REJECTED);
    assertThat(this.registrationRepository.findById(registrationId).orElseThrow().getReviewedBy()).isNotBlank();
  }

  @Test
  @DisplayName("An operator of the tenant can approve a step of a registration on an intermediate it does not own")
  void operatorOfTheTenantCanApprove() {
    final int step = this.pastTheEndOfTheFlow();
    final UUID registrationId =
        this.pendingAliceRegistration("https://alice.example/sp/approve", step).getRegistrationId();

    this.charlieAdminApi.approveStep(TENANT, CHARLIE.orgId, registrationId, step);

    assertThat(this.registrationRepository.findById(registrationId).orElseThrow().getReviewedBy()).isNotBlank();
  }

  @Test
  @DisplayName("An operator acting on another tenant cannot see, reject or approve the registration")
  void operatorOfAnotherTenantIsKeptOut() {
    final int step = this.pastTheEndOfTheFlow();
    final UUID registrationId =
        this.pendingAliceRegistration("https://alice.example/sp/other-tenant", step).getRegistrationId();

    assertThatThrownBy(() -> this.otherTenantAdminApi.getById1(OTHER_TENANT, ALICE.orgId, registrationId))
        .isInstanceOf(RestClientResponseException.class).satisfies(status(404));
    assertThatThrownBy(() -> this.otherTenantAdminApi.reject(OTHER_TENANT, ALICE.orgId, registrationId,
        new RejectRegistrationRequest().rejectionReason("Wrong tenant")))
        .isInstanceOf(RestClientResponseException.class).satisfies(status(404));
    assertThatThrownBy(() -> this.otherTenantAdminApi.approveStep(OTHER_TENANT, ALICE.orgId, registrationId, step))
        .isInstanceOf(RestClientResponseException.class).satisfies(status(404));
    assertThat(this.otherTenantAdminApi.listRegistrations1(OTHER_TENANT, ALICE.orgId))
        .extracting(Registration::getRegistrationId)
        .doesNotContain(registrationId);

    assertThat(this.registrationRepository.findById(registrationId).orElseThrow().getStatus())
        .isEqualTo(RegistrationStatus.PENDING_APPROVAL);
  }

  @Test
  @DisplayName("An organization that operates no tenant cannot reach the registration")
  void nonOperatorIsKeptOut() {
    final UUID registrationId =
        this.pendingAliceRegistration("https://alice.example/sp/member", 0).getRegistrationId();

    assertThatThrownBy(() -> this.memberAdminApi.getById1(TENANT, MEMBER.orgId, registrationId))
        .isInstanceOf(RestClientResponseException.class).satisfies(status(403));
    assertThatThrownBy(() -> this.memberAdminApi.reject(TENANT, MEMBER.orgId, registrationId,
        new RejectRegistrationRequest().rejectionReason("Not an operator")))
        .isInstanceOf(RestClientResponseException.class).satisfies(status(403));

    assertThat(this.registrationRepository.findById(registrationId).orElseThrow().getStatus())
        .isEqualTo(RegistrationStatus.PENDING_APPROVAL);
  }
}
