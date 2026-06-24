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
import se.swedenconnect.oidf.registry.guioperations.dto.JwksPayloadDto;
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
 * @author Felix Hellman
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

  /**
   * Fetches and verifies the signed JWKS JWT from the entity's /jwks endpoint. The signature is verified using the
   * federation keys from the entity's self-signed entity configuration. Parses directly from the raw JWT payload to
   * handle the structure: {@code {"federation": {"keys": [...]}, "hosted": {"keys": [...]}, "name": {...}}}
   *
   * @param entityId the entity ID whose /jwks endpoint to fetch
   * @return parsed and verified JWKS payload DTO
   */
  public JwksPayloadDto fetchServiceKeys(final EntityID entityId) {
    final URI jwksUri = UriComponentsBuilder.fromUri(entityId.toURI())
        .path("/jwks")
        .build()
        .toUri();

    log.debug("Fetching service keys from: {}", jwksUri);

    try {
      final ResponseEntity<String> response = this.restClient.get()
          .uri(jwksUri)
          .retrieve()
          .toEntity(String.class);
      final String responseBody = response.getBody();
      if (responseBody == null) {
        throw new IllegalArgumentException("Empty response from /jwks endpoint: " + jwksUri);
      }

      final SignedJWT signedJWT = SignedJWT.parse(responseBody);
      final JWSHeader header = signedJWT.getHeader();

      if (header.getType() == null || !"JWT".equals(header.getType().getType())) {
        throw new IllegalStateException("Unexpected typ in JWKS JWT, expected JWT but got: " + header.getType());
      }

      // Parse the payload first to extract the keys embedded in the JWT
      final Map<String, Object> payload = signedJWT.getPayload().toJSONObject();

      JWKSet federationJwks = new JWKSet();
      final Object fedClaim = payload.get("federation");
      if (fedClaim instanceof Map<?, ?>) {
        try {
          @SuppressWarnings("unchecked")
          final Map<String, Object> fedMap = (Map<String, Object>) fedClaim;
          federationJwks = JWKSet.parse(fedMap);
        }
        catch (final ParseException e) {
          log.warn("Failed to parse federation JWKS from {}: {}", jwksUri, e.getMessage());
        }
      }

      JWKSet hostedJwks = new JWKSet();
      final Object hostedClaim = payload.get("hosted");
      if (hostedClaim instanceof Map<?, ?>) {
        try {
          @SuppressWarnings("unchecked")
          final Map<String, Object> hostedMap = (Map<String, Object>) hostedClaim;
          hostedJwks = JWKSet.parse(hostedMap);
        }
        catch (final ParseException e) {
          log.warn("Failed to parse hosted JWKS from {}: {}", jwksUri, e.getMessage());
        }
      }

      // Verify signature using the federation keys embedded in the JWT payload
      final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
          .keyID(header.getKeyID())
          .build());

      final Optional<JWK> signingKey = selector.select(federationJwks).stream().findFirst();
      if (signingKey.isEmpty()) {
        throw new IllegalArgumentException(
            "No key with kid '%s' found in federation JWKS payload".formatted(header.getKeyID()));
      }

      final JWSVerifier verifier = switch (signingKey.get().getKeyType().getValue()) {
        case "EC" -> new ECDSAVerifier(signingKey.get().toECKey());
        case "RSA" -> new RSASSAVerifier(signingKey.get().toRSAKey());
        case null, default ->
            throw new IllegalArgumentException("Unsupported key type: " + signingKey.get().getKeyType());
      };

      if (!signedJWT.verify(verifier)) {
        throw new IllegalStateException("Signature verification failed for JWKS JWT from: " + jwksUri);
      }

      @SuppressWarnings("unchecked")
      final Map<String, Object> nameClaim = (Map<String, Object>) payload.get("name");
      final JwksPayloadDto.KeyNames names;
      if (nameClaim != null) {
        @SuppressWarnings("unchecked")
        final List<String> fedNames = (List<String>) nameClaim.getOrDefault("federation", List.of());
        @SuppressWarnings("unchecked")
        final List<String> hostedNames = (List<String>) nameClaim.getOrDefault("hosted", List.of());
        names = new JwksPayloadDto.KeyNames(fedNames, hostedNames);
      }
      else {
        names = JwksPayloadDto.KeyNames.empty();
      }

      final JwksPayloadDto result = new JwksPayloadDto(federationJwks, hostedJwks, names);

      // Emit audit event with all loaded key IDs
      final List<String> allKids = new java.util.ArrayList<>();
      result.federation().getKeys().stream().map(JWK::getKeyID).forEach(allKids::add);
      result.hosted().getKeys().stream().map(JWK::getKeyID).forEach(allKids::add);
      this.auditLogger.loadedServiceKeys(jwksUri, allKids);

      return result;
    }
    catch (final ParseException | JOSEException e) {
      throw new IllegalArgumentException("Failed to parse or verify JWKS JWT from: " + jwksUri, e);
    }
  }

}
