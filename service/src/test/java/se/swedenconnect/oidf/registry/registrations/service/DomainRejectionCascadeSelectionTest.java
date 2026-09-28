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
package se.swedenconnect.oidf.registry.registrations.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the two pure rules behind domain review: which registrations a domain rejection carries with
 * it, and which domain statuses may still be reviewed.
 */
class DomainRejectionCascadeSelectionTest {

  private static final String REJECTED_DOMAIN = "example.com";

  private static Registration registration(final String entityId, final RegistrationStatus status,
      final RegistrationType type) {
    return Registration.builder()
        .registrationId(UUID.randomUUID())
        .entityId(entityId)
        .status(status)
        .registrationType(type)
        .build();
  }

  private static boolean selected(final Registration registration, final List<String> remainingDomains) {
    return RegistrationAdminServiceImpl.isCascadeTarget(registration, REJECTED_DOMAIN, remainingDomains);
  }

  @Test
  @DisplayName("An in-flight registration under the rejected domain is carried along")
  void inFlightRegistrationUnderRejectedDomainIsSelected() {
    assertThat(selected(registration("https://sp.example.com/oidf",
        RegistrationStatus.PENDING_APPROVAL, RegistrationType.SUBORDINATE), List.of())).isTrue();
    assertThat(selected(registration("https://example.com/oidf",
        RegistrationStatus.STARTED, RegistrationType.SUBORDINATE), List.of())).isTrue();
  }

  @Test
  @DisplayName("A registration also covered by another registered domain is left alone")
  void registrationCoveredByAnotherDomainIsLeftAlone() {
    assertThat(selected(registration("https://sp.other.com/oidf",
        RegistrationStatus.PENDING_APPROVAL, RegistrationType.SUBORDINATE), List.of("other.com"))).isFalse();
  }

  @Test
  @DisplayName("A registration under an unrelated host is left alone")
  void registrationUnderUnrelatedHostIsLeftAlone() {
    assertThat(selected(registration("https://sp.notexample.com/oidf",
        RegistrationStatus.PENDING_APPROVAL, RegistrationType.SUBORDINATE), List.of())).isFalse();
  }

  @Test
  @DisplayName("A settled registration is left alone")
  void settledRegistrationIsLeftAlone() {
    assertThat(selected(registration("https://sp.example.com/oidf",
        RegistrationStatus.APPROVED, RegistrationType.SUBORDINATE), List.of())).isFalse();
    assertThat(selected(registration("https://sp.example.com/oidf",
        RegistrationStatus.REJECTED, RegistrationType.SUBORDINATE), List.of())).isFalse();
  }

  @Test
  @DisplayName("A trust mark subordinate is never selected on its own — it follows its parent")
  void trustMarkSubordinateIsNeverSelectedDirectly() {
    assertThat(selected(registration("https://sp.example.com/oidf",
        RegistrationStatus.PENDING_APPROVAL, RegistrationType.TRUST_MARK_SUBORDINATE), List.of())).isFalse();
  }

  @Test
  @DisplayName("A registration whose entity identifier has no host is left alone")
  void registrationWithoutHostIsLeftAlone() {
    assertThat(selected(registration("urn:example:sp",
        RegistrationStatus.PENDING_APPROVAL, RegistrationType.SUBORDINATE), List.of())).isFalse();
  }

  @Test
  @DisplayName("Only a pending domain may be reviewed")
  void onlyPendingDomainsMayBeReviewed() {
    assertThatCode(() -> RegistrationAdminServiceImpl.requirePending(domain(DomainStatus.PENDING)))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> RegistrationAdminServiceImpl.requirePending(domain(DomainStatus.VALIDATED)))
        .isInstanceOf(RegistryServerException.class)
        .hasMessageContaining("not pending review");
    assertThatThrownBy(() -> RegistrationAdminServiceImpl.requirePending(domain(DomainStatus.REJECTED)))
        .isInstanceOf(RegistryServerException.class)
        .hasMessageContaining("not pending review");
  }

  private static OrganizationDomain domain(final DomainStatus status) {
    final OrganizationDomain domain = new OrganizationDomain();
    domain.setDomainId(UUID.randomUUID());
    domain.setDomain(REJECTED_DOMAIN);
    domain.setStatus(status);
    return domain;
  }
}
