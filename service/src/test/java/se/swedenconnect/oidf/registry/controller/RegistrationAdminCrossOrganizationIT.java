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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces the cross-organization registration-admin IDOR from the external vulnerability report: an organization
 * with legitimate READ/WRITE rights on its own organization number ("Charlie") can substitute another organization's
 * ("Alice's") registration UUID in the path and act on it anyway.
 * <p>
 * {@code RegistrationAdminController.getById/reject/approveStep} resolve an {@code OrganizationRecord} for the caller
 * but never pass it to the service layer, so the lookup underneath is a plain, unscoped {@code findById} with no
 * ownership check.
 * <p>
 * Each test here asserts the SECURE behavior (a foreign-org registration must 404) and therefore currently FAILS — that
 * failure documents the vulnerability. Once the lookups are scoped to the caller's own organization, these tests should
 * pass unmodified.
 * <p>
 * Alice's pending registrations are inserted directly via {@link RegistrationRepository} rather than driven through the
 * real registration pipeline (which would need WireMock-stubbed entity statements, as in
 * {@code RegistrationFlowEndToEndIT}) — the pipeline itself isn't what's under test here, only the admin endpoints'
 * missing ownership check. Charlie needs no setup at all beyond a valid token for his own organization.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class RegistrationAdminCrossOrganizationIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType ALICE = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType CHARLIE = JwtTestUtils.OrganisationType.AF;
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
  private FlowAssignment aliceFlowAssignment;
  private Organization aliceOrganization;

  @BeforeEach
  void setUp() {
    final ApiClient aliceClient = buildApiClient(ALICE);
    this.aliceAdminApi = new RegistrationAdminApi(aliceClient);
    this.charlieAdminApi = new RegistrationAdminApi(buildApiClient(CHARLIE));

    // Alice provisions her own intermediate and flow. Charlie needs none of this — only a valid
    // token carrying rights on his own organization number.
    final EntitiesApi entitiesApi = new EntitiesApi(aliceClient);
    final ModulesApi modulesApi = new ModulesApi(aliceClient);
    final RegistrationFlowApi flowApi = new RegistrationFlowApi(aliceClient);

    final String taEntityId = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, ALICE.orgId,
        FederationEntity.builder().entityIdentifier(taEntityId).build());
    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(TENANT, ALICE.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, ALICE.orgId, new RegistrationFlowDto()
        .name("Cross-org IDOR fixture flow")
        .description("Fixture flow for RegistrationAdminCrossOrganizationIT")
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

  @Test
  @DisplayName("getById must not leak another organization's registration — currently FAILS: Charlie's "
      + "own valid org path plus Alice's registration UUID returns 200 with her data")
  void getByIdMustNotLeakAnotherOrganizationsRegistration() {
    final UUID aliceRegistrationId =
        this.pendingAliceRegistration("https://alice.example/sp/get", 0).getRegistrationId();

    assertThatThrownBy(() -> this.charlieAdminApi.getById1(TENANT, CHARLIE.orgId, aliceRegistrationId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
  }

  @Test
  @DisplayName("reject must not mutate another organization's registration — currently FAILS: Charlie can "
      + "reject Alice's pending registration through his own valid org path")
  void rejectMustNotMutateAnotherOrganizationsRegistration() {
    final UUID aliceRegistrationId =
        this.pendingAliceRegistration("https://alice.example/sp/reject", 0).getRegistrationId();

    assertThatThrownBy(() -> this.charlieAdminApi.reject(TENANT, CHARLIE.orgId, aliceRegistrationId,
        new RejectRegistrationRequest().rejectionReason("Cross-org exploit")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));

    final Registration stillOwnedByAlice = this.aliceAdminApi.getById1(TENANT, ALICE.orgId, aliceRegistrationId);
    assertThat(stillOwnedByAlice.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.PENDING_APPROVAL);
  }

  @Test
  @DisplayName("approveStep must not mutate another organization's registration — currently FAILS: "
      + "Charlie can approve Alice's pending step through his own valid org path")
  void approveStepMustNotMutateAnotherOrganizationsRegistration() {
    // One past the end of the real assembled step list, so *if* the ownership check is bypassed,
    // the pipeline has nothing left to run and simply completes — this isolates the assertion to
    // the ownership gap rather than to any particular step's behavior.
    final int pastTheEndOfTheFlow = this.registrationStepRepository.preDefaultSteps().size()
        + 1 // the one PredefinedDirectRegisterFlow MID step configured on Alice's flow
        + this.registrationStepRepository.postDefaultSteps().size();
    final UUID aliceRegistrationId = this.pendingAliceRegistration(
        "https://alice.example/sp/approve", pastTheEndOfTheFlow).getRegistrationId();

    assertThatThrownBy(() -> this.charlieAdminApi.approveStep(
        TENANT, CHARLIE.orgId, aliceRegistrationId, pastTheEndOfTheFlow))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));

    final Registration stillOwnedByAlice = this.aliceAdminApi.getById1(TENANT, ALICE.orgId, aliceRegistrationId);
    assertThat(stillOwnedByAlice.getStatusFedreg()).isEqualTo(Registration.StatusFedregEnum.PENDING_APPROVAL);
  }
}
