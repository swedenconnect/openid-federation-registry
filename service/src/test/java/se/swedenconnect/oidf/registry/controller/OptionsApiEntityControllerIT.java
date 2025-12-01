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
import org.junit.jupiter.api.DisplayName;
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
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static se.swedenconnect.oidf.registry.entity.FkKeyType.FEDERATION_ENTITY;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.*;

/**
 * Integration tests for the {@link OptionsApiController} class.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OptionsApiEntityControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>(TestContainersConfiguration.MARIADB_VERSION);

  @Autowired
  private TestDataOperations testDataOperations;

  @Test
  @DisplayName( "Create hosted entity - should succeed")
  public void testHostedEntity() {
    final OptionsRecord optionsRecord = new OptionsRecord();
    optionsRecord.setOption(
        List.of(
            Values.builder().key("issuer").value(AF.domainPrefix).build(),
            Values.builder().key("subject").value(AF.domainPrefix).build()
        ));

    OptionsRecord result = testDataOperations.postPut(FEDERATION_ENTITY,
        UUID.randomUUID(),
        HttpStatus.CREATED,
        AF, optionsRecord,
        HttpMethod.POST);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("Get hosted entity template - should succeed")
  public void testHostedEntityTemplateIsFilteredForVariables() {
    final OptionsRecord template = testDataOperations.get(FEDERATION_ENTITY, null, HttpStatus.OK, SKATT);
    assertThat(template).isNotNull();
    assertThat(template.getOption()).isNotEmpty();

    template.getOption().forEach(values -> {
      assertThat(values.getKey()).isNotNull();
      assertThat(values.getValue().contains("@{"))
          .withFailMessage("The value should not contain @{ but is: \" + values.getValue() + \" ")
          .isFalse();

      assertThat(values.getValidation()).isNotNull();
      assertThat(values.getValidation().contains("@{"))
          .withFailMessage("The value should not contain @{ but is: " + values.getValidation() + " ")
          .isFalse();
    });

  }

  @Test
  @DisplayName("CRUD hosted entity - should succeed")
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

    assertThat(data.getSubject()).isEqualTo(SKATT.domainPrefix + "/op");

    testDataOperations.delete(FEDERATION_ENTITY, id_skatt, HttpStatus.OK,
        SKATT);

  }

  @Test
  @DisplayName("List hosted entities - should succeed")
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

    assertThat(response.size()).isEqualTo(2);

  }

  @Test
  @DisplayName("Delete hosted entity - should succeed")
  public void testHostedEntityDelete() {
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
  @DisplayName("Update hosted entity - should succeed")
  public void testHostedEntityWithDifferentIssuerAndSubject() {
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
  @DisplayName("CRUD subordinate entity - should succeed")
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

    assertThat(data.getSubject()).isEqualTo("http://www.swedenconnect.se/op");

    testDataOperations.delete(FkKeyType.SUBORDINATE_ENTITY, id_skatt, HttpStatus.OK,
        SKATT);

  }

}
