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
import se.swedenconnect.oidf.registry.api.RegistrationFlowApi;
import se.swedenconnect.oidf.registry.api.model.FlowSummaryDto;
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces the cross-instance registration-flow IDOR from the external vulnerability report
 * ("Organization-number-only ownership lookups cross tenant instance boundaries"): the same org number can legitimately
 * exist as two different {@code Organization} rows on two different instances (migration V21), but
 * {@code FlowRepository.findByOrganizationOrgNumber(AndFlowId)} and
 * {@code RegistrationFlowService.listFlows}/{@code findOwnedFlowOrThrow} scope ownership by that raw org-number string
 * alone, so a caller authorized for org number X on their own tenant can list and delete a same-numbered org's flow on
 * a completely different tenant.
 * <p>
 * Alice and Charlie hold the same organization number ({@link JwtTestUtils.OrganisationType#PM}) but on two different,
 * already-configured test instances — {@code ENA} for Alice, {@code Swedenconnect} for Charlie (see
 * {@code service/src/test/resources/application.yml}) — so this reproduces the report with no new configuration and no
 * WireMock/pipeline execution, unlike the registration-admin case.
 * <p>
 * Each test here asserts the SECURE behavior and therefore currently FAILS — that failure documents the vulnerability.
 * Once the lookups are scoped to the caller's own {@code Organization.organizationId} rather than the raw org-number
 * string, these tests should pass unmodified.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class FlowCrossInstanceIT {

  private static final String ALICE_TENANT = "ENA";
  private static final String CHARLIE_TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType ORG = JwtTestUtils.OrganisationType.PM;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private RegistrationFlowApi aliceFlowApi;
  private RegistrationFlowApi charlieFlowApi;

  @BeforeEach
  void setUp() {
    this.aliceFlowApi = new RegistrationFlowApi(this.buildApiClient("ena"));
    this.charlieFlowApi = new RegistrationFlowApi(this.buildApiClient("swedenconnect"));
  }

  private ApiClient buildApiClient(final String functionGroup) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(ORG, functionGroup, "admin"));
    return client;
  }

  private UUID createAliceFlow(final String name) {
    final RegistrationFlowDto flow = this.aliceFlowApi.createFlow(ALICE_TENANT, ORG.orgId,
        new RegistrationFlowDto()
            .name(name)
            .description("Fixture flow for FlowCrossInstanceIT")
            .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
            .steps(List.of(new StepDto()
                .stepId(UUID.randomUUID())
                .name("MinimalStep")
                .description("Test step"))));
    return flow.getFlowId();
  }

  @Test
  @DisplayName("listFlows must not disclose another instance's same-numbered org's flow — currently "
      + "FAILS: Charlie's tenant-one list includes Alice's tenant-two flow")
  void listFlowsMustNotDiscloseAnotherInstancesFlow() {
    final UUID aliceFlowId = this.createAliceFlow("Alice's ENA flow");

    final List<FlowSummaryDto> charliesFlows = this.charlieFlowApi.listFlows1(CHARLIE_TENANT, ORG.orgId);

    assertThat(charliesFlows).extracting(FlowSummaryDto::getFlowId).doesNotContain(aliceFlowId);
  }

  @Test
  @DisplayName("deleteFlow must not mutate another instance's same-numbered org's flow — currently "
      + "FAILS: Charlie can delete Alice's tenant-two flow through his own tenant-one route")
  void deleteFlowMustNotMutateAnotherInstancesFlow() {
    final UUID aliceFlowId = this.createAliceFlow("Alice's flow to protect");

    assertThatThrownBy(() -> this.charlieFlowApi.deleteFlow(CHARLIE_TENANT, ORG.orgId, aliceFlowId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));

    final RegistrationFlowDto stillOwnedByAlice = this.aliceFlowApi.getFlow(ALICE_TENANT, ORG.orgId, aliceFlowId);
    assertThat(stillOwnedByAlice.getFlowId()).isEqualTo(aliceFlowId);
  }
}
