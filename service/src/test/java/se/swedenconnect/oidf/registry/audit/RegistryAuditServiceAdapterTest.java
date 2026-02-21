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

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.AuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventType;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditLogger;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.policy.dto.PolicyDto;

import java.util.Stack;
import java.util.UUID;

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
  void policyCreated_shouldEmitEventWithCorrectAttributes() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };

    final UUID policyId = UUID.randomUUID();
    final UUID organizationId = UUID.randomUUID();
    final PolicyDto newData = new PolicyDto();
    newData.setPolicyId(policyId);
    newData.setName("Test Policy");
    auditService.policyCreated(policyId, organizationId, null, newData);
    assertEquals(stack.peek().getType(), RegistryAuditEventType.POLICY_CREATED.name());
    assertNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("extId"));
    assertEquals(policyId.toString(), stack.peek().getData().get("extId"));
    assertNotNull(stack.peek().getData().get("organizationId"));
    assertEquals(organizationId.toString(), stack.peek().getData().get("organizationId"));
  }

  @Test
  void policyUpdated_shouldEmitEventWithCorrectAttributes() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };

    final UUID policyId = UUID.randomUUID();
    final UUID organizationId = UUID.randomUUID();
    final PolicyDto oldData = new PolicyDto();
    oldData.setPolicyId(policyId);
    oldData.setName("Old Policy");
    final PolicyDto newData = new PolicyDto();
    newData.setPolicyId(policyId);
    newData.setName("New Policy");
    auditService.policyUpdated(policyId, organizationId, oldData, newData);
    assertEquals(stack.peek().getType(), RegistryAuditEventType.POLICY_UPDATED.name());
    assertNotNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("extId"));
    assertEquals(policyId.toString(), stack.peek().getData().get("extId"));
    assertNotNull(stack.peek().getData().get("organizationId"));
    assertEquals(organizationId.toString(), stack.peek().getData().get("organizationId"));
  }

  @Test
  void policyDeleted_shouldEmitEventWithCorrectAttributes() {
    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };

    final UUID policyId = UUID.randomUUID();
    final UUID organizationId = UUID.randomUUID();
    final PolicyDto deletedData = new PolicyDto();
    deletedData.setPolicyId(policyId);
    deletedData.setName("Deleted Policy");
    auditService.policyDeleted(policyId, organizationId, deletedData);
    assertEquals(stack.peek().getType(), RegistryAuditEventType.POLICY_DELETED.name());
    assertNotNull(stack.peek().getData().get("oldData"));
    assertNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("extId"));
    assertEquals(policyId.toString(), stack.peek().getData().get("extId"));
    assertNotNull(stack.peek().getData().get("organizationId"));
    assertEquals(organizationId.toString(), stack.peek().getData().get("organizationId"));
  }

}