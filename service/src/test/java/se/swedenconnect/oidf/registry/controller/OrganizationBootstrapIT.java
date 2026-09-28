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
import se.swedenconnect.oidf.registry.api.OrganizationApi;
import se.swedenconnect.oidf.registry.api.model.CreateOrganizationRequest;
import se.swedenconnect.oidf.registry.api.model.Organization;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers an organization bootstrapping its own registry record: the record does not exist until the
 * organization posts it, posting it twice is a conflict, and another organization cannot post it at all.
 *
 * @author Felix Hellman
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class OrganizationBootstrapIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType APPLICANT = JwtTestUtils.OrganisationType.TESTORG1;
  private static final JwtTestUtils.OrganisationType OTHER = JwtTestUtils.OrganisationType.TESTORG2;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private OrganizationApi organizationApi(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return new OrganizationApi(client);
  }

  private static int statusOf(final Throwable throwable) {
    return ((RestClientResponseException) throwable).getStatusCode().value();
  }

  @Test
  @DisplayName("An organization is created only by posting it, and only once")
  void organizationIsCreatedOnlyByPostingItAndOnlyOnce() {
    final OrganizationApi applicantApi = this.organizationApi(APPLICANT);

    assertThatThrownBy(() -> applicantApi.getOrganization(TENANT, APPLICANT.orgId))
        .as("An organization that has not bootstrapped does not exist yet")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));

    final Organization created = applicantApi.createOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("TestOrg1 AB"));
    assertThat(created.getOrgNumber()).isEqualTo(APPLICANT.orgId);
    assertThat(created.getLegalName()).isEqualTo("TestOrg1 AB");
    assertThat(created.getTenant()).isEqualTo("swedenconnect");
    assertThat(created.getDomains()).isEmpty();
    assertThat(created.getPreValidatedTrustMarks()).isEmpty();

    final Organization fetched = applicantApi.getOrganization(TENANT, APPLICANT.orgId);
    assertThat(fetched.getLegalName()).isEqualTo("TestOrg1 AB");
    assertThat(fetched.getOrgName()).isEqualTo(APPLICANT.name);

    assertThatThrownBy(() -> applicantApi.createOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("TestOrg1 AB")))
        .as("Bootstrapping twice is a conflict, not a silent overwrite")
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(409));

    final Organization updated = applicantApi.updateOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("TestOrg1 Aktiebolag"));
    assertThat(updated.getLegalName()).isEqualTo("TestOrg1 Aktiebolag");
    assertThat(applicantApi.getOrganization(TENANT, APPLICANT.orgId).getLegalName())
        .isEqualTo("TestOrg1 Aktiebolag");
  }

  @Test
  @DisplayName("Updating an organization that does not exist is a 404")
  void updatingAMissingOrganizationIsNotFound() {
    final OrganizationApi otherApi = this.organizationApi(OTHER);
    assertThatThrownBy(() -> otherApi.updateOrganization(TENANT, OTHER.orgId,
        new CreateOrganizationRequest().legalName("TestOrg2 AB")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(404));
  }

  @Test
  @DisplayName("A blank legal name is rejected")
  void blankLegalNameIsRejected() {
    final OrganizationApi applicantApi = this.organizationApi(JwtTestUtils.OrganisationType.TESTORG3);
    assertThatThrownBy(() -> applicantApi.createOrganization(TENANT,
        JwtTestUtils.OrganisationType.TESTORG3.orgId, new CreateOrganizationRequest().legalName("  ")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(400));
  }

  @Test
  @DisplayName("One organization cannot bootstrap or read another organization's record")
  void crossOrganizationAccessIsForbidden() {
    final OrganizationApi otherApi = this.organizationApi(OTHER);

    assertThatThrownBy(() -> otherApi.getOrganization(TENANT, APPLICANT.orgId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(403));

    assertThatThrownBy(() -> otherApi.createOrganization(TENANT, APPLICANT.orgId,
        new CreateOrganizationRequest().legalName("Not mine to post")))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(403));
  }
}
