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
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestAuditDto;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.net.URI;
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
}
