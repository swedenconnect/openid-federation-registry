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
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.AssignFlowRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Intermediate;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that a change to a registration flow, and to the assignment of a flow, leaves an audit record.
 * The steps follow the AUD-04 case of the registry's end-to-end suite: create a flow, assign it, unassign it and
 * delete it.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class RegistrationFlowAuditIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType PM = JwtTestUtils.OrganisationType.PM;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private AuditEventRepository auditEventRepository;

  private RegistrationFlowApi flowApi;
  private EntitiesApi entitiesApi;
  private ModulesApi modulesApi;
  private TrustmarksApi trustmarksApi;

  @BeforeEach
  void setUp() {
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost:" + this.port);
    apiClient.setBearerToken(this.jwtTestUtils.createJwt(PM));
    this.flowApi = new RegistrationFlowApi(apiClient);
    this.entitiesApi = new EntitiesApi(apiClient);
    this.modulesApi = new ModulesApi(apiClient);
    this.trustmarksApi = new TrustmarksApi(apiClient);
  }

  @Test
  @DisplayName("Creating a registration flow is audited with the flow and the account")
  void creatingAFlowIsAudited() {
    final UUID flowId = this.createFlow("Audited flow");

    final AuditEvent event = this.event("REGISTRATION_FLOW_CREATED", flowId);
    assertThat(event.getPrincipal()).isNotBlank().isNotEqualTo("<NoActiveUserSet>");
    assertThat((String) event.getData().get("newData")).contains("Audited flow");
    assertThat(event.getData()).containsKeys("organizationId", "instanceId");
  }

  @Test
  @DisplayName("Updating a registration flow is audited with both the old and the new flow")
  void updatingAFlowIsAudited() {
    final UUID flowId = this.createFlow("Flow before the change");

    this.flowApi.updateFlow(TENANT, PM.orgId, flowId, flowDefinition("Flow after the change"));

    final AuditEvent event = this.event("REGISTRATION_FLOW_UPDATED", flowId);
    assertThat((String) event.getData().get("oldData")).contains("Flow before the change");
    assertThat((String) event.getData().get("newData")).contains("Flow after the change");
  }

  @Test
  @DisplayName("Deleting a registration flow is audited with the flow that was deleted")
  void deletingAFlowIsAudited() {
    final UUID flowId = this.createFlow("Flow to delete");

    this.flowApi.deleteFlow(TENANT, PM.orgId, flowId);

    final AuditEvent event = this.event("REGISTRATION_FLOW_DELETED", flowId);
    assertThat((String) event.getData().get("oldData")).contains("Flow to delete");
  }

  @Test
  @DisplayName("Assigning a flow to an intermediate, and removing the assignment, is audited")
  void assigningToAnIntermediateIsAudited() {
    final UUID taImId = this.createIntermediate();
    final UUID flowId = this.createFlow("Assigned flow");

    final UUID assignId = this.flowApi.assignFlow(TENANT, PM.orgId, taImId,
        new AssignFlowRequest().flowId(flowId)).getAssignId();

    final AuditEvent assigned = this.event("REGISTRATION_FLOW_ASSIGNED", assignId);
    assertThat((String) assigned.getData().get("newData"))
        .contains(flowId.toString(), taImId.toString(), "INTERMEDIATE");

    this.flowApi.unassignFlow(TENANT, PM.orgId, taImId, assignId);

    final AuditEvent unassigned = this.event("REGISTRATION_FLOW_UNASSIGNED", assignId);
    assertThat((String) unassigned.getData().get("oldData")).contains(flowId.toString(), taImId.toString());
  }

  @Test
  @DisplayName("Assigning the same flow twice leaves one record, because nothing changed the second time")
  void assigningTheSameFlowTwiceIsAuditedOnce() {
    final UUID taImId = this.createIntermediate();
    final UUID flowId = this.createFlow("Flow assigned twice");
    final AssignFlowRequest request = new AssignFlowRequest().flowId(flowId);

    final UUID assignId = this.flowApi.assignFlow(TENANT, PM.orgId, taImId, request).getAssignId();
    this.flowApi.assignFlow(TENANT, PM.orgId, taImId, request);

    assertThat(this.events("REGISTRATION_FLOW_ASSIGNED", assignId)).hasSize(1);
  }

  @Test
  @DisplayName("Assigning a flow to a trust mark issuer, and to a trust mark, is audited with its target")
  void assigningToTrustMarkIssuerAndTrustMarkIsAudited() {
    final UUID entityId = this.createEntity("https://www.pm.se/oidf/tmi/" + UUID.randomUUID());
    final UUID issuerId = this.modulesApi.createTrustmarkIssuerWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new TrustmarkIssuer().entityId(entityId).active(true)).getTrustmarkIssuerId();
    final UUID trustmarkId = UUID.randomUUID();
    this.trustmarksApi.createTrustmarkWithId(TENANT, PM.orgId, trustmarkId, new Trustmark()
        .trustmarkissuerId(issuerId).trustmarkType("https://www.pm.se/oidf/loa/" + UUID.randomUUID()));
    final UUID issuerFlowId = this.createFlow("Trust mark issuer flow");
    final UUID trustmarkFlowId = this.createFlow("Trust mark flow");

    final UUID issuerAssignId = this.flowApi.assignFlowToTrustMarkIssuer(TENANT, PM.orgId, issuerId,
        new AssignFlowRequest().flowId(issuerFlowId)).getAssignId();
    final UUID trustmarkAssignId = this.flowApi.assignFlowToTrustMark(TENANT, PM.orgId, trustmarkId,
        new AssignFlowRequest().flowId(trustmarkFlowId)).getAssignId();

    assertThat((String) this.event("REGISTRATION_FLOW_ASSIGNED", issuerAssignId).getData().get("newData"))
        .contains(issuerId.toString(), "TRUSTMARK_ISSUER");
    assertThat((String) this.event("REGISTRATION_FLOW_ASSIGNED", trustmarkAssignId).getData().get("newData"))
        .contains(trustmarkId.toString(), "TRUSTMARK");

    this.flowApi.unassignFlowFromTrustMarkIssuer(TENANT, PM.orgId, issuerId, issuerAssignId);
    this.flowApi.unassignFlowFromTrustMark(TENANT, PM.orgId, trustmarkId, trustmarkAssignId);

    assertThat(this.events("REGISTRATION_FLOW_UNASSIGNED", issuerAssignId)).hasSize(1);
    assertThat(this.events("REGISTRATION_FLOW_UNASSIGNED", trustmarkAssignId)).hasSize(1);
  }

  private AuditEvent event(final String type, final UUID extId) {
    final List<AuditEvent> found = this.events(type, extId);
    assertThat(found).as("%s for %s", type, extId).hasSize(1);
    return found.getFirst();
  }

  private List<AuditEvent> events(final String type, final UUID extId) {
    return this.auditEventRepository.find(null, null, type).stream()
        .filter(event -> extId.toString().equals(event.getData().get("extId")))
        .toList();
  }

  private UUID createFlow(final String name) {
    return this.flowApi.createFlow(TENANT, PM.orgId, flowDefinition(name)).getFlowId();
  }

  private static RegistrationFlowDto flowDefinition(final String name) {
    return new RegistrationFlowDto()
        .name(name)
        .description("Fixture flow for RegistrationFlowAuditIT")
        .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
        .steps(List.of(new StepDto()
            .stepId(UUID.randomUUID())
            .name("MinimalStep")
            .description("Test step")));
  }

  private UUID createIntermediate() {
    final UUID entityId = this.createEntity("https://www.pm.se/oidf/im/" + UUID.randomUUID());
    return this.modulesApi.createIntermediateWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new Intermediate().entityId(entityId).active(true)).getIntermediateId();
  }

  private UUID createEntity(final String entityIdentifier) {
    final UUID entityId = UUID.randomUUID();
    this.entitiesApi.createFederationEntityWithId(TENANT, PM.orgId, entityId,
        new FederationEntity().entityIdentifier(entityIdentifier));
    return entityId;
  }
}
