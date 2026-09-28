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
import se.swedenconnect.oidf.registry.organization.dto.AdminOrganizationDto;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the two pure rules behind the operator's organization listing: the order rows are shown in,
 * and the shape of the trust mark type lists handed out.
 */
class OrganizationAdminListingTest {

  private static AdminOrganizationDto organization(final String legalName, final String orgName,
      final String orgNumber) {
    return new AdminOrganizationDto(orgNumber, orgName, legalName, 0, 0, List.of());
  }

  private static List<String> orderedOrgNumbers(final AdminOrganizationDto... organizations) {
    return Arrays.stream(organizations)
        .sorted(RegistrationAdminServiceImpl.byDisplayName())
        .map(AdminOrganizationDto::orgNumber)
        .toList();
  }

  @Test
  @DisplayName("Organizations are ordered by legal name, case-insensitively")
  void organizationsAreOrderedByLegalNameIgnoringCase() {
    assertThat(orderedOrgNumbers(
        organization("zeta AB", "Zeta", "3"),
        organization("Alpha AB", "Alpha", "1"),
        organization("beta AB", "Beta", "2")))
        .containsExactly("1", "2", "3");
  }

  @Test
  @DisplayName("An organization without a legal name is ordered by its token-claim name instead")
  void organizationWithoutLegalNameFallsBackToOrgName() {
    assertThat(orderedOrgNumbers(
        organization("Charlie AB", "Charlie", "3"),
        organization(null, "Bravo", "2"),
        organization("  ", "Alpha", "1")))
        .as("A blank legal name is no name at all, so the token-claim name orders the row")
        .containsExactly("1", "2", "3");
  }

  @Test
  @DisplayName("An organization with no name at all is listed last, never dropped")
  void namelessOrganizationIsListedLast() {
    assertThat(orderedOrgNumbers(
        organization(null, null, "9"),
        organization("Alpha AB", null, "1")))
        .containsExactly("1", "9");
  }

  @Test
  @DisplayName("Organizations sharing a name are ordered by organization number, so the order is stable")
  void sameNameIsBrokenByOrgNumber() {
    assertThat(orderedOrgNumbers(
        organization("Same AB", null, "2"),
        organization("Same AB", null, "1")))
        .containsExactly("1", "2");
  }

  @Test
  @DisplayName("Trust mark types are de-duplicated, sorted, and stripped of blanks")
  void trustMarkTypesAreDistinctSortedAndNonBlank() {
    assertThat(RegistrationAdminServiceImpl.sortedDistinctTrustMarkTypes(Arrays.asList(
        "https://tm.example.com/tm/b",
        "https://tm.example.com/tm/a",
        "https://tm.example.com/tm/b",
        null,
        "   ")))
        .containsExactly("https://tm.example.com/tm/a", "https://tm.example.com/tm/b");
  }

  @Test
  @DisplayName("An instance issuing no trust marks yields an empty list, not null")
  void noTrustMarkTypesYieldsEmptyList() {
    assertThat(RegistrationAdminServiceImpl.sortedDistinctTrustMarkTypes(List.of())).isEmpty();
  }
}
