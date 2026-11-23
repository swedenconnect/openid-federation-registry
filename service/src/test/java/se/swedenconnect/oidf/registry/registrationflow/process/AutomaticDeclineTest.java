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

package se.swedenconnect.oidf.registry.registrationflow.process;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventType;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditLogger;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The registry's own decline of a registration says that the registry decided it, and names the account that
 * submitted the request.
 *
 * @author Per Fredrik Plars
 */
class AutomaticDeclineTest {

  @Test
  @DisplayName("A decline is audited as a rejection by the registry, naming the account that submitted the request")
  void declineNamesTheSubmittingAccount() {
    final List<FederationAuditEvent> events = new ArrayList<>();
    final AutomaticDecline automaticDecline = new AutomaticDecline(new RegistryAuditLogger() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        events.add(event);
      }
    });
    final Registration registration = registration("applicant-account");

    automaticDecline.decline(registration, "Automatically declined: an automated check failed.",
        "The step reported a failure");

    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(events).singleElement().satisfies(event -> {
      assertThat(event.getEvent()).isEqualTo(RegistryAuditEventType.REGISTRATION_REJECTED);
      assertThat(event.getPrincipal()).as("No user made the decision").isEqualTo("<Registry>");
      assertThat(event.getNewData()).contains("applicant-account");
    });
  }

  private static Registration registration(final String submittedBy) {
    final Instance instance = new Instance();
    instance.setInstanceId(UUID.randomUUID());
    final Organization organization = new Organization();
    organization.setOrganizationId(UUID.randomUUID());
    organization.setInstance(instance);
    final Registration registration = new Registration();
    registration.setRegistrationId(UUID.randomUUID());
    registration.setEntityId("https://www.example.org/oidf/rp");
    registration.setRegistrationType(RegistrationType.SUBORDINATE);
    registration.setOrganization(organization);
    registration.setCreatedBy(submittedBy);
    return registration;
  }
}
