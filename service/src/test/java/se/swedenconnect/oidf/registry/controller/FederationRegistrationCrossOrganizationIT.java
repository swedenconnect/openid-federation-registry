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
import se.swedenconnect.oidf.registry.api.FederationRegistrationApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.AssignFlowResponse;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Registration;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.organization.model.Organization;
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
 * Reproduces the cross-organization applicant-registration IDOR from the external vulnerability report
 * ("Cross-organization registration disclosure and deletion"): {@code FederationRegistrationController.getById} and
 * {@code deleteJoin} authorize Charlie against his own {@code {tenant}/{orgNumber}} path, then hand the caller-supplied
 * {@code registrationId} straight to {@code RegistrationServiceImpl}, which historically resolved it with a plain,
 * unscoped {@code registrationRepository.findById(...)} — letting Charlie read or delete Alice's registration by GUID
 * alone, without any right on Alice's organization.
 * <p>
 * As of this test, {@code RegistrationServiceImpl} already routes both operations through
 * {@code findOwnedRegistrationOrThrow}, which resolves the caller's {@code Organization.organizationId} and looks up
 * the registration via {@code findByRegistrationIdAndOrganization_OrganizationId(...)} — the fix landed as part of the
 * broader organizationId-scoping change (commit 7b01505) before this report was filed. These tests assert the SECURE
 * behavior and are expected to pass; they exist to give this specific report's PoC (GET and DELETE on the applicant
 * {@code /registration/v1} controller, as opposed to the operator-facing {@code /registration-admin/v1} controller
 * covered by {@link RegistrationAdminCrossOrganizationIT}) permanent regression coverage.
 * <p>
 * Alice's registration is inserted directly via {@link RegistrationRepository}, matching
 * {@code RegistrationAdminCrossOrganizationIT}'s approach — the registration pipeline itself isn't under test here,
 * only the ownership check on lookup.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class FederationRegistrationCrossOrganizationIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType ALICE = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType CHARLIE = JwtTestUtils.OrganisationType.AF;

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

  private FederationRegistrationApi aliceRegistrationApi;
  private FederationRegistrationApi charlieRegistrationApi;
  private FlowAssignment aliceFlowAssignment;
  private Organization aliceOrganization;

  @BeforeEach
  void setUp() {
    final ApiClient aliceClient = buildApiClient(ALICE);
    this.aliceRegistrationApi = new FederationRegistrationApi(aliceClient);
    this.charlieRegistrationApi = new FederationRegistrationApi(buildApiClient(CHARLIE));

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
        .name("Cross-org applicant IDOR fixture flow")
        .description("Fixture flow for FederationRegistrationCrossOrganizationIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(UUID.randomUUID())
            .name("MinimalStep")
            .description("Test step"))));
    final AssignFlowResponse assignment = flowApi.assignFlow(TENANT, ALICE.orgId, trustAnchor.getTrustAnchorId(),
        new AssignFlowRequest().flowId(flow.getFlowId()));

    // Fetched directly through the repositories (not the REST layer) so the fixture registration
    // below can be persisted straight to the database, matching the report's "STARTED, no dependent
    // objects" PoC fixture.
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

  private se.swedenconnect.oidf.registry.registrations.model.Registration aliceStartedRegistration(
      final String entityId) {
    final se.swedenconnect.oidf.registry.registrations.model.Registration registration =
        se.swedenconnect.oidf.registry.registrations.model.Registration.builder()
            .registrationId(UUID.randomUUID())
            .flowAssignment(this.aliceFlowAssignment)
            .organization(this.aliceOrganization)
            .entityId(entityId)
            .registrationType(RegistrationType.SUBORDINATE)
            .status(RegistrationStatus.STARTED)
            .pendingStepIndex(0)
            .stepResults(List.of())
            .build();
    return this.registrationRepository.save(registration);
  }

  @Test
  @DisplayName("getById must not disclose another organization's registration — Charlie's own valid org path plus "
      + "Alice's registration UUID must 404, not return her data")
  void getByIdMustNotDiscloseAnotherOrganizationsRegistration() {
    final UUID aliceRegistrationId =
        this.aliceStartedRegistration("https://alice.example/sp/get").getRegistrationId();

    // Owner control: Alice can read her own registration.
    assertThat(this.aliceRegistrationApi.getById(TENANT, ALICE.orgId, aliceRegistrationId).getRegistrationId())
        .isEqualTo(aliceRegistrationId);

    // Exploit attempt: Charlie's own authorized path, Alice's UUID.
    assertThatThrownBy(() -> this.charlieRegistrationApi.getById(TENANT, CHARLIE.orgId, aliceRegistrationId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
  }

  @Test
  @DisplayName("deleteJoin must not delete another organization's registration — Charlie's own valid org path plus "
      + "Alice's registration UUID must 404, and Alice's registration must remain intact")
  void deleteJoinMustNotDeleteAnotherOrganizationsRegistration() {
    final UUID aliceRegistrationId =
        this.aliceStartedRegistration("https://alice.example/sp/delete").getRegistrationId();

    assertThatThrownBy(() -> this.charlieRegistrationApi.deleteJoin(TENANT, CHARLIE.orgId, aliceRegistrationId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));

    final Registration stillOwnedByAlice = this.aliceRegistrationApi.getById(TENANT, ALICE.orgId, aliceRegistrationId);
    assertThat(stillOwnedByAlice.getRegistrationId()).isEqualTo(aliceRegistrationId);
    assertThat(this.registrationRepository.findById(aliceRegistrationId)).isPresent();
  }
}
