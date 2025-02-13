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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static se.swedenconnect.oidf.entity.registry.audit.RegistryAuditEventType.OPTIONS_CREATED;
import static se.swedenconnect.oidf.entity.registry.audit.RegistryAuditEventType.OPTIONS_DELETED;

/**
 * Testing the new optional api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OptionsApiControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private AuditEventRepository auditEventRepository;

  final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testOptionsRequest() {

    final ResponseEntity<String> tmi =
        this.restTemplate.getForEntity("/registry/v1/options/trustmarkissuer", String.class);
    if (tmi.getStatusCode().isError()) {
      log.info(tmi.getBody());
    }
    assertThat(tmi.getStatusCode()).isEqualTo(HttpStatus.OK);

    final ResponseEntity<String> ta =
        this.restTemplate.getForEntity("/registry/v1/options/trustanchor", String.class);
    if (ta.getStatusCode().isError()) {
      log.info(ta.getBody());
    }
    assertThat(ta.getStatusCode()).isEqualTo(HttpStatus.OK);

    final ResponseEntity<String> im =
        this.restTemplate.getForEntity("/registry/v1/options/intermediate", String.class);
    if (ta.getStatusCode().isError()) {
      log.info(im.getBody());
    }
    assertThat(im.getStatusCode()).isEqualTo(HttpStatus.OK);

    final ResponseEntity<String> resolver =
        this.restTemplate.getForEntity("/registry/v1/options/resolver", String.class);
    if (ta.getStatusCode().isError()) {
      log.info(resolver.getBody());
    }
    assertThat(resolver.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testCRUDTrustMarkIssuer() throws IOException {

    final String id = TestDataOperations.createTMI(restTemplate);

    final ResponseEntity<String> tmiRead =
        this.restTemplate.getForEntity("/registry/v1/options/trustmarkissuer/" + id, String.class);
    if (tmiRead.getStatusCode().isError()) {
      log.info(tmiRead.getBody());
    }
    assertThat(tmiRead.getStatusCode()).isEqualTo(HttpStatus.OK);

    this.restTemplate.delete("/registry/v1/options/trustmarkissuer/" + id);
    final ResponseEntity<String> tmiReadNotFound =
        this.restTemplate.getForEntity("/registry/v1/options/trustmarkissuer/" + id, String.class);
    if (tmiReadNotFound.getStatusCode().isError()) {
      log.info(tmiReadNotFound.getBody());
    }
    assertThat(tmiReadNotFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    assertFalse(auditEventRepository.find(null, null, OPTIONS_CREATED.name()).isEmpty());
    assertFalse(auditEventRepository.find(null, null, OPTIONS_DELETED.name()).isEmpty());


  }

  @Test
  public void testCRUDPolicies() throws IOException {

    final String id = TestDataOperations.createPolicies(restTemplate);

    final ResponseEntity<String> read =
        this.restTemplate.getForEntity("/registry/v1/options/policies/" + id, String.class);
    if (read.getStatusCode().isError()) {
      log.info(read.getBody());
    }
    assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);

    this.restTemplate.delete("/registry/v1/options/policies/" + id);
    final ResponseEntity<String> readNotFound =
        this.restTemplate.getForEntity("/registry/v1/options/policies/" + id, String.class);
    if (readNotFound.getStatusCode().isError()) {
      log.info(readNotFound.getBody());
    }
    assertThat(readNotFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

  }

  @Test
  public void testList() throws IOException {
    TestDataOperations.createPolicies(restTemplate);
    TestDataOperations.createTMI(restTemplate);
    TestDataOperations.createRESOLVER(restTemplate);
    TestDataOperations.createTA(restTemplate);

    final ResponseEntity<String> response = this.restTemplate.getForEntity(
        "/registry/v1/options/list", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    final JsonNode responseBody = objectMapper.readTree(response.getBody());
    assertThat(responseBody).isNotNull();

    assertThat(responseBody.has("POLICIES")).isTrue();
    assertThat(responseBody.get("POLICIES").size()).isGreaterThan(0);
    assertThat(responseBody.get("POLICIES").elements().hasNext()).isTrue();

    assertThat(responseBody.has("RESOLVER")).isTrue();
    assertThat(responseBody.get("RESOLVER").size()).isGreaterThan(0);
    assertThat(responseBody.get("RESOLVER").elements().hasNext()).isTrue();

    assertThat(responseBody.has("TRUSTANCHOR")).isTrue();
    assertThat(responseBody.get("TRUSTANCHOR").size()).isGreaterThan(0);
    assertThat(responseBody.get("TRUSTANCHOR").elements().hasNext()).isTrue();

    assertThat(responseBody.has("TRUSTMARKISSUER")).isTrue();
    assertThat(responseBody.get("TRUSTMARKISSUER").size()).isGreaterThan(0);
    assertThat(responseBody.get("TRUSTMARKISSUER").elements().hasNext()).isTrue();

  }
  @Test
  public void testCRUDTrustMark() throws IOException {

    TestDataOperations.createTMI(restTemplate);
    final ResponseEntity<String> tm =
        this.restTemplate.getForEntity("/registry/v1/options/trustmark", String.class);
    if (tm.getStatusCode().isError()) {
      log.info(tm.getBody());
    }
    assertThat(tm.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = this.objectMapper.readTree(tm.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "trust-mark-entity-id", () -> "http://www.swedenconnect.se/trustmark1");
      ifThen(valueNode, "trustmarkissuer_id", () -> valueNode.get("options").elements().next().get("key").asText());
    });

    final String newTMI = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdTMI =
        this.restTemplate.postForEntity("/registry/v1/options/trustmark/" + id, new HttpEntity<>(newTMI, headers),
            String.class);
    if (createdTMI.getStatusCode().isError()) {
      log.info(createdTMI.getBody());
    }
    assertThat(createdTMI.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> createdTMITwice =
        this.restTemplate.postForEntity("/registry/v1/options/trustmark/" + id, new HttpEntity<>(newTMI, headers),
            String.class);
    if (createdTMITwice.getStatusCode().isError()) {
      log.info(createdTMITwice.getBody());
    }
    assertThat(createdTMITwice.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "trust-mark-entity-id", () -> "http://www.swedenconnect.se/trustmarkUpdated");
      ifThen(valueNode, "trustmarkissuer_id", () -> valueNode.get("options").elements().next().get("key").asText());
    });

    final String updatedTM = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    this.restTemplate.put("/registry/v1/options/trustmark/" + id, new HttpEntity<>(updatedTM, headers),
        String.class);

    final ResponseEntity<String> tmRead =
        this.restTemplate.getForEntity("/registry/v1/options/trustmark/" + id, String.class);
    if (tmRead.getStatusCode().isError()) {
      log.info(tmRead.getBody());
    }
    assertThat(tmRead.getStatusCode()).isEqualTo(HttpStatus.OK);

    this.restTemplate.delete("/registry/v1/options/trustmark/" + id);
    final ResponseEntity<String> tmReadNotFound =
        this.restTemplate.getForEntity("/registry/v1/options/trustmark/" + id, String.class);
    if (tmReadNotFound.getStatusCode().isError()) {
      log.info(tmReadNotFound.getBody());
    }
    assertThat(tmReadNotFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

  }

  private void ifThen(ObjectNode valueNode, String key, Supplier<String> value) {
    if (valueNode.get("key").asText().contains(key)) {
      valueNode.put("value", value.get());
    }
  }

  @Test
  public void teatBadReq() {
    final ResponseEntity<String> not_found =
        this.restTemplate.getForEntity("/registry/v1/options/somethingNotExisting", String.class);
    if (not_found.getStatusCode().isError()) {
      log.info(not_found.getBody());
    }
    assertThat(not_found.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

  }
}
