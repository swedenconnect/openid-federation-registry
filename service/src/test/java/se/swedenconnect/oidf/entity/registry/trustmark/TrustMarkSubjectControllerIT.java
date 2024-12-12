/*
 * Copyright 2024 Sweden Connect.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.oidf.entity.registry.trustmark;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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
  public void testCRUD() {

    final TrustMarkSubjectRecord record = TrustMarkSubjectRecord.builder()
        .trustMarkSubjectRecordId(UUID.randomUUID().toString())
        .issuer("http://www.swedenconnect.se/issuer")
        .trustMarkId("http://www.swedenconnect.se/trustmarkid")
        .subject("http://www.swedenconnect.se/subject")
        .revoked(true)
        .granted(OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC))
        .build();

    final ResponseEntity<String> create =
        this.restTemplate.postForEntity("/registry/v1/trustmarksubject", record, String.class);
    if (create.getStatusCode().isError()) {
      log.info(create.getBody());
    }
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);


    record.setExpires(OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC));
    this.restTemplate.put("/registry/v1/trustmarksubject/"+record.getTrustMarkSubjectRecordId(), record);

    final ResponseEntity<TrustMarkSubjectRecord> read =
        this.restTemplate.getForEntity("/registry/v1/trustmarksubject/"+record.getTrustMarkSubjectRecordId(), TrustMarkSubjectRecord.class);
    assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(record).isNotNull().isEqualTo(read.getBody());

    this.restTemplate.delete("/registry/v1/trustmarksubject/"+record.getTrustMarkSubjectRecordId());

    final ResponseEntity<TrustMarkSubjectRecord> notFound =
        this.restTemplate.getForEntity("/registry/v1/trustmarksubject/"+record.getTrustMarkSubjectRecordId(), TrustMarkSubjectRecord.class);
    assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

}