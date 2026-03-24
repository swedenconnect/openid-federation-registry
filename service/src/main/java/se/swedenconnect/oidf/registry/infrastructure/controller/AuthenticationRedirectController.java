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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for selecting authentication.
 *
 * @author Felix Hellman
 */
@Slf4j
@RestController
public class AuthenticationRedirectController {
  private final InMemoryClientRegistrationRepository registrations;

  /**
   * Constructor for the AuthenticationRedirectController.
   *
   * @param registrations an instance of {@code InMemoryClientRegistrationRepository} used to manage client
   *     registrations for the authentication flow.
   */
  public AuthenticationRedirectController(final InMemoryClientRegistrationRepository registrations) {
    this.registrations = registrations;
  }

  /**
   * Handles redirection during an authentication flow. The method constructs a redirect URL based on an OAuth2 client
   * registration identifier and an optional "continue" parameter.
   *
   * @param request the HTTP request containing parameters for the authentication process. It requires: - "reg": the
   *     client registration identifier. This parameter is mandatory. - "continue" (optional): a relative path
   *     indicating where to redirect after authorization. If provided, it must start with "/".
   * @return a {@code RedirectView} constructed with the redirect URL pointing to the OAuth2 authorization endpoint. If
   *     the "continue" parameter is valid, it is appended as a query parameter.
   * @throws IllegalArgumentException if the required "reg" parameter is not provided, or if a client registration
   *     with the given identifier cannot be found.
   */
  @GetMapping("/authenticate")
  RedirectView getRedirect(final HttpServletRequest request) {
    final String reg = request.getParameter("reg");
    if (!StringUtils.hasText(reg)) {
      throw new IllegalArgumentException("No registration id provided");
    }

    final ClientRegistration clientRegistration = this.registrations.findByRegistrationId(reg);
    if (clientRegistration == null) {
      throw new IllegalArgumentException("No registration found for id: " + reg);
    }

    final StringBuilder redirect = new StringBuilder("./oauth2/authorization/").append(reg);

    final String cont = request.getParameter("continue");
    if (StringUtils.hasText(cont) && cont.startsWith("/")) {
      redirect.append("?continue=").append(cont);
    }

    return new RedirectView(redirect.toString());
  }


}
