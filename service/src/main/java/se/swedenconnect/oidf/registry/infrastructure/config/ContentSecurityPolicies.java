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

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The Content-Security-Policy values sent by the registry.
 * <p>
 * The GUI policy only allows scripts, styles, fonts and requests from the registry itself. Styles also allow
 * {@code 'unsafe-inline'}, since Vuetify adds its theme as a {@code <style>} element at runtime. The logout form is
 * redirected to the end session endpoint of the OpenID provider, and Chrome applies {@code form-action} to that
 * redirect too, so the origins of those endpoints are allowed as form targets.
 * <p>
 * Swagger UI uses inline scripts and styles, so it gets a milder policy.
 *
 * @author Per Fredrik Plars
 */
public final class ContentSecurityPolicies {

  /** The policy for Swagger UI and the OpenAPI documents. */
  public static final String SWAGGER = "default-src 'self'; "
      + "script-src 'self' 'unsafe-inline'; "
      + "style-src 'self' 'unsafe-inline'; "
      + "img-src 'self' data:; "
      + "object-src 'none'; "
      + "base-uri 'self'; "
      + "frame-ancestors 'none'; "
      + "form-action 'self'";

  private static final String END_SESSION_ENDPOINT = "end_session_endpoint";

  private ContentSecurityPolicies() {
  }

  /**
   * The policy for the GUI and everything else except Swagger UI.
   *
   * @param formActionOrigins origins, besides the registry itself, that forms may be submitted or redirected to
   * @return the policy
   */
  public static String gui(final Collection<String> formActionOrigins) {
    final String formAction = formActionOrigins.isEmpty()
        ? "'self'"
        : "'self' " + String.join(" ", new TreeSet<>(formActionOrigins));
    return "default-src 'self'; "
        + "script-src 'self'; "
        + "style-src 'self' 'unsafe-inline'; "
        + "img-src 'self' data:; "
        + "font-src 'self'; "
        + "connect-src 'self'; "
        + "object-src 'none'; "
        + "base-uri 'self'; "
        + "frame-ancestors 'none'; "
        + "form-action " + formAction;
  }

  /**
   * Returns the origins of the end session endpoints of the client registrations, which the logout is redirected to.
   *
   * @param clientRegistrationRepository the client registrations
   * @return the origins, for example {@code https://idp.example.se}
   */
  public static Set<String> endSessionOrigins(final ClientRegistrationRepository clientRegistrationRepository) {
    if (!(clientRegistrationRepository instanceof final Iterable<?> registrations)) {
      return Set.of();
    }
    return StreamSupport.stream(registrations.spliterator(), false)
        .filter(ClientRegistration.class::isInstance)
        .map(ClientRegistration.class::cast)
        .map(registration -> registration.getProviderDetails().getConfigurationMetadata().get(END_SESSION_ENDPOINT))
        .filter(Objects::nonNull)
        .map(endpoint -> origin(URI.create(endpoint.toString())))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String origin(final URI uri) {
    return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
  }
}
