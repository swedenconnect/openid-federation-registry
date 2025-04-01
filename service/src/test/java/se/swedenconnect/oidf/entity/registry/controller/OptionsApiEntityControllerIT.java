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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.entity.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.io.IOException;
import java.util.List;
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
  public void testHostedEntity() {
    final OptionsRecord optionsRecord = new OptionsRecord();
    optionsRecord.setOption(
        List.of(
            Values.builder().key("issuer").value("http://issuer").build(),
            Values.builder().key("subject").value("http://subject").build()
        ));

    testDataOperations.postPut(FkKeyType.FEDERATION_ENTITY,
        UUID.randomUUID(),
        HttpStatus.CREATED,
        JwtTestUtils.OrganisationType.AF, optionsRecord,
        HttpMethod.POST);
  }


  @Test
  public void testCRUDHostedEntity() {

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

    final OptionsTestData.SubordinateEntityTestData data = testDataOperations.get(FkKeyType.FEDERATION_ENTITY,
        id_skatt,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT,
        OptionsTestData.SubordinateEntityTestData.class);

    assertEquals(data.getSubject(), "http://www.swedenconnect.se/op");

    testDataOperations.delete(FkKeyType.FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

  }

  @Test
  public void testList() {
    final JwtTestUtils.OrganisationType org = JwtTestUtils.OrganisationType.SKATT;

    testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        JwtTestUtils.OrganisationType.PM,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final List<OptionsTestData.HostedEntityTestData> response =
        testDataOperations.listForFKType(FkKeyType.FEDERATION_ENTITY,
            org,
            OptionsTestData.HostedEntityTestData.class);

    assertEquals(2, response.size());


  }

  @Test
  public void testHostedEntityDelete() throws IOException {
    final JwtTestUtils.OrganisationType org = JwtTestUtils.OrganisationType.SKATT;
    final UUID id_skatt = testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .issuer("http://www.skatt.se/oidf/ta")
            .subject("http://www.skatt.se/oidf/ta")
            .build());

    final UUID taId = testDataOperations.createTrustAnchor(UUID.randomUUID(), org, HttpStatus.CREATED,
        OptionsTestData.TrustAnchorTestData.builder().entityId(id_skatt).build());

    testDataOperations.delete(FkKeyType.FEDERATION_ENTITY, id_skatt, HttpStatus.BAD_REQUEST,
        JwtTestUtils.OrganisationType.SKATT);

    testDataOperations.delete(FkKeyType.TRUSTANCHOR, taId, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

    testDataOperations.delete(FkKeyType.FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

  }


  @Test
  public void testHostedEntityWithDifferentIssuerAndSubject() throws IOException {
    final JwtTestUtils.OrganisationType org = JwtTestUtils.OrganisationType.SKATT;
    final UUID id_skatt = testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .issuer("http://www.skatt.se/oidf/issuer")
            .subject("http://www.skatt.se/oidf/subject")
            .build());

    testDataOperations.createTrustAnchor(UUID.randomUUID(), org, HttpStatus.BAD_REQUEST,
        OptionsTestData.TrustAnchorTestData.builder().entityId(id_skatt).build());

    testDataOperations.updateHostedEntity(
        id_skatt,
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .subject("http://www.skatt.se/oidf/ta")
            .issuer("http://www.skatt.se/oidf/ta")
            .build());

    final UUID taId = testDataOperations.createTrustAnchor(UUID.randomUUID(), org, HttpStatus.CREATED,
        OptionsTestData.TrustAnchorTestData.builder().entityId(id_skatt).build());

    testDataOperations.updateHostedEntity(
        id_skatt,
        org,
        HttpStatus.BAD_REQUEST,
        OptionsTestData.HostedEntityTestData.builder()
            .subject("http://www.skatt.se/oidf/ta")
            .issuer("http://www.skatt.se/oidf/im")
            .build());

    testDataOperations.delete(FkKeyType.TRUSTANCHOR, taId, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

    testDataOperations.delete(FkKeyType.FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

  }

  @Test
  public void testCRUDSubordinateEntity() {

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


}
