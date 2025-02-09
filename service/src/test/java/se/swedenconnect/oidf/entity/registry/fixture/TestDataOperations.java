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

package se.swedenconnect.oidf.entity.registry.fixture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class TestDataOperations {

  private static ObjectMapper objectMapper = new ObjectMapper();

  public static String createTMI(TestRestTemplate restTemplate) throws JsonProcessingException {
    final ResponseEntity<String> tmi =
        restTemplate.getForEntity("/registry/v1/options/trustmarkissuer", String.class);
    if (tmi.getStatusCode().isError()) {
      log.info(tmi.getBody());
    }
    assertThat(tmi.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(tmi.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "active", () -> "true");
      ifThen(valueNode, "entity-identifier", () -> "http://www.swedenconnect.se/issuer");
      ifThen(valueNode, "alias", () -> "tmi");
      ifThen(valueNode, "instance_id", () -> valueNode.get("options").elements().next().get("key").asText());
      ifThen(valueNode, "jwk",
          () -> "{\"kty\":\"EC\",\"d\":\"RVF_NZ7AJQj5RuFm3YsocqSgWmbMIQxG9WJ2HXd_YPs\",\"crv\":\"P-256\",\"kid\":\"ec-key-id\",\"x\":\"uiLBUCuinEhulOibSNXt6s2O8AelJ-BGU5Yf-r2U4cY\",\"y\":\"_Ifzyx4v0xaVoejfca9_FlYmGp9nm7e4Ie0XJHjbfE0\"}");
    });

    final String newTMI = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdTMI =
        restTemplate.postForEntity("/registry/v1/options/trustmarkissuer/" + id, new HttpEntity<>(newTMI, headers),
            String.class);
    if (createdTMI.getStatusCode().isError()) {
      log.info(createdTMI.getBody());
    }
    assertThat(createdTMI.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return id;
  }

  private static void ifThen(ObjectNode valueNode, String key, Supplier<String> value) {
    if (valueNode.get("key").asText().contains(key)) {
      valueNode.put("value", value.get());
    }
  }
}
