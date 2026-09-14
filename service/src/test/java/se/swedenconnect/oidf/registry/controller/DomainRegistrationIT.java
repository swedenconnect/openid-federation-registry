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
import se.swedenconnect.oidf.registry.api.model.AdminDomain;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.Domain;
import se.swedenconnect.oidf.registry.api.model.DomainRejectionResult;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Organization;
import se.swedenconnect.oidf.registry.api.model.PreValidatedTrustMarksRequest;
import se.swedenconnect.oidf.registry.api.model.RejectRegistrationRequest;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the domain lifecycle end to end: an organization claims a domain, the tenant operator — an
 * organization owning a trust anchor on the tenant — reviews it, and an organization that owns no trust anchor
 * is told the review endpoints do not exist.
 *
 * @author Felix Hellman
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class DomainRegistrationIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType NOT_AN_OPERATOR = JwtTestUtils.OrganisationType.AF;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private RegistrationAdminApi operatorAdminApi;

  @BeforeEach
  void setUp() {
    // The tenant operator is recognised by owning a trust anchor on this tenant, so give PM one.
    final ApiClient operatorClient = this.apiClient(OPERATOR);
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT,
        OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
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

  private static int statusOf(final Throwable throwable) {
    return ((RestClientResponseException) throwable).getStatusCode().value();
  }

  private AdminDomain findDomain(final List<AdminDomain> domains, final UUID domainId) {
    return domains.stream()
        .filter(domain -> domainId.equals(domain.getDomainId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Domain %s is not in the operator's list".formatted(domainId)));
  }

  @Test
  @DisplayName("A claimed domain is pending until the operator approves it")
  void claimedDomainIsPendingUntilApproved() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG1;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);

    final Domain requested = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("Approved.Example.COM"));
    assertThat(requested.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);
    assertThat(requested.getDomain()).as("The stored domain is lower-cased").isEqualTo("approved.example.com");
    assertThat(requested.getReviewedAt()).isNull();

    final AdminDomain pending = this.findDomain(
        this.operatorAdminApi.listDomains1(TENANT, OPERATOR.orgId, "PENDING"), requested.getDomainId());
    assertThat(pending.getOrgNumber()).isEqualTo(applicant.orgId);
    assertThat(pending.getLegalName()).isEqualTo(applicant.name + " AB");

    final AdminDomain approved = this.operatorAdminApi.approveDomain(TENANT, OPERATOR.orgId,
        requested.getDomainId());
    assertThat(approved.getStatus()).isEqualTo(AdminDomain.StatusEnum.VALIDATED);
    assertThat(approved.getReviewedAt()).isNotNull();

    assertThat(organizationApi.listDomains(TENANT, applicant.orgId))
        .filteredOn(domain -> requested.getDomainId().equals(domain.getDomainId()))
        .singleElement()
        .satisfies(domain -> assertThat(domain.getStatus()).isEqualTo(Domain.StatusEnum.VALIDATED));

    assertThatThrownBy(() -> this.operatorAdminApi.approveDomain(TENANT, OPERATOR.orgId,
        requested.getDomainId()))
        .as("A domain that has already been reviewed cannot be reviewed again")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409));
  }

  @Test
  @DisplayName("Claiming the same domain twice is a conflict")
  void claimingTheSameDomainTwiceIsAConflict() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG2;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);

    organizationApi.requestDomain(TENANT, applicant.orgId, new DomainRequest().domain("dup.example.com"));
    assertThatThrownBy(() -> organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("dup.example.com")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409));
  }

  @Test
  @DisplayName("A rejected domain can be claimed again, which re-opens it as pending")
  void rejectedDomainCanBeClaimedAgain() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG3;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);

    final Domain requested = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("rejected.example.com"));

    final DomainRejectionResult rejected = this.operatorAdminApi.rejectDomain(TENANT, OPERATOR.orgId,
        requested.getDomainId(), new RejectRegistrationRequest().rejectionReason("Not yours"));
    assertThat(rejected.getStatus()).isEqualTo(DomainRejectionResult.StatusEnum.REJECTED);
    assertThat(rejected.getRejectionReason()).isEqualTo("Not yours");
    assertThat(rejected.getCascadedRegistrationIds()).isEmpty();

    final Domain reopened = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("rejected.example.com"));
    assertThat(reopened.getDomainId()).isEqualTo(requested.getDomainId());
    assertThat(reopened.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);
    assertThat(reopened.getRejectionReason()).isNull();
    assertThat(reopened.getReviewedAt()).isNull();
  }

  @Test
  @DisplayName("A claimed domain can be withdrawn by the organization that claimed it")
  void claimedDomainCanBeWithdrawn() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG4;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);

    final Domain requested = organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("withdrawn.example.com"));
    assertThat(this.operatorAdminApi.countPendingDomains(TENANT, OPERATOR.orgId).get("count"))
        .isNotNull()
        .isGreaterThan(0L);

    organizationApi.deleteDomain(TENANT, applicant.orgId, requested.getDomainId());
    assertThat(organizationApi.listDomains(TENANT, applicant.orgId))
        .noneMatch(domain -> requested.getDomainId().equals(domain.getDomainId()));

    assertThatThrownBy(() -> organizationApi.deleteDomain(TENANT, applicant.orgId, requested.getDomainId()))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }

  @Test
  @DisplayName("An invalid domain is rejected before it is stored")
  void invalidDomainIsRejected() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG5;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);

    assertThatThrownBy(() -> organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("https://example.com/path")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(400));
  }

  @Test
  @DisplayName("Claiming a domain before bootstrapping the organization is a 404")
  void claimingADomainBeforeBootstrapIsNotFound() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG6;
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(applicant));

    assertThatThrownBy(() -> organizationApi.requestDomain(TENANT, applicant.orgId,
        new DomainRequest().domain("early.example.com")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));

    assertThatThrownBy(() -> organizationApi.listDomains(TENANT, applicant.orgId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }

  @Test
  @DisplayName("An organization owning no trust anchor sees no domain review at all")
  void organizationWithoutATrustAnchorSeesNoDomainReview() {
    final RegistrationAdminApi outsiderApi = new RegistrationAdminApi(this.apiClient(NOT_AN_OPERATOR));
    final UUID someDomainId = UUID.randomUUID();

    assertThatThrownBy(() -> outsiderApi.listDomains1(TENANT, NOT_AN_OPERATOR.orgId, null))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
    assertThatThrownBy(() -> outsiderApi.countPendingDomains(TENANT, NOT_AN_OPERATOR.orgId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
    assertThatThrownBy(() -> outsiderApi.approveDomain(TENANT, NOT_AN_OPERATOR.orgId, someDomainId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
    assertThatThrownBy(() -> outsiderApi.rejectDomain(TENANT, NOT_AN_OPERATOR.orgId, someDomainId,
        new RejectRegistrationRequest().rejectionReason("nope")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
    assertThatThrownBy(() -> outsiderApi.replacePreValidatedTrustMarks(TENANT, NOT_AN_OPERATOR.orgId,
        JwtTestUtils.OrganisationType.TESTORG1.orgId,
        new PreValidatedTrustMarksRequest().preValidatedTrustMarks(List.of("https://tm.example.com/tm/x"))))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }

  @Test
  @DisplayName("The operator replaces an organization's pre-validated trust marks")
  void operatorReplacesPreValidatedTrustMarks() {
    final JwtTestUtils.OrganisationType applicant = JwtTestUtils.OrganisationType.TESTORG7;
    final OrganizationApi organizationApi = this.bootstrappedOrganization(applicant);

    final Organization withTrustMarks = this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT,
        OPERATOR.orgId, applicant.orgId, new PreValidatedTrustMarksRequest()
            .preValidatedTrustMarks(List.of("https://tm.example.com/tm/a", "https://tm.example.com/tm/b")));
    assertThat(withTrustMarks.getPreValidatedTrustMarks())
        .containsExactlyInAnyOrder("https://tm.example.com/tm/a", "https://tm.example.com/tm/b");

    assertThat(organizationApi.getOrganization(TENANT, applicant.orgId).getPreValidatedTrustMarks())
        .containsExactlyInAnyOrder("https://tm.example.com/tm/a", "https://tm.example.com/tm/b");

    final Organization replaced = this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId,
        applicant.orgId, new PreValidatedTrustMarksRequest()
            .preValidatedTrustMarks(List.of("https://tm.example.com/tm/c")));
    assertThat(replaced.getPreValidatedTrustMarks())
        .as("The list is replaced wholesale, not merged")
        .containsExactly("https://tm.example.com/tm/c");

    assertThatThrownBy(() -> this.operatorAdminApi.replacePreValidatedTrustMarks(TENANT, OPERATOR.orgId,
        "5520099999", new PreValidatedTrustMarksRequest().preValidatedTrustMarks(List.of())))
        .as("An organization that has not bootstrapped has nothing to pre-approve")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }
}
