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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContentSecurityPolicies}.
 */
class ContentSecurityPoliciesTest {

  @Test
  @DisplayName("The GUI policy only allows scripts from the registry itself")
  void guiPolicyAllowsOnlyOwnScripts() {
    assertThat(ContentSecurityPolicies.gui(Set.of()))
        .contains("default-src 'self'; ")
        .contains("script-src 'self'; ")
        .contains("connect-src 'self'; ")
        .contains("object-src 'none'; ")
        .contains("frame-ancestors 'none'; ")
        .endsWith("form-action 'self'")
        .doesNotContain("unsafe-eval")
        .doesNotContain("script-src 'self' 'unsafe-inline'");
  }

  @Test
  @DisplayName("The GUI policy allows forms to be redirected to the given origins")
  void guiPolicyAllowsFormActionOrigins() {
    assertThat(ContentSecurityPolicies.gui(Set.of("https://idp.example.se", "https://other.example.se:8443")))
        .endsWith("form-action 'self' https://idp.example.se https://other.example.se:8443");
  }

  @Test
  @DisplayName("The origins of the end session endpoints of the client registrations are returned")
  void endSessionOriginsAreReturned() {
    final ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(List.of(
        registration("a", Map.of("end_session_endpoint", "https://idp.example.se/realms/a/logout")),
        registration("b", Map.of("end_session_endpoint", "https://idp.example.se:8443/realms/b/logout")),
        registration("c", Map.of("end_session_endpoint", "https://idp.example.se/realms/c/logout")),
        registration("d", Map.of())));

    assertThat(ContentSecurityPolicies.endSessionOrigins(repository))
        .containsExactly("https://idp.example.se", "https://idp.example.se:8443");
  }

  @Test
  @DisplayName("No origins are returned for a repository that can not be iterated")
  void noOriginsForNonIterableRepository() {
    final ClientRegistrationRepository repository = registrationId -> null;

    assertThat(ContentSecurityPolicies.endSessionOrigins(repository)).isEmpty();
  }

  private static ClientRegistration registration(final String id, final Map<String, Object> metadata) {
    return ClientRegistration.withRegistrationId(id)
        .clientId(id)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("https://idp.example.se/auth")
        .tokenUri("https://idp.example.se/token")
        .providerConfigurationMetadata(metadata)
        .build();
  }
}
