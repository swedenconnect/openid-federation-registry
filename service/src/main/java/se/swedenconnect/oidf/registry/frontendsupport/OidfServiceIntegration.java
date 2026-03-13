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
package se.swedenconnect.oidf.registry.frontendsupport;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;

import java.net.URI;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
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
   * Configures and retrieves an entity statement for the provided EntityID.
   *
   * @param entityId the entity ID used to construct the URI for the entity statement.
   * @return an EntityStatement object containing the configured entity statement.
   */
  public EntityStatement entityConfiguration(final EntityID entityId) {
    this.auditLogger.resolveJwks(entityId);
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(entityId.toURI())
        .path("/.well-known/openid-federation");
    final URI uri = uriBuilder.build().toUri();
    return this.callEntityStatement(uri);
  }

  private EntityStatement callEntityStatement(final URI uri) {

    log.debug("Getting entityStatement from: {}", uri);
    try {
      final ResponseEntity<String> response = this.restClient.get()
          .uri(uri)
          .retrieve()
          .toEntity(String.class);
      final String responseBody = Objects.requireNonNull(response.getBody());
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
