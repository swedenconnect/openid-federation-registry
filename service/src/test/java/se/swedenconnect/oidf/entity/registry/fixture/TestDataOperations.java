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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class TestDataOperations {

  private final TestRestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TestDataOperations(final TestRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public UUID createPolicies(JwtTestUtils.OrganisationType organisationType)
      throws JsonProcessingException {
    final ResponseEntity<String> reply = restTemplate.getForEntity("/registry/v1/options/policies", String.class);
    if (reply.getStatusCode().isError()) {
      log.info(reply.getBody());
    }
    assertThat(reply.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(reply.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      ifThen(valueNode, "name", () -> "Ena-Policy");
      ifThen(valueNode, "policy", () -> "{\"signature\":\"HS256\"}");
    });

    final String newTMI = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    final UUID id = UUID.randomUUID();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organisationType));

    final ResponseEntity<String> createdTMI =
        restTemplate.postForEntity("/registry/v1/options/policies/" + id, new HttpEntity<>(newTMI, headers),
            String.class);
    if (createdTMI.getStatusCode().isError()) {
      log.info(createdTMI.getBody());
    }
    assertThat(createdTMI.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return id;
  }

  public OptionsRecord get(final FkKeyType configGroup, final UUID id,
      final HttpStatus httpStatus, final JwtTestUtils.OrganisationType organizationType) {

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organizationType));

    final HttpEntity<String> entity = new HttpEntity<>(headers);
    String url = "/registry/v1/options/%s/%s".formatted(configGroup, id);
    if (url.endsWith("/null")) {
      url = url.substring(0, url.length() - 5);
    }

    final ResponseEntity<OptionsRecord> reply = restTemplate.exchange(url, HttpMethod.GET, entity, OptionsRecord.class);

    if (reply.getStatusCode().isError()) {
      log.info(String.valueOf(reply.getBody()));
    }
    assertThat(reply.getStatusCode()).isEqualTo(httpStatus);
    return reply.getBody();
  }

  public OptionsRecord post(
      final TestRestTemplate restTemplate,
      final FkKeyType configGroup,
      final UUID id,
      final HttpStatus httpStatus,
      final JwtTestUtils.OrganisationType organizationType,
      final OptionsRecord record) {

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organizationType));

    final HttpEntity<OptionsRecord> entity = new HttpEntity<>(record, headers);

    final ResponseEntity<OptionsRecord> reply =
        restTemplate.exchange("/registry/v1/options/%s/%s".formatted(configGroup, id.toString()), HttpMethod.POST,
            entity,
            OptionsRecord.class);

    assertThat(reply.getStatusCode()).isEqualTo(httpStatus);
    return reply.getBody();
  }

  public void delete(
      final FkKeyType configGroup,
      final UUID id,
      HttpStatus httpStatus,
      final JwtTestUtils.OrganisationType organizationType) {

    final HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organizationType));
    final HttpEntity<String> entity = new HttpEntity<>(headers);

    final ResponseEntity<Void> response =
        restTemplate.exchange("/registry/v1/options/%s/%s".formatted(configGroup, id), HttpMethod.DELETE, entity,
            Void.class);
    assertThat(response.getStatusCode()).isEqualTo(httpStatus);
  }

  public UUID updatePolicies(
      final JwtTestUtils.OrganisationType organisationType,
      final UUID policyId,
      final Map<String, String> dataToUpdate) throws JsonProcessingException {

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organisationType));

    final HttpEntity<String> entity = new HttpEntity<>(headers);

    final ResponseEntity<String> reply =
        restTemplate.exchange("/registry/v1/options/policies/" + policyId, HttpMethod.GET, entity, String.class);

    if (reply.getStatusCode().isError()) {
      log.info(reply.getBody());
    }
    assertThat(reply.getStatusCode()).isEqualTo(HttpStatus.OK);
    final JsonNode node = objectMapper.readTree(reply.getBody());
    final JsonNode data = node.get("option");

    data.elements().forEachRemaining(jsonNode -> {
      final ObjectNode valueNode = (ObjectNode) jsonNode;
      if (dataToUpdate.containsKey(valueNode.get("key").asText())) {
        valueNode.put("value", dataToUpdate.get(valueNode.get("key").asText()));
      }
    });

    final String newTMI = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);

    final ResponseEntity<String> createdTMI =
        restTemplate.exchange("/registry/v1/options/policies/" + policyId, HttpMethod.PUT,
            new HttpEntity<>(newTMI, headers), String.class);
    if (createdTMI.getStatusCode().isError()) {
      log.info(createdTMI.getBody());
    }
    assertThat(createdTMI.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return policyId;
  }

  public UUID createTMI(
      final JwtTestUtils.OrganisationType organisationType) throws JsonProcessingException {

    final FkKeyType groupId = FkKeyType.TRUSTMARKISSUER;
    final OptionsRecord template = get(groupId, null, HttpStatus.OK, organisationType);
    template.getOption().forEach(valueNode -> {
      setIfThen(valueNode, "active", () -> "true");
      setIfThen(valueNode, "entity-identifier", () -> "http://www.swedenconnect.se/issuer");
      setIfThen(valueNode, "alias", () -> "tmi");
    });
    final UUID id = UUID.randomUUID();
    post(restTemplate, groupId, id, HttpStatus.CREATED, organisationType, template);
    return id;
  }

  public static Function<Values, String> defaultTrustMark(UUID trustMarkIssuerId) {
    return s -> switch (s.getKey()) {
      case "trust-mark-entity-id" -> "http://tmi.swedenconnect.se/loa3";
      case "ref-uri" -> "http://doc.swedenconnect.se/loa3";
      case "logo-uri" -> "http://www.swedenconnect.se/image.png";
      case "trustmarkissuer_id" -> trustMarkIssuerId.toString();
      default -> null;
    };
  }

  public static Function<Values, String> defaultTrustAnchor() {
    return s -> switch (s.getKey()) {
      case "active" -> "true";
      case "entity-identifier" -> "http://www.swedenconnect.se/trustanchor";
      case "alias" -> "trustanchor";
      default -> null;
    };
  }

  public UUID createTrustMark(
      final UUID id,
      final JwtTestUtils.OrganisationType organisationType,
      final HttpStatus expectedHttpStatus,
      final Function<Values, String> options) throws JsonProcessingException {
    return create(id, FkKeyType.TRUSTMARK, organisationType, expectedHttpStatus, options);
  }

  protected UUID create(
      UUID id,
      final FkKeyType configGroup,
      final JwtTestUtils.OrganisationType organisationType,
      final HttpStatus expectedHttpStatus,
      final Function<Values, String> overrideSettingValues) {

    final OptionsRecord template = get(configGroup, null, HttpStatus.OK, organisationType);
    template.getOption().forEach(valueNode -> {
      final String value = overrideSettingValues.apply(valueNode);
      if (value != null) {
        valueNode.setValue(value);
      }
    });

    post(restTemplate, configGroup, id, expectedHttpStatus, organisationType, template);
    return id;
  }

  public String createTA() throws JsonProcessingException {
    final ResponseEntity<String> ta = restTemplate.getForEntity("/registry/v1/options/trustanchor", String.class);
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

  public String createRESOLVER() throws JsonProcessingException {
    final ResponseEntity<String> resolver = restTemplate.getForEntity("/registry/v1/options/resolver", String.class);
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

  private static void setIfThen(Values valueNode, String key, Supplier<String> value) {
    if (valueNode.getKey().equals(key)) {
      valueNode.setValue(value.get());
    }
  }

  public static JWK genKey() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
      keyGen.initialize(Curve.P_256.toECParameterSpec());

      final KeyPair keyPair = keyGen.generateKeyPair();

      // Ange ett unikt kid (Key ID)
      return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).privateKey(keyPair.getPrivate())
          .keyID("ec-key-id") // Ange ett unikt kid (Key ID)
          .build();
    }
    catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }

  }

  public JsonNode listAll(final JwtTestUtils.OrganisationType organisationType) throws JsonProcessingException {
    final ResponseEntity<String> response = this.restTemplate.getForEntity(
        "/registry/v1/options/list", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    return objectMapper.readTree(response.getBody());
  }

  public JsonNode listForFKType(FkKeyType fkKeyType, final JwtTestUtils.OrganisationType organisationType)
      throws JsonProcessingException {
    final ResponseEntity<String> response = this.restTemplate.getForEntity(
        "/registry/v1/options/list/" + fkKeyType, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    return objectMapper.readTree(response.getBody());
  }
}
