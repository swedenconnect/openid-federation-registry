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
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.swedenconnect.oidf.registry.entity.FkKeyType.FEDERATION_ENTITY;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.AF;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.PM;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.SKATT;

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
            Values.builder().key("issuer").value(AF.domainPrefix).build(),
            Values.builder().key("subject").value(AF.domainPrefix).build()
        ));

    testDataOperations.postPut(FEDERATION_ENTITY,
        UUID.randomUUID(),
        HttpStatus.CREATED,
        AF, optionsRecord,
        HttpMethod.POST);
  }

  @Test
  public void testHostedEntityTemplateIsFilteredForVariables() {
    final OptionsRecord template = testDataOperations.get(FEDERATION_ENTITY, null, HttpStatus.OK, SKATT);
    template.getOption().forEach(values -> {
      assertTrue(!values.getValue().contains("@{"),
          "The value should not contain @{ but is: " + values.getValue() + " ");
      assertTrue(!values.getValidation().contains("@{"),
          "The value should not contain @{ but is: " + values.getValidation() + " ");
    });

  }

  @Test
  public void testCRUDHostedEntity() {

    final UUID id_skatt = testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(SKATT)
            .build());

    testDataOperations.updateHostedEntity(
        id_skatt,
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(SKATT)
            .subject("http://www.skv.se/oidf/op")
            .build());

    final OptionsTestData.SubordinateEntityTestData data = testDataOperations.get(FEDERATION_ENTITY,
        id_skatt,
        HttpStatus.OK,
        SKATT,
        OptionsTestData.SubordinateEntityTestData.class);

    assertEquals(data.getSubject(), SKATT.domainPrefix + "/op");

    testDataOperations.delete(FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        SKATT);

  }

  @Test
  public void testList() {
    final JwtTestUtils.OrganisationType org = SKATT;

    testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(org)
            .build());

    testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(org)
            .build());

    testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        PM,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(PM)
            .build());

    final List<OptionsTestData.HostedEntityTestData> response =
        testDataOperations.listForFKType(FEDERATION_ENTITY,
            org,
            OptionsTestData.HostedEntityTestData.class);

    assertEquals(2, response.size());

  }

  @Test
  public void testHostedEntityDelete() throws IOException {
    final JwtTestUtils.OrganisationType org = SKATT;
    final UUID id_skatt = testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(org)
            .issuer(org.domainPrefix + "/ta")
            .subject(org.domainPrefix + "/ta")
            .build());

    final UUID taId = testDataOperations.createTrustAnchor(UUID.randomUUID(), org, HttpStatus.CREATED,
        OptionsTestData.TrustAnchorTestData.builder().entityId(id_skatt).build());

    testDataOperations.delete(FEDERATION_ENTITY, id_skatt, HttpStatus.BAD_REQUEST,
        SKATT);

    testDataOperations.delete(FkKeyType.TRUSTANCHOR, taId, HttpStatus.OK,
        SKATT);

    testDataOperations.delete(FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        SKATT);

  }

  @Test
  public void testHostedEntityWithDifferentIssuerAndSubject() throws IOException {
    final JwtTestUtils.OrganisationType org = SKATT;
    final UUID id_skatt = testDataOperations.createHostedEntity(
        UUID.randomUUID(),
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(org)
            .issuer(org.domainPrefix + "/ta")
            .subject(org.domainPrefix + "/different")
            .build());

    testDataOperations.createTrustAnchor(UUID.randomUUID(), org, HttpStatus.NOT_FOUND,
        OptionsTestData.TrustAnchorTestData.builder().entityId(id_skatt).build());

    testDataOperations.updateHostedEntity(
        id_skatt,
        org,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(org)
            .build());

    final UUID taId = testDataOperations.createTrustAnchor(UUID.randomUUID(), org, HttpStatus.CREATED,
        OptionsTestData.TrustAnchorTestData.builder().entityId(id_skatt).build());

    testDataOperations.updateHostedEntity(
        id_skatt,
        org,
        HttpStatus.BAD_REQUEST,
        OptionsTestData.HostedEntityTestData.create(org)
            .issuer(org.domainPrefix + "/ta")
            .subject(org.domainPrefix + "/im")
            .build());

    testDataOperations.delete(FkKeyType.TRUSTANCHOR, taId, HttpStatus.OK,
        SKATT);

    testDataOperations.delete(FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        SKATT);

  }

  @Test
  public void testCRUDSubordinateEntity() {

    final UUID id_skatt = testDataOperations.createSubordinateEntity(
        UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .build());

    testDataOperations.updateSubordinateEntity(
        id_skatt,
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.SubordinateEntityTestData.builder()
            .subject("http://www.swedenconnect.se/op")
            .build());

    OptionsTestData.SubordinateEntityTestData data = testDataOperations.get(FkKeyType.SUBORDINATE_ENTITY,
        id_skatt, HttpStatus.OK,
        SKATT, OptionsTestData.SubordinateEntityTestData.class);

    assertEquals(data.getSubject(), "http://www.swedenconnect.se/op");

    testDataOperations.delete(FkKeyType.SUBORDINATE_ENTITY, id_skatt, HttpStatus.OK,
        SKATT);

  }

}
