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
package se.swedenconnect.oidf.registry.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fails application startup unless the resource server checks the issuer of access tokens fetched from a JWK set.
 * <p>
 * The issuer check itself is performed by Spring Boot, which adds a {@code JwtIssuerValidator} to the
 * {@code JwtDecoder} when {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} is set. With only
 * {@code jwk-set-uri} the {@code iss} claim is not checked at all, so any token signed by a key in that JWK set is
 * accepted, whatever issuer it claims. That failure mode is invisible at runtime, so it is turned into a startup
 * failure here instead.
 * <p>
 * {@code issuer-uri} can not be combined with {@code public-key-location} without {@code jwk-set-uri}: Spring Boot then
 * ignores the public key and fetches the keys through OpenID Connect discovery at the issuer instead, which also fails
 * startup here.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Configuration
public class ResourceServerIssuerStartupCheck implements InitializingBean {

  static final String ISSUER_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.issuer-uri";
  static final String JWK_SET_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";
  static final String PUBLIC_KEY_LOCATION_PROPERTY = "spring.security.oauth2.resourceserver.jwt.public-key-location";

  private final Environment environment;

  /**
   * Constructor.
   *
   * @param environment to read the resource server configuration from
   */
  public ResourceServerIssuerStartupCheck(final Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    final boolean hasIssuer = this.hasText(ISSUER_URI_PROPERTY);
    final boolean hasJwkSetUri = this.hasText(JWK_SET_URI_PROPERTY);

    if (hasJwkSetUri && !hasIssuer) {
      throw new IllegalStateException(
          "%s must be set when %s is used. Without it the 'iss' claim of access tokens is not checked, meaning "
              .formatted(ISSUER_URI_PROPERTY, JWK_SET_URI_PROPERTY)
              + "tokens from any issuer that signs with a key in the JWK set are accepted by this registry.");
    }
    if (hasIssuer && !hasJwkSetUri && this.hasText(PUBLIC_KEY_LOCATION_PROPERTY)) {
      throw new IllegalStateException(
          "%s can not be combined with %s. Spring Boot would ignore the public key and fetch the keys from the issuer "
              .formatted(ISSUER_URI_PROPERTY, PUBLIC_KEY_LOCATION_PROPERTY)
              + "instead. Use %s together with %s.".formatted(JWK_SET_URI_PROPERTY, ISSUER_URI_PROPERTY));
    }

    if (hasIssuer) {
      log.info("Resource server accepts access tokens issued by {}", this.environment.getProperty(ISSUER_URI_PROPERTY));
    }
  }

  private boolean hasText(final String property) {
    final String value = this.environment.getProperty(property);
    return value != null && !value.isBlank();
  }
}
