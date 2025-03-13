/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.entity.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing the new optional api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OptionsApiControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");
  final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired
  private TestDataOperations testDataOperations;
  @Autowired
  private AuditEventRepository auditEventRepository;

  @Test
  public void testOptionsRequest() {
    Arrays.stream(FkKeyType.values()).forEach(type ->
        testDataOperations.get(type, null, HttpStatus.OK, JwtTestUtils.OrganisationType.AF));
  }

  @Test
  public void testList() throws IOException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.PM,
        HttpStatus.CREATED,
        TestDataOperations.defaultHostedEntity(null));

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.PM,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));



    testDataOperations.createPolicies(JwtTestUtils.OrganisationType.PM);

    testDataOperations.createResolver(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.PM,
        HttpStatus.CREATED,
        TestDataOperations.defaultResolver(entityId));

    testDataOperations.createTrustAnchor(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.PM,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustAnchor(entityId));



    final JsonNode responseBody = testDataOperations.listAll(JwtTestUtils.OrganisationType.PM);
    assertThat(responseBody).isNotNull();

    assertThat(responseBody.has("POLICIES")).isTrue();
    assertThat(responseBody.get("POLICIES").size()).isGreaterThan(0);
    assertThat(responseBody.get("POLICIES").elements().hasNext()).isTrue();

    assertThat(responseBody.has("RESOLVER")).isTrue();
    assertThat(responseBody.get("RESOLVER").size()).isGreaterThan(0);
    assertThat(responseBody.get("RESOLVER").elements().hasNext()).isTrue();

    assertThat(responseBody.has("TRUSTANCHOR")).isTrue();
    assertThat(responseBody.get("TRUSTANCHOR").size()).isGreaterThan(0);
    assertThat(responseBody.get("TRUSTANCHOR").elements().hasNext()).isTrue();

    assertThat(responseBody.has("TRUSTMARKISSUER")).isTrue();
    assertThat(responseBody.get("TRUSTMARKISSUER").size()).isGreaterThan(0);
    assertThat(responseBody.get("TRUSTMARKISSUER").elements().hasNext()).isTrue();

  }

}
