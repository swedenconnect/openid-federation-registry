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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;
import java.util.UUID;

/**
 * Testing the new optional api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OptionsApiPolicyControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestDataOperations testDataOperations;

  @Test
  public void testCRUDPolicies() throws IOException {

    final UUID id_skatt = testDataOperations.createPolicies(JwtTestUtils.OrganisationType.SKATT);
    testDataOperations.get(FkKeyType.POLICIES, id_skatt, HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);
    testDataOperations.delete(FkKeyType.POLICIES, id_skatt, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.AF);
    testDataOperations.delete(FkKeyType.POLICIES, id_skatt, HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);
    testDataOperations.get(FkKeyType.POLICIES, id_skatt, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.SKATT);

    final UUID af = testDataOperations.createPolicies(JwtTestUtils.OrganisationType.AF);
    testDataOperations.get(FkKeyType.POLICIES, af, HttpStatus.OK, JwtTestUtils.OrganisationType.AF);
    testDataOperations.updatePolicies(JwtTestUtils.OrganisationType.AF, af, Map.of("name", "update"));

    testDataOperations.get(FkKeyType.POLICIES, af, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.PM);
  }

  @Test
  public void testListPolicies() throws IOException {

    testDataOperations.createPolicies(JwtTestUtils.OrganisationType.SKATT);
    testDataOperations.createPolicies(JwtTestUtils.OrganisationType.SKATT);

    testDataOperations.createPolicies(JwtTestUtils.OrganisationType.AF);
    testDataOperations.createPolicies(JwtTestUtils.OrganisationType.AF);

    final JsonNode response = testDataOperations.listForFKType(FkKeyType.POLICIES, JwtTestUtils.OrganisationType.SKATT);
    System.out.println(response);

    final JsonNode af = testDataOperations.listForFKType(FkKeyType.POLICIES, JwtTestUtils.OrganisationType.AF);
    System.out.println(af);

    final JsonNode pm = testDataOperations.listForFKType(FkKeyType.POLICIES, JwtTestUtils.OrganisationType.PM);
    System.out.println(pm);
  }


}
