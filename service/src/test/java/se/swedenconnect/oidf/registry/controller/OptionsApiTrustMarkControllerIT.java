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
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

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
class OptionsApiTrustMarkControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestDataOperations testDataOperations;

  private static void assertThatTrustmarkIdOptionExist(final OptionsRecord optionsRecord, final UUID tmiId1) {
    long count = optionsRecord.getOption()
        .stream()
        .filter(values -> values.getKey().equals("trustmark_id"))
        .flatMap(values -> values.getOptions().stream())
        .map(OptionRecord::getKey)
        .filter(key -> key.equals(tmiId1.toString()))
        .count();

    assertEquals(1, count, "Did not find trustmarkissuer_id: " + tmiId1.toString());
  }

  private static void assertThatTrustmarkissueridOptionExist(final OptionsRecord optionsRecord, final UUID tmiId1) {
    long count = optionsRecord.getOption()
        .stream()
        .filter(values -> values.getKey().equals("trustmarkissuer_id"))
        .flatMap(values -> values.getOptions().stream())
        .map(OptionRecord::getKey)
        .filter(key -> key.equals(tmiId1.toString()))
        .count();

    assertEquals(1, count, "Did not find trustmarkissuer_id: " + tmiId1.toString());
  }

  @Test
  public void testOptionsTrustMarkId() throws IOException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId2 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId2));

    final OptionsRecord optionsRecord1 = testDataOperations.get(FkKeyType.TRUSTMARK,
        null,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

    assertThatTrustmarkissueridOptionExist(optionsRecord1, tmiId1);

    final OptionsRecord optionsRecord2 = testDataOperations.get(FkKeyType.TRUSTMARK,
        null,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.AF);

    assertThatTrustmarkissueridOptionExist(optionsRecord2, tmiId2);
  }

  @Test
  public void testOptionsTrustMarkSubjectId() throws IOException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId2 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId2));

    final UUID tm1 = testDataOperations.createTrustMark(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId1));

    final UUID tm2 = testDataOperations.createTrustMark(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId2));

    final OptionsRecord optionsRecord1 = testDataOperations.get(FkKeyType.TRUSTMARKSUBJECT,
        null,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

    assertThatTrustmarkIdOptionExist(optionsRecord1, tm1);

    final OptionsRecord optionsRecord2 = testDataOperations.get(FkKeyType.TRUSTMARKSUBJECT,
        null,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.AF);

    assertThatTrustmarkIdOptionExist(optionsRecord2, tm2);
  }

  @Test
  public void testCRUDTrustMark() throws IOException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId2 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId2));
    final UUID tmId = UUID.randomUUID();

    testDataOperations.get(FkKeyType.TRUSTMARK, null, HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);

    testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CONFLICT,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.AF,
        HttpStatus.UNAUTHORIZED,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.delete(FkKeyType.TRUSTMARK,
        tmId,
        HttpStatus.UNAUTHORIZED,
        JwtTestUtils.OrganisationType.AF);

    testDataOperations.delete(FkKeyType.TRUSTMARK,
        tmId,
        HttpStatus.OK,
        JwtTestUtils.OrganisationType.SKATT);

    testDataOperations.get(FkKeyType.TRUSTMARK,
        tmId, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.SKATT);

  }

  @Test
  public void testCRUDTrustMarkSubject() throws IOException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.builder()
            .build());

    final UUID tmiId = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID tmId = UUID.randomUUID();

    final UUID tmID = testDataOperations.createTrustMark(
        tmId,
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId));

    final UUID trustmarksubjectid =
        testDataOperations.createTrustMarkSubject(UUID.randomUUID(),
            JwtTestUtils.OrganisationType.SKATT,
            HttpStatus.CREATED,
            TestDataOperations.defaultTrustMarkSubject(tmID));

    final OptionsRecord optionsRecord = testDataOperations.get(FkKeyType.TRUSTMARKSUBJECT, trustmarksubjectid,
        HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);

    System.out.println(optionsRecord);

  }

}
