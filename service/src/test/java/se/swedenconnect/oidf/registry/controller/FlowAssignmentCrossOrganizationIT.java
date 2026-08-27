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
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.IntermediateFlowAssignmentDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces the cross-organization flow-assignment IDOR from the external vulnerability report ("Cross-organization
 * flow assignment manipulation"): {@code RegistrationFlowController}'s intermediate-assignment endpoints resolve an
 * {@code OrganizationRecord} for the caller but never pass it to the service layer, so
 * {@code RegistrationFlowService.getFlowAssignmentsForIntermediate/assignFlow/unassignFlow} look up the target
 * intermediate and flow by plain UUID with no ownership check at all — worse than the org-number collision in the
 * previous report, there isn't even an imprecise check here.
 * <p>
 * Alice and Charlie are different organizations on the same tenant (unlike the cross-instance report, this is a
 * same-tenant, different-org scenario, matching the report's own PoC). Each tests the SECURE behavior and therefore
 * currently FAILS — that failure documents the vulnerability. Once the lookups are scoped to the caller's own
 * {@code Organization.organizationId}, these tests should pass unmodified.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class FlowAssignmentCrossOrganizationIT {

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

  private RegistrationFlowApi aliceFlowApi;
  private RegistrationFlowApi charlieFlowApi;
  private UUID aliceTaImId;
  private UUID aliceFlowId;
  private UUID aliceBaselineAssignId;
  private UUID charlieTaImId;
  private UUID charlieFlowId;

  @BeforeEach
  void setUp() {
    final ApiClient aliceClient = buildApiClient(ALICE);
    final ApiClient charlieClient = buildApiClient(CHARLIE);
    this.aliceFlowApi = new RegistrationFlowApi(aliceClient);
    this.charlieFlowApi = new RegistrationFlowApi(charlieClient);

    this.aliceTaImId = createIntermediate(aliceClient, ALICE, "https://www.pm.se/oidf/ta/" + UUID.randomUUID());
    this.aliceFlowId = createFlow(this.aliceFlowApi, ALICE, "Alice's flow");
    this.aliceBaselineAssignId = this.aliceFlowApi.assignFlow(TENANT, ALICE.orgId, this.aliceTaImId,
        new AssignFlowRequest().flowId(this.aliceFlowId)).getAssignId();

    this.charlieTaImId = createIntermediate(charlieClient, CHARLIE,
        "https://registry.swedenconnect.se/oidf/" + CHARLIE.orgId + "/ta/" + UUID.randomUUID());
    this.charlieFlowId = createFlow(this.charlieFlowApi, CHARLIE, "Charlie's flow");
  }

  private ApiClient buildApiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private UUID createIntermediate(final ApiClient client, final JwtTestUtils.OrganisationType org,
      final String entityIdentifier) {
    final EntitiesApi entitiesApi = new EntitiesApi(client);
    final ModulesApi modulesApi = new ModulesApi(client);
    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, org.orgId,
        FederationEntity.builder().entityIdentifier(entityIdentifier).build());
    final TrustAnchor trustAnchor = modulesApi.createTrustAnchor(TENANT, org.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    return trustAnchor.getTrustAnchorId();
  }

  private UUID createFlow(final RegistrationFlowApi flowApi, final JwtTestUtils.OrganisationType org,
      final String name) {
    final RegistrationFlowDto flow = flowApi.createFlow(TENANT, org.orgId, new RegistrationFlowDto()
        .name(name)
        .description("Fixture flow for FlowAssignmentCrossOrganizationIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(UUID.randomUUID())
            .name("MinimalStep")
            .description("Test step"))));
    return flow.getFlowId();
  }

  @Test
  @DisplayName("getFlowAssignments must not disclose another organization's intermediate assignments — currently "
      + "FAILS: Charlie's path returns Alice's assignment metadata")
  void getFlowAssignmentsMustNotDiscloseAnotherOrgsAssignments() {
    final List<IntermediateFlowAssignmentDto> charliesView =
        this.charlieFlowApi.getFlowAssignments(TENANT, CHARLIE.orgId, this.aliceTaImId);

    assertThat(charliesView).extracting(IntermediateFlowAssignmentDto::getAssignId)
        .doesNotContain(this.aliceBaselineAssignId);
  }

  @Test
  @DisplayName("assignFlow must not attach a flow to another organization's intermediate — currently FAILS: "
      + "Charlie's path creates an assignment on Alice's intermediate")
  void assignFlowMustNotAttachToAnotherOrgsIntermediate() {
    assertThatThrownBy(() -> this.charlieFlowApi.assignFlow(TENANT, CHARLIE.orgId, this.aliceTaImId,
        new AssignFlowRequest().flowId(this.charlieFlowId)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
  }

  @Test
  @DisplayName("assignFlow must not attach another organization's flow to the caller's own intermediate — "
      + "currently FAILS: Charlie can assign Alice's flow to his own intermediate")
  void assignFlowMustNotAttachAnotherOrgsFlowToOwnIntermediate() {
    assertThatThrownBy(() -> this.charlieFlowApi.assignFlow(TENANT, CHARLIE.orgId, this.charlieTaImId,
        new AssignFlowRequest().flowId(this.aliceFlowId)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
  }

  @Test
  @DisplayName("unassignFlow must not delete another organization's assignment — currently FAILS: Charlie can "
      + "delete Alice's baseline assignment through his own path")
  void unassignFlowMustNotDeleteAnotherOrgsAssignment() {
    assertThatThrownBy(() -> this.charlieFlowApi.unassignFlow(TENANT, CHARLIE.orgId, this.aliceTaImId,
        this.aliceBaselineAssignId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));

    final List<IntermediateFlowAssignmentDto> aliceStillHasIt =
        this.aliceFlowApi.getFlowAssignments(TENANT, ALICE.orgId, this.aliceTaImId);
    assertThat(aliceStillHasIt).extracting(IntermediateFlowAssignmentDto::getAssignId)
        .contains(this.aliceBaselineAssignId);
  }
}
