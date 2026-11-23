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

package se.swedenconnect.oidf.registry.audit;

import com.nimbusds.oauth2.sdk.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.AuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventType;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditLogger;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.registrationflow.dto.FlowAssignmentAuditDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.Technology;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestAuditDto;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.net.URI;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegistryAuditServiceAdapterTest {

  /**
   * This class tests the audit event methods in the `RegistryAuditServiceAdapter` class. The methods are
   * responsible for emitting events indicating the creation, update, or deletion of various registry entities,
   * including JSON representations of the old and new data.
   */

  @Test
  void jwksCreated_shouldEmitEventWithCorrectAttributes() throws ParseException {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };

    auditService.resolveJwks(URI.create("https://digg.se/papartner"));

    assertEquals(stack.peek().getType(), RegistryAuditEventType.RESOLVED_ENTITY_CONFIGURATION.name());
    assertNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("extId"));
    assertEquals("https://digg.se/papartner", stack.peek().getData().get("extId"));
  }

  @Test
  @DisplayName("A submitted registration request is audited with the request as the new data")
  void registrationSubmitted_shouldEmitEventWithTheRequest() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID registrationId = UUID.randomUUID();

    auditService.registrationSubmitted(registrationId, UUID.randomUUID(), UUID.randomUUID(),
        request(registrationId, RegistrationStatus.PENDING_APPROVAL));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_SUBMITTED.name());
    assertThat(stack.peek().getData().get("extId")).isEqualTo(registrationId.toString());
    assertThat(stack.peek().getData().get("oldData")).isNull();
    assertThat((String) stack.peek().getData().get("newData"))
        .contains("https://www.example.org/oidf/rp", "PENDING_APPROVAL");
  }

  @Test
  @DisplayName("A resubmitted registration request is audited with both the old and the new request")
  void registrationUpdated_shouldEmitEventWithBothStates() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID registrationId = UUID.randomUUID();

    auditService.registrationUpdated(registrationId, UUID.randomUUID(), UUID.randomUUID(),
        request(registrationId, RegistrationStatus.REJECTED),
        request(registrationId, RegistrationStatus.PENDING_APPROVAL));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_UPDATED.name());
    assertThat((String) stack.peek().getData().get("oldData")).contains("REJECTED");
    assertThat((String) stack.peek().getData().get("newData")).contains("PENDING_APPROVAL");
  }

  @Test
  @DisplayName("A deleted registration request is audited with the request that was deleted")
  void registrationDeleted_shouldEmitEventWithTheDeletedRequest() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID registrationId = UUID.randomUUID();

    auditService.registrationDeleted(registrationId, UUID.randomUUID(), UUID.randomUUID(),
        request(registrationId, RegistrationStatus.APPROVED));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_DELETED.name());
    assertThat((String) stack.peek().getData().get("oldData")).contains("applicant-account", "APPROVED");
    assertThat(stack.peek().getData().get("newData")).isNull();
  }

  @Test
  @DisplayName("A created registration flow is audited with the flow as the new data")
  void registrationFlowCreated_shouldEmitEventWithTheFlow() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID flowId = UUID.randomUUID();

    auditService.registrationFlowCreated(flowId, UUID.randomUUID(), UUID.randomUUID(), null, flow(flowId, "A flow"));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_FLOW_CREATED.name());
    assertThat(stack.peek().getData().get("extId")).isEqualTo(flowId.toString());
    assertThat(stack.peek().getData().get("oldData")).isNull();
    assertThat((String) stack.peek().getData().get("newData")).contains("A flow");
  }

  @Test
  @DisplayName("An updated registration flow is audited with both the old and the new flow")
  void registrationFlowUpdated_shouldEmitEventWithBothStates() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID flowId = UUID.randomUUID();

    auditService.registrationFlowUpdated(flowId, UUID.randomUUID(), UUID.randomUUID(), flow(flowId, "Before"),
        flow(flowId, "After"));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_FLOW_UPDATED.name());
    assertThat((String) stack.peek().getData().get("oldData")).contains("Before");
    assertThat((String) stack.peek().getData().get("newData")).contains("After");
  }

  @Test
  @DisplayName("A deleted registration flow is audited with the flow that was deleted")
  void registrationFlowDeleted_shouldEmitEventWithTheDeletedFlow() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID flowId = UUID.randomUUID();

    auditService.registrationFlowDeleted(flowId, UUID.randomUUID(), UUID.randomUUID(), flow(flowId, "Deleted"));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_FLOW_DELETED.name());
    assertThat((String) stack.peek().getData().get("oldData")).contains("Deleted");
    assertThat(stack.peek().getData().get("newData")).isNull();
  }

  @Test
  @DisplayName("An assignment is audited with the assignment id, the flow and the target it was assigned to")
  void registrationFlowAssigned_shouldEmitEventWithTheTarget() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID assignId = UUID.randomUUID();
    final UUID flowId = UUID.randomUUID();
    final UUID taImId = UUID.randomUUID();

    auditService.registrationFlowAssigned(assignId, UUID.randomUUID(), UUID.randomUUID(), null,
        new FlowAssignmentAuditDto(assignId, flowId, "A flow",
            FlowAssignmentAuditDto.TargetType.INTERMEDIATE, taImId));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_FLOW_ASSIGNED.name());
    assertThat(stack.peek().getData().get("extId")).isEqualTo(assignId.toString());
    assertThat((String) stack.peek().getData().get("newData"))
        .contains(flowId.toString(), taImId.toString(), "INTERMEDIATE");
  }

  @Test
  @DisplayName("A removed assignment is audited with the assignment that was removed")
  void registrationFlowUnassigned_shouldEmitEventWithTheRemovedAssignment() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = capturing(stack);
    final UUID assignId = UUID.randomUUID();
    final UUID trustmarkId = UUID.randomUUID();

    auditService.registrationFlowUnassigned(assignId, UUID.randomUUID(), UUID.randomUUID(),
        new FlowAssignmentAuditDto(assignId, UUID.randomUUID(), "A flow",
            FlowAssignmentAuditDto.TargetType.TRUSTMARK, trustmarkId));

    assertThat(stack.peek().getType()).isEqualTo(RegistryAuditEventType.REGISTRATION_FLOW_UNASSIGNED.name());
    assertThat((String) stack.peek().getData().get("oldData")).contains(trustmarkId.toString(), "TRUSTMARK");
    assertThat(stack.peek().getData().get("newData")).isNull();
  }

  private static RegistryAuditService capturing(final Stack<AuditEvent> stack) {
    return new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };
  }

  private static RegistrationRequestAuditDto request(final UUID registrationId, final RegistrationStatus status) {
    return new RegistrationRequestAuditDto(registrationId, "https://www.example.org/oidf/rp",
        RegistrationType.SUBORDINATE, status, UUID.randomUUID(), "applicant-account");
  }

  private static RegistrationFlowDto flow(final UUID flowId, final String name) {
    return new RegistrationFlowDto(flowId, name, "A description", null, Technology.OIDC, "openid_relying_party",
        List.of(), Step.FlowType.INTERMEDIATE, true);
  }
}
