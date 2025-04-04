/*
 * Copyright 2025 Sweden Connect
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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.AuditEvent;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;

import java.util.Stack;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegistryAuditServiceAdapterTest {

  /**
   * This class tests the `policyWrite` method in the `RegistryAuditServiceAdapter` class. The `policyWrite` method is
   * responsible for emitting an event indicating the creation or update of a policy record, including JSON
   * representations of the old and new policy records.
   */

  @Test
  void policyWrite_shouldEmitEventWithCorrectAttributes() throws JsonProcessingException {

    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };

    final OptionsRecord record = new OptionsRecord();
    auditService.optionsCreate(UUID.randomUUID(), FkKeyType.POLICIES, record, record);
    assertEquals(stack.peek().getType(), RegistryAuditEventType.OPTIONS_CREATED.name());
    assertNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("optionId"));
    stack.clear();
    final OptionsRecord record2 = new OptionsRecord();
    auditService.optionsUpdate(UUID.randomUUID(), FkKeyType.POLICIES, record2, record2);
    assertEquals(stack.peek().getType(), RegistryAuditEventType.OPTIONS_UPDATE.name());
    assertNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("optionId"));

  }

}