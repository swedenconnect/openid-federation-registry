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
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.AuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventType;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditLogger;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;

import java.net.URI;
import java.util.Stack;


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

    auditService.resolveJwks(URI.create("https://plars.org/papartner"));

    assertEquals(stack.peek().getType(), RegistryAuditEventType.RESOLVED_ENTITY_CONFIGURATION.name());
    assertNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("extId"));
    assertEquals("https://plars.org/papartner", stack.peek().getData().get("extId"));
  }



}