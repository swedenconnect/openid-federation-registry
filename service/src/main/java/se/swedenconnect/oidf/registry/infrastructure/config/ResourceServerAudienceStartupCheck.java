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
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Fails application startup unless the resource server is configured with at least one expected audience.
 * <p>
 * The audience check itself is performed by Spring Boot, which adds a {@code JwtAudienceValidator} to the
 * {@code JwtDecoder} when {@code spring.security.oauth2.resourceserver.jwt.audiences} is set. When the property is
 * absent, Boot silently builds a decoder that validates the signature and {@code exp}/{@code nbf} only, so <em>any</em>
 * token signed by the configured key — including tokens issued for a different resource server — is accepted. That
 * failure mode is invisible at runtime, so it is turned into a startup failure here instead.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
public class ResourceServerAudienceStartupCheck implements InitializingBean {

  static final String AUDIENCES_PROPERTY = "spring.security.oauth2.resourceserver.jwt.audiences";

  private final Environment environment;

  /**
   * Constructor.
   *
   * @param environment to read the resource server configuration from
   */
  public ResourceServerAudienceStartupCheck(final Environment environment) {
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    final List<String> audiences = Binder.get(this.environment)
        .bind(AUDIENCES_PROPERTY, Bindable.listOf(String.class))
        .orElseGet(List::of)
        .stream()
        .filter(audience -> audience != null && !audience.isBlank())
        .toList();

    if (audiences.isEmpty()) {
      throw new IllegalStateException(
          "%s must be set to at least one non-empty value. Without it access tokens are accepted regardless of the "
              .formatted(AUDIENCES_PROPERTY)
              + "'aud' claim, meaning tokens issued for another resource server are accepted by this registry.");
    }

    log.info("Resource server accepts access tokens with audience {}", audiences);
  }
}
