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
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.Domain;
import se.swedenconnect.oidf.registry.api.model.DomainRequest;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.RejectRegistrationRequest;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the rule that only one organization on a tenant holds a given domain at a time: a second claimant is
 * refused, a subdomain is a different domain and stays free, a rejected claim releases the domain for somebody
 * else, and the tenant boundary keeps two federations from competing over the same hostname.
 *
 * @author Felix Hellman
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class DomainUniquenessIT {

  private static final String TENANT = "Swedenconnect";
  private static final String OTHER_TENANT = "ENA";
  private static final String OTHER_TENANT_FUNCTION_GROUP = "ena";
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final String ALREADY_HELD = "is already held by another organization";

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
    final ApiClient operatorClient = this.apiClient(OPERATOR, "swedenconnect");
    final FederationEntity taEntity = new EntitiesApi(operatorClient).createFederationEntity(TENANT,
        OPERATOR.orgId,
        FederationEntity.builder().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()).build());
    new ModulesApi(operatorClient).createTrustAnchor(TENANT, OPERATOR.orgId,
        TrustAnchor.builder().entityId(taEntity.getEntityId()).active(true).build());
    this.operatorAdminApi = new RegistrationAdminApi(operatorClient);
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType org, final String functionGroup) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org, functionGroup, "admin"));
    return client;
  }

  private OrganizationApi bootstrappedOrganization(final JwtTestUtils.OrganisationType org, final String tenant,
      final String functionGroup) {
    final OrganizationApi organizationApi = new OrganizationApi(this.apiClient(org, functionGroup));
    try {
      organizationApi.createOrganization(tenant, org.orgId,
          new CreateOrganizationRequest().legalName(org.name + " AB"));
    }
    catch (final RestClientResponseException e) {
      // Tests in this class share one database, so an organization may already have been bootstrapped by
      // another test. Only "already exists" is tolerated here.
      if (e.getStatusCode().value() != 409) {
        throw e;
      }
    }
    return organizationApi;
  }

  private OrganizationApi bootstrappedOrganization(final JwtTestUtils.OrganisationType org) {
    return this.bootstrappedOrganization(org, TENANT, "swedenconnect");
  }

  private static int statusOf(final Throwable throwable) {
    return ((RestClientResponseException) throwable).getStatusCode().value();
  }

  private static String bodyOf(final Throwable throwable) {
    return ((RestClientResponseException) throwable).getResponseBodyAsString();
  }

  @Test
  @DisplayName("A domain held by one organization cannot be claimed by another on the same tenant")
  void aHeldDomainCannotBeClaimedByAnother() {
    final JwtTestUtils.OrganisationType holder = JwtTestUtils.OrganisationType.TESTORG1;
    final JwtTestUtils.OrganisationType contender = JwtTestUtils.OrganisationType.TESTORG2;
    final String domain = "held.example.se";

    final Domain claimed = this.bootstrappedOrganization(holder)
        .requestDomain(TENANT, holder.orgId, new DomainRequest().domain(domain));
    assertThat(claimed.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);

    final OrganizationApi contenderApi = this.bootstrappedOrganization(contender);
    assertThatThrownBy(() -> contenderApi.requestDomain(TENANT, contender.orgId,
        new DomainRequest().domain(domain)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409))
        .satisfies(ex -> assertThat(bodyOf(ex))
            .contains("Domain %s is already held by another organization".formatted(domain)));
  }

  @Test
  @DisplayName("Holding a domain does not reserve its subdomains for the holder")
  void holdingADomainDoesNotReserveItsSubdomains() {
    final JwtTestUtils.OrganisationType holder = JwtTestUtils.OrganisationType.TESTORG3;
    final JwtTestUtils.OrganisationType neighbour = JwtTestUtils.OrganisationType.TESTORG4;

    this.bootstrappedOrganization(holder)
        .requestDomain(TENANT, holder.orgId, new DomainRequest().domain("parent.example.se"));

    final Domain subdomain = this.bootstrappedOrganization(neighbour)
        .requestDomain(TENANT, neighbour.orgId, new DomainRequest().domain("a.parent.example.se"));

    assertThat(subdomain.getDomain()).isEqualTo("a.parent.example.se");
    assertThat(subdomain.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);
  }

  @Test
  @DisplayName("A rejected claim releases the domain, and the released holder cannot take it back")
  void rejectionReleasesTheDomainToTheNextClaimant() {
    final JwtTestUtils.OrganisationType first = JwtTestUtils.OrganisationType.TESTORG5;
    final JwtTestUtils.OrganisationType second = JwtTestUtils.OrganisationType.TESTORG6;
    final String domain = "released.example.se";

    final OrganizationApi firstApi = this.bootstrappedOrganization(first);
    final Domain firstClaim = firstApi.requestDomain(TENANT, first.orgId, new DomainRequest().domain(domain));

    final OrganizationApi secondApi = this.bootstrappedOrganization(second);
    assertThatThrownBy(() -> secondApi.requestDomain(TENANT, second.orgId, new DomainRequest().domain(domain)))
        .as("The domain is still held while the first claim is pending")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409));

    this.operatorAdminApi.rejectDomain(TENANT, OPERATOR.orgId, firstClaim.getDomainId(),
        new RejectRegistrationRequest().rejectionReason("Not yours"));

    final Domain secondClaim = secondApi.requestDomain(TENANT, second.orgId, new DomainRequest().domain(domain));
    assertThat(secondClaim.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);
    assertThat(secondClaim.getDomainId()).isNotEqualTo(firstClaim.getDomainId());

    assertThatThrownBy(() -> firstApi.requestDomain(TENANT, first.orgId, new DomainRequest().domain(domain)))
        .as("Re-opening a rejected claim is refused once somebody else has taken the domain")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409))
        .satisfies(ex -> assertThat(bodyOf(ex)).contains(ALREADY_HELD));
  }

  @Test
  @DisplayName("A withdrawn claim releases the domain as well")
  void withdrawalReleasesTheDomain() {
    final JwtTestUtils.OrganisationType first = JwtTestUtils.OrganisationType.TESTORG7;
    final JwtTestUtils.OrganisationType second = JwtTestUtils.OrganisationType.TESTORG8;
    final String domain = "withdrawn.example.se";

    final OrganizationApi firstApi = this.bootstrappedOrganization(first);
    final Domain firstClaim = firstApi.requestDomain(TENANT, first.orgId, new DomainRequest().domain(domain));
    firstApi.deleteDomain(TENANT, first.orgId, firstClaim.getDomainId());

    final Domain secondClaim = this.bootstrappedOrganization(second)
        .requestDomain(TENANT, second.orgId, new DomainRequest().domain(domain));
    assertThat(secondClaim.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);
  }

  @Test
  @DisplayName("An approved domain is held just as firmly as a pending one")
  void anApprovedDomainStaysHeld() {
    final JwtTestUtils.OrganisationType holder = JwtTestUtils.OrganisationType.TESTORG9;
    final JwtTestUtils.OrganisationType contender = JwtTestUtils.OrganisationType.TESTORG10;
    final String domain = "approved.example.se";

    final Domain claimed = this.bootstrappedOrganization(holder)
        .requestDomain(TENANT, holder.orgId, new DomainRequest().domain(domain));
    this.operatorAdminApi.approveDomain(TENANT, OPERATOR.orgId, claimed.getDomainId());

    final OrganizationApi contenderApi = this.bootstrappedOrganization(contender);
    assertThatThrownBy(() -> contenderApi.requestDomain(TENANT, contender.orgId,
        new DomainRequest().domain(domain)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409))
        .satisfies(ex -> assertThat(bodyOf(ex)).contains(ALREADY_HELD));
  }

  @Test
  @DisplayName("The rule stops at the tenant boundary: another tenant may hold the same domain")
  void anotherTenantMayHoldTheSameDomain() {
    final JwtTestUtils.OrganisationType org = JwtTestUtils.OrganisationType.TESTORG2;
    final String domain = "cross.example.se";

    final Domain onOwnTenant = this.bootstrappedOrganization(org)
        .requestDomain(TENANT, org.orgId, new DomainRequest().domain(domain));
    assertThat(onOwnTenant.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);

    final Domain onOtherTenant = this
        .bootstrappedOrganization(org, OTHER_TENANT, OTHER_TENANT_FUNCTION_GROUP)
        .requestDomain(OTHER_TENANT, org.orgId, new DomainRequest().domain(domain));
    assertThat(onOtherTenant.getStatus()).isEqualTo(Domain.StatusEnum.PENDING);
    assertThat(onOtherTenant.getDomainId()).isNotEqualTo(onOwnTenant.getDomainId());
  }
}
