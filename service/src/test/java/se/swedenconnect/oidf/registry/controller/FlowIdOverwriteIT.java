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
import se.swedenconnect.oidf.registry.api.model.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.api.model.StepDto;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces the caller-selected-UUID overwrite IDOR from the external vulnerability report ("Caller-selected flow UUID
 * overwrites another organization's registration flow"): {@code RegistrationFlowController.createFlowWithId} accepts a
 * caller-supplied UUID and passes it straight into {@code RegistrationFlowService.createRegistrationFlow}, which builds
 * a fresh {@code RegistrationFlow} entity with that ID and calls {@code flowRepository.save(...)} with no existence
 * check. Since {@code RegistrationFlow.flowId} has no {@code @GeneratedValue}, Spring Data JPA treats a non-null
 * caller-supplied ID as "not new" and performs a merge/UPDATE instead of an insert — silently overwriting another
 * organization's flow, including its {@code organization_id}, if the caller happens to know (or is handed) its UUID.
 * <p>
 * Alice and Charlie are different organizations on the same tenant, matching the report's own PoC. This test asserts
 * the SECURE behavior (a duplicate caller-selected ID must be rejected with 409, not silently applied) and therefore
 * currently FAILS — that failure documents the vulnerability. Once creation uses insert-only semantics, this test
 * should pass unmodified.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class FlowIdOverwriteIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType ALICE = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType CHARLIE = JwtTestUtils.OrganisationType.AF;

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
    this.aliceFlowApi = new RegistrationFlowApi(buildApiClient(ALICE));
    this.charlieFlowApi = new RegistrationFlowApi(buildApiClient(CHARLIE));
  }

  private ApiClient buildApiClient(final JwtTestUtils.OrganisationType org) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(org));
    return client;
  }

  @Test
  @DisplayName("createFlowWithId must not let another organization overwrite an existing flow by reusing its UUID "
      + "— currently FAILS: Charlie's create-with-id silently replaces Alice's flow and takes ownership of it")
  void createFlowWithIdMustNotOverwriteAnotherOrgsFlow() {
    final RegistrationFlowDto aliceOriginal = this.aliceFlowApi.createFlow(TENANT, ALICE.orgId,
        new RegistrationFlowDto()
            .name("alice-owned-flow")
            .description("Alice's original flow")
            .technology(RegistrationFlowDto.TechnologyEnum.OIDC)
            .steps(List.of(new StepDto()
                .stepId(UUID.randomUUID())
                .name("MinimalStep")
                .description("Test step"))));
    final UUID aliceFlowId = aliceOriginal.getFlowId();

    // Control: before the attack, Alice can read her flow and Charlie cannot.
    assertThat(this.aliceFlowApi.getFlow(TENANT, ALICE.orgId, aliceFlowId).getName()).isEqualTo("alice-owned-flow");
    assertThatThrownBy(() -> this.charlieFlowApi.getFlow(TENANT, CHARLIE.orgId, aliceFlowId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));

    assertThatThrownBy(() -> this.charlieFlowApi.createFlowWithId(TENANT, CHARLIE.orgId, aliceFlowId,
        new RegistrationFlowDto()
            .name("charlie-overwrite")
            .description("Charlie's overwrite attempt")
            .technology(RegistrationFlowDto.TechnologyEnum.SAML)
            .steps(List.of(new StepDto()
                .stepId(UUID.randomUUID())
                .name("MinimalStep")
                .description("Test step")))))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(409));

    final RegistrationFlowDto aliceAfter = this.aliceFlowApi.getFlow(TENANT, ALICE.orgId, aliceFlowId);
    assertThat(aliceAfter.getName()).isEqualTo("alice-owned-flow");
    assertThatThrownBy(() -> this.charlieFlowApi.getFlow(TENANT, CHARLIE.orgId, aliceFlowId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
  }
}
