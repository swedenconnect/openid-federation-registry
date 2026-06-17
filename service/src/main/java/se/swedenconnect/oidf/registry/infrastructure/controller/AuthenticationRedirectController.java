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

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for selecting authentication.
 *
 * @author Felix Hellman
 */
@Hidden
@Slf4j
@RestController
public class AuthenticationRedirectController {
  private final InMemoryClientRegistrationRepository registrations;
  private final RegistryProperties registryProperties;

  /**
   * Constructor for the AuthenticationRedirectController.
   *
   * @param registrations an instance of {@code InMemoryClientRegistrationRepository} used to manage client
   *     registrations for the authentication flow.
   * @param registryProperties registry configuration, including optional clientRegistrationId.
   */
  public AuthenticationRedirectController(final InMemoryClientRegistrationRepository registrations,
      final RegistryProperties registryProperties) {
    this.registrations = registrations;
    this.registryProperties = registryProperties;
  }

  /**
   * Validates at startup that the configured clientRegistrationId is consistent with the registered OAuth2 clients.
   * Prevents startup if more than one ClientRegistration exists without an explicit selection.
   */
  @PostConstruct
  void validateClientRegistration() {
    final List<ClientRegistration> all = new ArrayList<>();
    this.registrations.iterator().forEachRemaining(all::add);

    if (all.isEmpty()) {
      throw new IllegalStateException("No OAuth2 ClientRegistrations are configured.");
    }

    if (all.size() > 1 && !StringUtils.hasText(this.registryProperties.clientRegistrationId())) {
      final List<String> ids = all.stream().map(ClientRegistration::getRegistrationId).toList();
      throw new IllegalStateException(
          "Multiple OAuth2 ClientRegistrations found but "
              + "'openid.federation.registry.client-registration-id' is not configured. "
              + "Available registration IDs: " + ids);
    }
  }

  /**
   * Handles redirection during an authentication flow. Uses the configured
   * {@code openid.federation.registry.client-registration-id} to select the OAuth2 client, or falls back to the
   * single registered client when only one exists.
   *
   * @param request the HTTP request (reserved for future use of a "continue" parameter)
   * @return a {@code RedirectView} pointing to the OAuth2 authorization endpoint for the selected registration.
   * @throws IllegalArgumentException if the configured registration ID does not match any registered client.
   */
  @GetMapping("/authenticate")
  RedirectView getRedirect(final HttpServletRequest request) {
    final String registrationId = StringUtils.hasText(this.registryProperties.clientRegistrationId())
        ? this.registryProperties.clientRegistrationId()
        : this.registrations.iterator().next().getRegistrationId();

    final ClientRegistration clientRegistration = this.registrations.findByRegistrationId(registrationId);
    if (clientRegistration == null) {
      throw new IllegalArgumentException(
          "No ClientRegistration found for id: '" + registrationId + "'");
    }

    final String redirect = "./oauth2/authorization/" + clientRegistration.getRegistrationId();
    log.debug("Redirecting login request to '{}' for clientRegistrationId '{}'",
        redirect, clientRegistration.getRegistrationId());
    return new RedirectView(redirect);
  }


}
