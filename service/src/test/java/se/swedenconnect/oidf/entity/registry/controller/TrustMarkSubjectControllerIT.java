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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the TrustMarkSubjectController class. This class uses Testcontainers to run a MariaDB container
 * for the tests, and Spring Boot's TestRestTemplate to interact with the API endpoints.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TrustMarkSubjectControllerIT {

  /**
   * A static instance of the MariaDBContainer using version 11.2 of the MariaDB image. This container is managed by the
   * Spring framework with the use of {@code @Container} and {@code @ServiceConnection} annotations. The instance
   * facilitates database management for integration testing by providing an isolated database environment.
   */
  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");
  @Autowired
  private TestRestTemplate restTemplate;

  /**
   * Doing CRUD operations on TrustMarkSubject
   */
  @Test
  public void testCRUD() throws JsonProcessingException {

    final TrustMarkSubjectRecord record = TrustMarkSubjectRecord.builder()
        .trustMarkSubjectRecordId(UUID.randomUUID().toString())
        .issuer("http://www.swedenconnect.se/issuer")
        .trustMarkId("http://www.swedenconnect.se/trustmarkid")
        .subject("http://www.swedenconnect.se/subject")
        .revoked(true)
        .granted(OffsetDateTime.now(ZoneId.of("UTC")))
        .build();

    final ResponseEntity<String> create =
        this.restTemplate.postForEntity("/registry/v1/trustmarksubjects", record, String.class);
    if (create.getStatusCode().isError()) {
      log.info(create.getBody());
    }
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);


    record.setExpires(OffsetDateTime.now(ZoneId.of("UTC")));
    this.restTemplate.put("/registry/v1/trustmarksubjects/" + record.getTrustMarkSubjectRecordId(), record);

    final ResponseEntity<TrustMarkSubjectRecord> read =
        this.restTemplate.getForEntity("/registry/v1/trustmarksubjects/"+record.getTrustMarkSubjectRecordId(),
                TrustMarkSubjectRecord.class);
    assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(record).isNotNull().isEqualTo(read.getBody());

    this.restTemplate.delete("/registry/v1/trustmarksubjects/"+record.getTrustMarkSubjectRecordId());

    final ResponseEntity<TrustMarkSubjectRecord> notFound =
        this.restTemplate.getForEntity("/registry/v1/trustmarksubjects/"+record.getTrustMarkSubjectRecordId(), TrustMarkSubjectRecord.class);
    assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }


  /**
   * Testing indata validation fail
   */
  @Test
  public void testIndataValidationSubjRecID() throws JsonProcessingException {

    final TrustMarkSubjectRecord record = TrustMarkSubjectRecord.builder()
        .trustMarkSubjectRecordId("WrongIdFormat")
        .issuer("http://www.swedenconnect.se/issuer")
        .trustMarkId("http://www.swedenconnect.se/trustmarkid")
        .subject("http://www.swedenconnect.se/subject")
        .revoked(true)
        .granted(OffsetDateTime.now(ZoneId.of("UTC")))
        .build();

    final ResponseEntity<ProblemDetail> create =
        this.restTemplate.postForEntity("/registry/v1/trustmarksubjects", record, ProblemDetail.class);
    if (create.getStatusCode().isError()) {
      log.info(create.getBody().toString());
    }
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    final List<Map<String, String>> reply = (List<Map<String, String>>) create.getBody().getProperties().get("cause");
    assertThat(reply.getFirst().get("field")).contains("trustMarkSubjectRecord.trustMarkSubjectRecordId");
    assertThat(reply.getFirst().get("detail")).contains("must match");

  }

  /**
   * Testing indata validation fail for no subject
   */
  @Test
  public void testIndataValidationNoSubject() throws JsonProcessingException {

    final TrustMarkSubjectRecord record = TrustMarkSubjectRecord.builder()
        .trustMarkSubjectRecordId(UUID.randomUUID().toString())
        .issuer("http://www.swedenconnect.se/issuer")
        .trustMarkId("http://www.swedenconnect.se/trustmarkid")
        .revoked(true)
        .granted(OffsetDateTime.now(ZoneId.of("UTC")))
        .build();
    final ResponseEntity<ProblemDetail> create =
        this.restTemplate.postForEntity("/registry/v1/trustmarksubjects", record, ProblemDetail.class);
    if (create.getStatusCode().isError()) {
      log.info(create.getBody().toString());
    }
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    final List<Map<String, String>> reply = (List<Map<String, String>>) create.getBody().getProperties().get("cause");
    assertThat(reply.getFirst().get("field")).contains("trustMarkSubjectRecord.subject");
  }

  /**
   * Testing indata validation fail for trustmark
   */
  @Test
  public void testIndataValidationTrustMarkId() throws JsonProcessingException {

    final TrustMarkSubjectRecord record = TrustMarkSubjectRecord.builder()
        .trustMarkSubjectRecordId(UUID.randomUUID().toString())
        .issuer("http://www.swedenconnect.se/issuer")
        .trustMarkId("WrongFormat")
        .subject("http://www.swedenconnect.se/subject")
        .revoked(true)
        .granted(OffsetDateTime.now(ZoneId.of("UTC")))
        .build();

    final ResponseEntity<ProblemDetail> create =
        this.restTemplate.postForEntity("/registry/v1/trustmarksubjects", record, ProblemDetail.class);
    if (create.getStatusCode().isError()) {
      log.info(create.getBody().toString());
    }
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    final List<Map<String, String>> reply = (List<Map<String, String>>) create.getBody().getProperties().get("cause");
    assertThat(reply.getFirst().get("field")).contains("trustMarkSubjectRecord.trustMarkId");
    assertThat(reply.getFirst().get("detail")).contains("must match");
  }

  /**
   * Tests the retrieval of all TrustMarkSubject records from the registry.
   * The method validates that the API endpoint for retrieving records responds correctly
   * with a status of HTTP 200 OK and a non-null body containing the expected number of records.
   */
  @Test
  public void testGetAllTrustMarkSubjects() {
    // Arrange
    IntStream.range(10, 15).boxed().forEach(i -> {
      final TrustMarkSubjectRecord trustMarkSubjectRecord = new TrustMarkSubjectRecord.Builder()
          .trustMarkSubjectRecordId(UUID.randomUUID().toString())
          .issuer("https://www.swedenconnect.se/issuer-" + i)
          .subject("https://www.swedenconnect.se/subject-" + i)
          .trustMarkId("https://www.swedenconnect.se/trustmarkid-" + i)
          .revoked(false)
          .granted(OffsetDateTime.now(ZoneId.of("UTC")))
          .expires(OffsetDateTime.now(ZoneId.of("UTC")).plusDays(i))
          .build();
      this.restTemplate.postForEntity("/registry/v1/trustmarksubjects", trustMarkSubjectRecord, TrustMarkSubjectRecord.class);
    });

    // Act
    final ResponseEntity<TrustMarkSubjectRecord[]> response = this.restTemplate.getForEntity("/registry/v1/trustmarksubjects", TrustMarkSubjectRecord[].class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    final TrustMarkSubjectRecord[] trustMarkSubjectRecords = response.getBody();
    assertThat(trustMarkSubjectRecords).isNotNull();
    assertThat(trustMarkSubjectRecords).hasSizeGreaterThanOrEqualTo(5);
  }

}