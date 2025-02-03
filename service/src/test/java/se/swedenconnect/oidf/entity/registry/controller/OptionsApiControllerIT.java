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

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

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

  final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testCRUD() {

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
  public void testCRUDTMI() throws IOException {

    final ResponseEntity<String> tmi =
        this.restTemplate.getForEntity("/registry/v1/options/trustmarkissuer", String.class);
    if (tmi.getStatusCode().isError()) {
      log.info(tmi.getBody());
    }
    assertThat(tmi.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = this.objectMapper.readTree(tmi.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "active", () -> "true");
      ifThen(valueNode, "entity_identifier", () -> "http://www.swedenconnect.se/issuer");
      ifThen(valueNode, "alias", () -> "tmi");
      ifThen(valueNode, "instance_id", () -> valueNode.get("options").elements().next().get("key").asText());
    });

    final String newTMI = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdTMI =
        this.restTemplate.postForEntity("/registry/v1/options/trustmarkissuer/" + id, new HttpEntity<>(newTMI, headers),
            String.class);
    if (createdTMI.getStatusCode().isError()) {
      log.info(createdTMI.getBody());
    }
    assertThat(createdTMI.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> tmiRead =
        this.restTemplate.getForEntity("/registry/v1/options/trustmarkissuer/" + id, String.class);
    if (tmiRead.getStatusCode().isError()) {
      log.info(tmiRead.getBody());
    }
    assertThat(tmiRead.getStatusCode()).isEqualTo(HttpStatus.OK);


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
