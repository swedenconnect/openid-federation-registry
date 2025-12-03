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
import org.springframework.http.HttpStatus;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.fixture.OptionsTestData;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.fixture.UseMariaDBContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.AF;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.OrganisationType.SKATT;

/**
 * Integration tests for the {@link OptionsApiController} class.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@UseMariaDBContainer
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OptionsApiTrustMarkControllerIT {

  @Autowired
  private TestDataOperations testDataOperations;

  private static void assertThatTrustmarkIdOptionExist(final OptionsRecord optionsRecord, final UUID tmiId1) {
    assertThat(optionsRecord.getOption()).isNotEmpty();

    long count = optionsRecord.getOption()
        .stream()
        .filter(values -> {
          assertThat(values.getKey()).isNotBlank();
          return values.getKey().equals("trustmark_id");
        })
        .flatMap(values -> {
          assertNotNull(values.getOptions());
          assertThat(values.getOptions()).isNotEmpty();
          return values.getOptions().stream();
        })
        .map(OptionRecord::getKey)
        .filter(key -> key.equals(tmiId1.toString()))
        .count();

    assertThat(count)
        .withFailMessage("Did not find trustmark_id: " + tmiId1.toString())
        .isEqualTo(1);
  }

  private static void assertThatTrustmarkissueridOptionExist(final OptionsRecord optionsRecord, final UUID tmiId1) {
    assertThat(optionsRecord.getOption()).isNotEmpty();
    long count = optionsRecord.getOption()
        .stream()
        .filter(values -> {
          assertThat(values.getKey()).isNotBlank();
          return values.getKey().equals("trustmarkissuer_id");
        })
        .flatMap(values -> {
          assertThat(values.getOptions()).isNotEmpty();
          return values.getOptions().stream();
        })
        .map(OptionRecord::getKey)
        .filter(key -> key.equals(tmiId1.toString()))
        .count();

    assertThat(count)
        .withFailMessage("Did not find trustmarkissuer_id: " + tmiId1.toString())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Entity creation - should generate trust mark issuer id")
  public void testOptionsTrustMarkId() {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(SKATT)
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(AF)
            .build());

    final UUID tmiId2 = testDataOperations.createTMI(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId2));

    // Assert
    final OptionsRecord optionsRecord1 = testDataOperations.get(FkKeyType.TRUSTMARK,
        null,
        HttpStatus.OK,
        SKATT);

    assertThatTrustmarkissueridOptionExist(optionsRecord1, tmiId1);

    final OptionsRecord optionsRecord2 = testDataOperations.get(FkKeyType.TRUSTMARK,
        null,
        HttpStatus.OK,
        AF);

    assertThatTrustmarkissueridOptionExist(optionsRecord2, tmiId2);
  }

  @Test
  @DisplayName("Entity creation - should generate trust mark id")
  public void testOptionsTrustMarkSubjectId() {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(SKATT)
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(AF)
            .build());

    final UUID tmiId2 = testDataOperations.createTMI(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId2));

    final UUID tm1 = testDataOperations.createTrustMark(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId1));

    final UUID tm2 = testDataOperations.createTrustMark(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId2));

    // Assert
    final OptionsRecord optionsRecord1 = testDataOperations.get(FkKeyType.TRUSTMARKSUBJECT,
        null,
        HttpStatus.OK,
        SKATT);

    assertThatTrustmarkIdOptionExist(optionsRecord1, tm1);

    final OptionsRecord optionsRecord2 = testDataOperations.get(FkKeyType.TRUSTMARKSUBJECT,
        null,
        HttpStatus.OK,
        AF);

    assertThatTrustmarkIdOptionExist(optionsRecord2, tm2);
  }

  @Test
  @DisplayName("CRUD TrustMark - should succeed")
  public void testCRUDTrustMark() {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(SKATT)
            .build());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID entityId2 = testDataOperations.createHostedEntity(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(AF)
            .build());

    final UUID tmiId2 = testDataOperations.createTMI(UUID.randomUUID(),
        AF,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId2));
    final UUID tmId = UUID.randomUUID();

    testDataOperations.get(FkKeyType.TRUSTMARK, null, HttpStatus.OK, SKATT);

    testDataOperations.createTrustMark(
        tmId,
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.createTrustMark(
        tmId,
        SKATT,
        HttpStatus.CONFLICT,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.createTrustMark(
        tmId,
        AF,
        HttpStatus.NOT_FOUND,
        TestDataOperations.defaultTrustMark(tmiId1));

    testDataOperations.delete(FkKeyType.TRUSTMARK,
        tmId,
        HttpStatus.NOT_FOUND,
        AF);

    testDataOperations.delete(FkKeyType.TRUSTMARK,
        tmId,
        HttpStatus.OK,
        SKATT);

    testDataOperations.get(FkKeyType.TRUSTMARK,
        tmId, HttpStatus.NOT_FOUND, SKATT);

  }

  @Test
  @DisplayName("CRUD TrustMarkSubject - should succeed")
  public void testCRUDTrustMarkSubject() {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        OptionsTestData.HostedEntityTestData.create(SKATT)
            .build());

    final UUID tmiId = testDataOperations.createTMI(UUID.randomUUID(),
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));

    final UUID tmId = UUID.randomUUID();

    final UUID tmID = testDataOperations.createTrustMark(
        tmId,
        SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMark(tmiId));

    final UUID trustmarksubjectid =
        testDataOperations.createTrustMarkSubject(UUID.randomUUID(),
            SKATT,
            HttpStatus.CREATED,
            TestDataOperations.defaultTrustMarkSubject(tmID));

    final OptionsRecord optionsRecord = testDataOperations.get(FkKeyType.TRUSTMARKSUBJECT, trustmarksubjectid,
        HttpStatus.OK, SKATT);

    assertThat(optionsRecord.getOption()).isNotEmpty();
  }
}
