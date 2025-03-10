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
import java.util.UUID;

/**
 * Testing the new optional api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OptionsApiTrustMarkControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestDataOperations testDataOperations;

  @Test
  public void testCRUDTrustMark() throws IOException {

    final UUID tmiId = testDataOperations.createTMI(JwtTestUtils.OrganisationType.SKATT);
    final UUID tmId = UUID.randomUUID();

    testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId));

    testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CONFLICT,
        TestDataOperations.defaultTrustMark(tmiId));

    testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.UNAUTHORIZED,
        TestDataOperations.defaultTrustMark(tmiId));

    testDataOperations.delete(FkKeyType.TRUSTMARK,
        tmId,
        HttpStatus.UNAUTHORIZED,
        JwtTestUtils.OrganisationType.AF);

    testDataOperations.delete(FkKeyType.TRUSTMARK,
        tmId,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

  }

}
