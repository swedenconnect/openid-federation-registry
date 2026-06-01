/*
 * Copyright 2026 Sweden Connect
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
package se.swedenconnect.oidf.registry.guioperations;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;

import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OIDF Service Integration
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OidfServiceIntegration {

  private final RestClient restClient;
  private final RegistryAuditService auditLogger;
  final static String ENTITY_STATEMENT_JWT = "entity-statement+jwt";

  /**
   * Constructs a new {@code OidfServiceIntegration} instance. When getting the restclient builder there is a clean
   * restclient that is only configured with trustbundles if needed
   *
   * @param restClient the REST client used to interact with external services
   * @param auditLogger autditlogger
   */
  public OidfServiceIntegration(@Qualifier("jwksLoaderRestClient") final RestClient restClient,
      final RegistryAuditService auditLogger) {
    this.restClient = restClient;
    this.auditLogger = auditLogger;

  }

  /**
   * Fetches the hosted JWKS from an oidf-service-node's {@code /jwk} endpoint.
   * <p>
   * The endpoint returns a self-signed JWT whose payload contains {@code federation.keys} and
   * {@code hosted.keys}. This method verifies the signature using the keys carried in the JWT
   * itself and returns only the {@code hosted} key set.
   *
   * @param baseUrl the base URL of the service node (e.g. {@code https://oidf-node.example.com})
   * @return the {@code hosted} {@link JWKSet} from the service node
   */
  @SuppressWarnings("unchecked")
  public JWKSet fetchHostedJwksFromServiceNode(final URI baseUrl) {
    final URI jwkUri = UriComponentsBuilder.fromUri(baseUrl).path("/jwks").build().toUri();
    log.debug("Fetching hosted JWKS from service node: {}", jwkUri);
    try {
      final ResponseEntity<String> response = this.restClient.get()
          .uri(jwkUri)
          .retrieve()
          .toEntity(String.class);
      final String body = response.getBody();
      if (body == null) {
        throw new IllegalArgumentException("Empty response from /jwks endpoint: " + jwkUri);
      }

      final SignedJWT jwt = SignedJWT.parse(body);
      final Map<String, Object> payload = jwt.getPayload().toJSONObject();

      // Collect keys from both sections for self-verification
      final List<JWK> allKeys = new ArrayList<>();
      for (final String section : List.of("federation", "hosted")) {
        final Object sectionRaw = payload.get(section);
        if (sectionRaw instanceof Map<?, ?> sectionMap) {
          final Object keysRaw = sectionMap.get("keys");
          if (keysRaw != null) {
            allKeys.addAll(JWKSet.parse(new JSONObject(Map.of("keys", keysRaw))).getKeys());
          }
        }
      }

      final String kid = jwt.getHeader().getKeyID();
      final JWK signingKey = allKeys.stream()
          .filter(k -> kid.equals(k.getKeyID()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
              "No key for kid '" + kid + "' in /jwks response from: " + jwkUri));

      final JWSVerifier verifier = switch (signingKey.getKeyType().getValue()) {
        case "EC" -> new ECDSAVerifier(signingKey.toECKey());
        case "RSA" -> new RSASSAVerifier(signingKey.toRSAKey());
        case null, default -> throw new IllegalArgumentException(
            "Unsupported key type: " + signingKey.getKeyType());
      };

      if (!jwt.verify(verifier)) {
        throw new IllegalStateException("Signature verification failed for /jwks response from: " + jwkUri);
      }

      final Object hostedRaw = payload.get("hosted");
      if (!(hostedRaw instanceof Map<?, ?> hostedMap)) {
        throw new IllegalArgumentException("No 'hosted' section in /jwks response from: " + jwkUri);
      }
      final Object hostedKeysRaw = hostedMap.get("keys");
      if (hostedKeysRaw == null) {
        throw new IllegalArgumentException("No 'hosted.keys' in /jwks response from: " + jwkUri);
      }
      final List<JWK> hostedKeys = JWKSet.parse(new JSONObject(Map.of("keys", hostedKeysRaw))).getKeys();
      final Map<String, JWK> deduped = new java.util.LinkedHashMap<>();
      for (final JWK k : hostedKeys) {
        deduped.putIfAbsent(k.getKeyID() != null ? k.getKeyID() : k.toString(), k);
      }
      return new JWKSet(new ArrayList<>(deduped.values()));
    }
    catch (final ParseException | JOSEException e) {
      throw new IllegalArgumentException("Failed to process /jwks response from: " + jwkUri, e);
    }
  }

  /**
   * Configures and retrieves an entity statement for the provided EntityID. On the standard location.
   *
   * @param entityId the entity ID used to construct the URI for the entity statement.
   * @return an EntityStatement object containing the configured entity statement.
   */
  public EntityStatement entityConfigurationOnStandardLocation(final EntityID entityId) {
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(entityId.toURI())
        .path("/.well-known/openid-federation");
    final URI uri = uriBuilder.build().toUri();
    return this.callEntityStatementAndVerifyJwks(uri);
  }

  /**
   * Configures and retrieves an entity statement for the provided EntityID.
   * Validate that JWKS exists and then check that the signature matches with the included keys.
   * if not a IllegalArgumentException is thrown.
   *
   * @param uri URI for the entity statement.
   * @return an EntityStatement object containing the configured entity statement.
   */
  public EntityStatement callEntityStatementAndVerifyJwks(final URI uri) {
    this.auditLogger.resolveJwks(uri);
    log.debug("Loading entityStatement from: {}", uri);
    try {
      final ResponseEntity<String> response = this.restClient.get()
          .uri(uri)
          .retrieve()
          .toEntity(String.class);
      final String responseBody = response.getBody();
      if (responseBody == null) {
        throw new IllegalArgumentException("There is no body in response when loading entity statement from: " + uri);
      }
      final EntityStatement statement = EntityStatement.parse(responseBody);

      final SignedJWT signedJWT = statement.getSignedStatement();
      final JWSHeader header = signedJWT.getHeader();
      final String typ = header.getType().getType();
      if (!typ.equals(ENTITY_STATEMENT_JWT)) {
        throw new IllegalStateException("Unsupported entity statement type: " + typ);
      }

      if (!signedJWT.getPayload().toJSONObject().containsKey("jwks")) {
        throw new IllegalArgumentException("JWKS is missing in entity statement payload");
      }

      @SuppressWarnings("unchecked")
      final Map<String, Object> jwksMap = (Map<String, Object>) signedJWT.getPayload().toJSONObject().get("jwks");
      final JWKSet jwkSet = JWKSet.parse(jwksMap);
      final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
          .keyID(header.getKeyID())
          .build());

      final Optional<JWK> jwk = selector
          .select(jwkSet)
          .stream()
          .findFirst();

      if (jwk.isEmpty()) {
        throw new IllegalArgumentException("Unable to find key for kid: " + header.getKeyID() + " in JWKSet");
      }

      final JWSVerifier verifier = switch (jwk.get().getKeyType().getValue()) {
        case "EC" -> new ECDSAVerifier(jwk.get().toECKey());
        case "RSA" -> new RSASSAVerifier(jwk.get().toRSAKey());
        case null, default -> null;
      };

      if (!signedJWT.verify(verifier)) {
        throw new IllegalStateException("Signature verification failed");
      }

      return statement;
    }
    catch (final com.nimbusds.oauth2.sdk.ParseException | JOSEException | ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
