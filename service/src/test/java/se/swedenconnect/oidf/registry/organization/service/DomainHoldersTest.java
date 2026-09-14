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
package se.swedenconnect.oidf.registry.organization.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure rule deciding whether a domain is already held by somebody else on the tenant.
 *
 * @author Felix Hellman
 */
class DomainHoldersTest {

  private static final UUID ASKING_ORGANIZATION = UUID.randomUUID();
  private static final UUID OTHER_ORGANIZATION = UUID.randomUUID();

  @Test
  @DisplayName("Nobody holding the domain is not a conflict")
  void noHoldersIsNotAConflict() {
    assertThat(DomainHolders.heldByAnotherOrganization(List.of(), ASKING_ORGANIZATION)).isFalse();
  }

  @Test
  @DisplayName("The organization's own claim does not stand in its own way")
  void ownClaimIsNotAConflict() {
    assertThat(DomainHolders.heldByAnotherOrganization(
        List.of(claim(ASKING_ORGANIZATION, DomainStatus.PENDING)), ASKING_ORGANIZATION)).isFalse();
    assertThat(DomainHolders.heldByAnotherOrganization(
        List.of(claim(ASKING_ORGANIZATION, DomainStatus.VALIDATED)), ASKING_ORGANIZATION)).isFalse();
  }

  @Test
  @DisplayName("Another organization's claim is a conflict")
  void anotherOrganizationsClaimIsAConflict() {
    assertThat(DomainHolders.heldByAnotherOrganization(
        List.of(claim(OTHER_ORGANIZATION, DomainStatus.PENDING)), ASKING_ORGANIZATION)).isTrue();
    assertThat(DomainHolders.heldByAnotherOrganization(
        List.of(claim(OTHER_ORGANIZATION, DomainStatus.VALIDATED)), ASKING_ORGANIZATION)).isTrue();
  }

  @Test
  @DisplayName("Another organization's claim is a conflict even next to the asking organization's own")
  void mixedHoldersAreAConflict() {
    assertThat(DomainHolders.heldByAnotherOrganization(
        List.of(claim(ASKING_ORGANIZATION, DomainStatus.PENDING), claim(OTHER_ORGANIZATION, DomainStatus.PENDING)),
        ASKING_ORGANIZATION)).isTrue();
  }

  @Test
  @DisplayName("Nothing is decided without an asking organization or a set of holders")
  void missingInputIsNotAConflict() {
    assertThat(DomainHolders.heldByAnotherOrganization(null, ASKING_ORGANIZATION)).isFalse();
    assertThat(DomainHolders.heldByAnotherOrganization(
        List.of(claim(OTHER_ORGANIZATION, DomainStatus.PENDING)), null)).isFalse();
  }

  @Test
  @DisplayName("The conflict message names the domain and is the same wherever it is reported")
  void conflictMessageNamesTheDomain() {
    assertThat(DomainHolders.alreadyHeldMessage("example.se"))
        .isEqualTo("Domain example.se is already held by another organization");
  }

  private static OrganizationDomain claim(final UUID organizationId, final DomainStatus status) {
    final Organization organization = new Organization();
    organization.setOrganizationId(organizationId);
    final OrganizationDomain domain = new OrganizationDomain();
    domain.setDomainId(UUID.randomUUID());
    domain.setOrganization(organization);
    domain.setDomain("example.se");
    domain.setStatus(status);
    return domain;
  }
}
