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
import se.swedenconnect.oidf.entity.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing the new optional api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OptionsApiEntityControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestDataOperations testDataOperations;

  @Test
  public void testCRUDHostedEntity() throws IOException {

    final UUID id_skatt = testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    testDataOperations.updateHostedEntity(
        id_skatt,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .subject("http://www.swedenconnect.se/op")
            .build());

    OptionsTestData.SubordinateEntityTestData data = testDataOperations.get(FkKeyType.HOSTED_ENTITY,
        id_skatt,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT,
        OptionsTestData.SubordinateEntityTestData.class);

    assertEquals(data.getSubject(), "http://www.swedenconnect.se/op");

    testDataOperations.delete(FkKeyType.HOSTED_ENTITY, id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

  }

  @Test
  public void testCRUDSubordinateEntity() throws IOException {

    final UUID id_skatt = testDataOperations.createSubordinateEntity(
        UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .build());

    testDataOperations.updateSubordinateEntity(
        id_skatt,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .subject("http://www.swedenconnect.se/op")
            .build());

    OptionsTestData.SubordinateEntityTestData data = testDataOperations.get(FkKeyType.SUBORDINATE_ENTITY,
        id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT, OptionsTestData.SubordinateEntityTestData.class);

    assertEquals(data.getSubject(), "http://www.swedenconnect.se/op");

    testDataOperations.delete(FkKeyType.SUBORDINATE_ENTITY, id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

  }

  @Test
  public void testCRUDSubordinateEntity() throws IOException {

    final UUID id_skatt = testDataOperations.createSubordinateEntity(
        UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .build());

    testDataOperations.updateSubordinateEntity(
        id_skatt,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .subject("http://www.swedenconnect.se/op")
            .build());

    OptionsTestData.SubordinateEntityTestData data = testDataOperations.get(FkKeyType.SUBORDINATE_ENTITY,
        id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT, OptionsTestData.SubordinateEntityTestData.class);

    assertEquals(data.getSubject(), "http://www.swedenconnect.se/op");


  }
}
