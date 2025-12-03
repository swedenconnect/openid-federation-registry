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

package se.swedenconnect.oidf.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.fixture.UseMariaDBContainer;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.PM;

/**
 * Integration tests for the {@link OptionsApiController} class.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@UseMariaDBContainer
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OptionsApiControllerIT {

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
  @DisplayName("List All after creation - should include all created elements")
  public void testList() throws IOException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        PM,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(PM)
            .issuer(PM.domainPrefix)
            .subject(PM.domainPrefix)
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        PM,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    testDataOperations.createPolicies(PM);

    testDataOperations.createResolver(UUID.randomUUID(),
        PM,
        HttpStatus.CREATED,
        TestDataOperations.defaultResolver(entityId));

    testDataOperations.createTrustAnchor(UUID.randomUUID(),
        PM,
        HttpStatus.CREATED,
        OptionsTestData.TrustAnchorTestData.builder().entityId(entityId).build());

    final JsonNode responseBody = testDataOperations.listAll(PM);
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
