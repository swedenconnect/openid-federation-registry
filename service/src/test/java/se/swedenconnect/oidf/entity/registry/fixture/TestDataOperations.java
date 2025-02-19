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
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.List;
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

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static String createPolicies(TestRestTemplate restTemplate) throws JsonProcessingException {
    final ResponseEntity<String> tmi =
        restTemplate.getForEntity("/registry/v1/options/policies", String.class);
    if (tmi.getStatusCode().isError()) {
      log.info(tmi.getBody());
    }
    assertThat(tmi.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(tmi.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "name", () -> "Ena-Policy");
      ifThen(valueNode, "policy", () -> "{\"signature\":\"HS256\"}");
      ifThen(valueNode, "organization_id", () -> valueNode.get("options").elements().next().get("key").asText());
    });

    final String newTMI = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdTMI =
        restTemplate.postForEntity("/registry/v1/options/policies/" + id, new HttpEntity<>(newTMI, headers),
            String.class);
    if (createdTMI.getStatusCode().isError()) {
      log.info(createdTMI.getBody());
    }
    assertThat(createdTMI.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return id;
  }

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

  public static String createTrustMark(TestRestTemplate restTemplate, UUID trustMarkIssuerId)
      throws JsonProcessingException {
    final ResponseEntity<String> response =
        restTemplate.getForEntity("/registry/v1/options/trustmark", String.class);
    if (response.getStatusCode().isError()) {
      log.info(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(response.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "trust-mark-entity-id", () -> "http://tmi.swedenconnect.se/loa3");
      ifThen(valueNode, "ref-uri", () -> "http://doc.swedenconnect.se/loa3");
      ifThen(valueNode, "logo-uri", () -> "http://www.swedenconnect.se/image.png");
      ifThen(valueNode, "trustmarkissuer_id", () -> trustMarkIssuerId.toString());
    });

    final String newResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdResponse =
        restTemplate.postForEntity("/registry/v1/options/trustmark/" + id, new HttpEntity<>(newResponse, headers),
            String.class);
    if (createdResponse.getStatusCode().isError()) {
      log.info(createdResponse.getBody());
    }
    assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return id;
  }

  public static String createTA(TestRestTemplate restTemplate) throws JsonProcessingException {
    final ResponseEntity<String> ta =
        restTemplate.getForEntity("/registry/v1/options/trustanchor", String.class);
    if (ta.getStatusCode().isError()) {
      log.info(ta.getBody());
    }
    assertThat(ta.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(ta.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "active", () -> "true");
      ifThen(valueNode, "entity-identifier", () -> "http://www.swedenconnect.se/trustanchor");
      ifThen(valueNode, "alias", () -> "ta");
      ifThen(valueNode, "instance_id", () -> valueNode.get("options").elements().next().get("key").asText());
    });

    final String newta = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdta =
        restTemplate.postForEntity("/registry/v1/options/trustanchor/" + id, new HttpEntity<>(newta, headers),
            String.class);
    if (createdta.getStatusCode().isError()) {
      log.info(createdta.getBody());
    }
    assertThat(createdta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return id;
  }

  public static String createRESOLVER(TestRestTemplate restTemplate) throws JsonProcessingException {
    final ResponseEntity<String> resolver =
        restTemplate.getForEntity("/registry/v1/options/resolver", String.class);
    if (resolver.getStatusCode().isError()) {
      log.info(resolver.getBody());
    }
    assertThat(resolver.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(resolver.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "active", () -> "true");
      ifThen(valueNode, "entity-identifier", () -> "http://www.swedenconnect.se/resolver");
      ifThen(valueNode, "alias", () -> "resolver");
      ifThen(valueNode, "instance_id", () -> valueNode.get("options").elements().next().get("key").asText());
      ifThen(valueNode, "trust-anchor", () -> "http://www.swedenconnect.se/trustanchor");
      ifThen(valueNode, "trusted-keys", () -> new JWKSet(List.of(genKey(), genKey())).toString());
    });

    final String newModule = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final String id = UUID.randomUUID().toString();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    final ResponseEntity<String> createdresolver =
        restTemplate.postForEntity("/registry/v1/options/resolver/" + id, new HttpEntity<>(newModule, headers),
            String.class);
    if (createdresolver.getStatusCode().isError()) {
      log.info(createdresolver.getBody());
    }
    assertThat(createdresolver.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return id;
  }

  private static void ifThen(ObjectNode valueNode, String key, Supplier<String> value) {
    if (valueNode.get("key").asText().contains(key)) {
      valueNode.put("value", value.get());
    }
  }

  public static JWK genKey() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
      keyGen.initialize(Curve.P_256.toECParameterSpec());

      final KeyPair keyPair = keyGen.generateKeyPair();

      // Ange ett unikt kid (Key ID)
      return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
          .privateKey(keyPair.getPrivate())
          .keyID("ec-key-id") // Ange ett unikt kid (Key ID)
          .build();
    }
    catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }

  }
}
