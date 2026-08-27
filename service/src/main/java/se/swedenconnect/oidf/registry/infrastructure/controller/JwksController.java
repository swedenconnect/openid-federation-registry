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
package se.swedenconnect.oidf.registry.infrastructure.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes this application's own public key as a {@code /jwks} endpoint, so Keycloak can verify the
 * {@code private_key_jwt} client assertions this application signs when authenticating to the token endpoint — see
 * {@code iam.security.client.credential} and
 * {@link se.swedenconnect.iam.security.autoconfigure .IamSecurityClientAutoConfiguration}.
 *
 * <p>Must be permitted without authentication in {@code SecurityConfig} — Keycloak fetches it unauthenticated.
 *
 * <p>{@code iam.security.client.credential} is not configured in every environment yet (the switch to
 * {@code private_key_jwt} is being rolled out gradually, coordinated with a Keycloak-side client change). The
 * underlying {@code oidcClientJwk} bean throws at *instantiation* time, not at bean-definition time, whenever the
 * credential is missing — so resolution is deferred to request time via {@link ObjectProvider} and the endpoint
 * degrades to {@code 404} rather than taking the whole application context down.
 *
 * @author Per Fredrik Plars
 */
@Hidden
@Slf4j
@RestController
public class JwksController {

  private final ObjectProvider<JWK> oidcClientJwk;

  /**
   * Creates a new controller.
   *
   * @param oidcClientJwk this application's client credential, auto-configured from
   *     {@code iam.security.client.credential} when present
   */
  public JwksController(final ObjectProvider<JWK> oidcClientJwk) {
    this.oidcClientJwk = oidcClientJwk;
  }

  /**
   * Returns this application's public key set, or {@code 404} if {@code iam.security.client.credential} isn't
   * configured in this environment.
   *
   * @return the JWK set, as JSON
   */
  @GetMapping("/jwks")
  public ResponseEntity<String> jwks() {
    final JWK jwk;
    try {
      jwk = this.oidcClientJwk.getIfAvailable();
    }
    catch (final RuntimeException e) {
      log.debug("iam.security.client.credential is not configured — /jwks unavailable", e);
      return ResponseEntity.notFound().build();
    }
    if (jwk == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new JWKSet(jwk.toPublicJWK()).toString());
  }
}
