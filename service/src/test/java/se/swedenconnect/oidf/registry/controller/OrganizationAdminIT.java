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
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.OrganizationApi;
import se.swedenconnect.oidf.registry.api.RegistrationAdminApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.AdminOrganization;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.Domain;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Organization;
import se.swedenconnect.oidf.registry.api.model.PreValidatedTrustMarksRequest;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the tenant operator's organization administration: listing the organizations on its own tenant,
 * fetching one of them, reading back the trust mark types its federation issues, and pre-approving types for an
 * organization. An organization that owns no trust anchor is told none of it exists.
 *
 * @author Felix Hellman
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class OrganizationAdminIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType NOT_AN_OPERATOR = JwtTestUtils.OrganisationType.TESTORG2;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private RegistrationAdminApi operatorAdminApi;
  private String trustMarkType;

  @BeforeEach
  void setUp() {
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final EntitiesApi entitiesApi = new EntitiesApi(operatorClient);
    final ModulesApi modulesApi = new ModulesApi(operatorClient);

    // The tenant operator is recognised by owning a trust anchor on this tenant, so give PM one.
    final FederationEntity taEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    modulesApi.createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());

    // The trust mark type picker is fed from the trust marks the tenant's own issuers hold, so issue one.
    final String issuerSuffix = UUID.randomUUID().toString();
    final FederationEntity tmiEntity = entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/tmi/" + issuerSuffix).build());
    final TrustmarkIssuer trustmarkIssuer = modulesApi.createTrustmarkIssuer(TENANT, OPERATOR.orgId,
        new TrustmarkIssuer()
            .entityId(tmiEntity.getEntityId())
            .active(true)
            .trustMarkTokenValidityDuration("PT1H"));

    this.trustMarkType = "https://www.pm.se/oidf/tm/" + issuerSuffix;
    new TrustmarksApi(operatorClient).createTrustmark(TENANT, OPERATOR.orgId, new Trustmark()
        .trustmarkissuerId(trustmarkIssuer.getTrustmarkIssuerId())
        .trustmarkType(this.trustMarkType));

    this.operatorAdminApi = new RegistrationAdminApi(operatorClient);
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  private OrganizationApi bootstrappedOrganization(final JwtTestUtils.OrganisationType org) {
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(org));
    organizationApi.createOrganization(TENANT, org.orgId,
        new CreateOrganizationRequest().legalName(org.name + " AB"));
    return organizationApi;
  }

  private AdminOrganization findOrganization(final String orgNumber) {
    return this.operatorAdminApi.listOrganizations(TENANT, OPERATOR.orgId).stream()
        .filter(organization -> orgNumber.equals(organization.getOrgNumber()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Organization %s is not in the operator's list".formatted(orgNumber)));
  }

  private static int statusOf(final Throwable throwable) {
    return ((RestClientResponseException) throwable).getStatusCode().value();
  }

  @Test
  @DisplayName("The operator lists every organization on its tenant, with domain counts, ordered by name")
  void operatorListsOrganizationsOnItsTenant() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG1;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);
    organizationApi.requestDomain(TENANT, applicant.orgId, new DomainRequest().domain("listed.example.com"));

    final List<AdminOrganization> organizations = this.operatorAdminApi.listOrganizations(TENANT, OPERATOR.orgId);

    assertThat(organizations)
        .filteredOn(organization -> applicant.orgId.equals(organization.getOrgNumber()))
        .singleElement()
        .satisfies(organization -> {
          assertThat(organization.getLegalName()).isEqualTo(applicant.name + " AB");
          assertThat(organization.getOrgName()).isEqualTo(applicant.name);
          assertThat(organization.getDomainCount()).isEqualTo(1L);
          assertThat(organization.getPendingDomainCount()).isEqualTo(1L);
          assertThat(organization.getPreValidatedTrustMarks()).isEmpty();
        });

    assertThat(organizations)
        .as("The operator's own organization is on the tenant too, so it is listed")
        .anyMatch(organization -> OPERATOR.orgId.equals(organization.getOrgNumber()));

    final List<String> displayNames = organizations.stream()
        .map(organization -> organization.getLegalName() != null
            ? organization.getLegalName()
            : organization.getOrgName())
        .filter(Objects::nonNull)
        .toList();
    assertThat(displayNames)
        .as("The listing is ordered by the name the operator reads it under")
        .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
  }

  @Test
  @DisplayName("The operator fetches a single organization on its tenant, domains included")
  void operatorFetchesASingleOrganization() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG3;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);
    final Domain claimed = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("single.example.com"));

    final Organization fetched = this.operatorAdminApi.getOrganizationOnTenant(TENANT, OPERATOR.orgId,
        applicant.orgId);
    assertThat(fetched.getOrgNumber()).isEqualTo(applicant.orgId);
    assertThat(fetched.getLegalName()).isEqualTo(applicant.name + " AB");
    assertThat(fetched.getTenant()).isNotNull();
    assertThat(fetched.getDomains())
        .filteredOn(domain -> claimed.getDomainId().equals(domain.getDomainId()))
        .singleElement()
        .satisfies(domain -> assertThat(domain.getDomain()).isEqualTo("single.example.com"));

    assertThatThrownBy(() -> this.operatorAdminApi.getOrganizationOnTenant(TENANT, OPERATOR.orgId, "5520099999"))
        .as("An organization that is not on this instance is missing, not somebody else's")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }

  @Test
  @DisplayName("The trust mark types of the tenant's own issuers are offered to the operator, sorted")
  void trustMarkTypesComeFromTheTenantsOwnIssuers() {
    final List<String> types = this.operatorAdminApi.listTrustMarkTypes(TENANT, OPERATOR.orgId);

    assertThat(types).contains(this.trustMarkType);
    assertThat(types).doesNotHaveDuplicates();
    assertThat(types).isSorted();
  }

  @Test
  @DisplayName("The operator pre-approves trust mark types and sees them on the organization's row")
  void operatorPreValidatesTrustMarksAndSeesThemInTheListing() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG4;
    this.bootstrappedOrganization(applicant);

    final Organization updated = this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId,
        applicant.orgId, new PreValidatedTrustMarksRequest()
            .preValidatedTrustMarks(List.of(this.trustMarkType, "https://tm.example.com/tm/extra")));
    assertThat(updated.getPreValidatedTrustMarks())
        .containsExactlyInAnyOrder(this.trustMarkType, "https://tm.example.com/tm/extra");

    assertThat(this.findOrganization(applicant.orgId).getPreValidatedTrustMarks())
        .as("The listing the Organizations view renders carries the same pre-approvals")
        .containsExactlyInAnyOrder(this.trustMarkType, "https://tm.example.com/tm/extra");

    this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId, applicant.orgId,
        new PreValidatedTrustMarksRequest().preValidatedTrustMarks(List.of()));
    assertThat(this.findOrganization(applicant.orgId).getPreValidatedTrustMarks())
        .as("An empty list is how the operator withdraws every pre-approval")
        .isEmpty();
  }

  @Test
  @DisplayName("A blank, non-https or repeated trust mark type is rejected before anything is stored")
  void invalidTrustMarkEntryIsRejected() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG5;
    this.bootstrappedOrganization(applicant);
    this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId, applicant.orgId,
        new PreValidatedTrustMarksRequest().preValidatedTrustMarks(List.of(this.trustMarkType)));

    assertThat(Arrays.asList(
        List.of("   "),
        List.of("http://tm.example.com/tm/insecure"),
        List.of("not a uri"),
        List.of("https://tm.example.com/tm/dup", "https://tm.example.com/tm/dup")))
        .allSatisfy(invalid -> assertThatThrownBy(() ->
            this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId, applicant.orgId,
                new PreValidatedTrustMarksRequest().preValidatedTrustMarks(invalid)))
            .isInstanceOf(RestClientResponseException.class)
            .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(400)));

    assertThat(this.findOrganization(applicant.orgId).getPreValidatedTrustMarks())
        .as("A rejected request leaves the stored list untouched")
        .containsExactly(this.trustMarkType);
  }

  @Test
  @DisplayName("An organization owning no trust anchor sees no organization administration at all")
  void organizationWithoutATrustAnchorSeesNoOrganizationAdmin() {
    final RegistrationAdminApi outsiderApi = new RegistrationAdminApi(this.apiClient(NOT_AN_OPERATOR));

    assertThatThrownBy(() -> outsiderApi.listOrganizations(TENANT, NOT_AN_OPERATOR.orgId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
    assertThatThrownBy(() -> outsiderApi.getOrganizationOnTenant(TENANT, NOT_AN_OPERATOR.orgId,
        JwtTestUtils.OrganisationType.TESTORG1.orgId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
    assertThatThrownBy(() -> outsiderApi.listTrustMarkTypes(TENANT, NOT_AN_OPERATOR.orgId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }
}
