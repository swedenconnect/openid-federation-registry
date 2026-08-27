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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import se.swedenconnect.iam.security.client.ResourceParameterConverter;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryJwtConverter;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient.RegistryOidcUser;

import java.util.Optional;

/**
 * Security configuration class that defines security-related settings for the application. This class integrates OAuth2
 * Resource Server and configures security rules for specific HTTP endpoints. It uses Spring Security to define the
 * security rules, such as enabling JWT token-based authentication, disabling CSRF for stateless APIs, and specifying
 * role-based access controls for various endpoints.
 *
 * @author Per Fredrik Plars
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {



  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http,
      final ClientRegistrationRepository clientRegistrationRepository,
      final OidcUserService orgRightsOidcUserService,
      final ObjectProvider<RestClientAuthorizationCodeTokenResponseClient> authCodeTokenResponseClient) {

    final OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
        new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

    http
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter(this.customJwtAuthenticationConverter()))
        )
        .csrf(AbstractHttpConfigurer::disable)
        .oauth2Login(login -> {
          login
              .loginPage("/")
              .defaultSuccessUrl("/", true)
              .failureHandler((request, response, exception) -> {
                log.error("Authentication failed", exception);
                response.sendRedirect("/login?errorcode=backend.login.failed");
              })
              .userInfoEndpoint(userInfo -> userInfo
                  .oidcUserService(orgRightsOidcUserService)
              );
          // iam.security.client.credential (private_key_jwt) is rolled out per environment, coordinated with a
          // Keycloak-side client change — fall back to Spring's default (client_secret) token endpoint client
          // wherever it isn't configured yet, rather than failing application startup entirely.
          this.resolvePrivateKeyJwtTokenResponseClient(authCodeTokenResponseClient)
              .ifPresent(client -> login.tokenEndpoint(token -> token.accessTokenResponseClient(client)));
        })
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(logoutSuccessHandler)
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID", "SESSION")
        )
        .authorizeHttpRequests(auth -> auth
            // Registry API — right-level enforced by @PreAuthorize(@orgRightsService) on controllers
            .requestMatchers("/registry/v1/**").authenticated()
            // Registration-Flows — right-level enforced by @PreAuthorize(@orgRightsService) on controllers
            .requestMatchers("/registration-flow/v1/**").authenticated()
            // Registration — open to any authenticated user (join/apply flow)
            .requestMatchers("/registration/v1/**").authenticated()
            // Registration-Admin — right-level enforced by @PreAuthorize(@orgRightsService) on controllers
            .requestMatchers("/registration-admin/v1/**").authenticated()

            .requestMatchers(HttpMethod.GET, "/logout/frontchannel").permitAll()
            .requestMatchers(HttpMethod.GET, "/jwks").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/federationservice/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/entities/**",
                "/registration-flows/**", "/registrations/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/*").permitAll()

            .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
            .authenticated()
            .requestMatchers(HttpMethod.GET, "/userinfo").authenticated()
            .requestMatchers(HttpMethod.GET, "/tenants").authenticated()

            .anyRequest().denyAll()
        );
    return http.build();
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter() {
    return new RegistryJwtConverter();
  }

  /**
   * Resolves the {@code private_key_jwt} token response client, if {@code iam.security.client.credential} is
   * configured. The underlying bean chain (via {@code IamSecurityClientAutoConfiguration}) throws at instantiation time
   * — not bean-definition time — when the credential is missing, so resolution happens here, behind a try/catch, rather
   * than through required constructor/method injection.
   */
  private Optional<RestClientAuthorizationCodeTokenResponseClient> resolvePrivateKeyJwtTokenResponseClient(
      final ObjectProvider<RestClientAuthorizationCodeTokenResponseClient> authCodeTokenResponseClient) {
    try {
      return Optional.ofNullable(authCodeTokenResponseClient.getIfAvailable());
    }
    catch (final RuntimeException e) {
      log.debug("iam.security.client.credential is not configured — using default client authentication", e);
      return Optional.empty();
    }
  }

  /**
   * Token response client for the {@code authorization_code} grant, authenticating to Keycloak's token endpoint with
   * {@code private_key_jwt} instead of a client secret. {@code authCodeJwtConverter} and
   * {@code resourceParameterConverter} are auto-configured by {@code IamSecurityClientAutoConfiguration} from
   * {@code iam.security.client.credential}.
   *
   * @param authCodeJwtConverter signs the {@code client_assertion} sent to the token endpoint
   * @param resourceParameterConverter adds the RFC 8707 {@code resource} parameter, when requested
   * @return the token response client, wired into {@code oauth2Login}'s token endpoint
   */
  @Bean
  RestClientAuthorizationCodeTokenResponseClient authCodeTokenResponseClient(
      final NimbusJwtClientAuthenticationParametersConverter<OAuth2AuthorizationCodeGrantRequest> authCodeJwtConverter,
      final ResourceParameterConverter resourceParameterConverter) {
    final RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
    client.addParametersConverter(authCodeJwtConverter);
    client.addParametersConverter(resourceParameterConverter);
    return client;
  }

  @Bean
  @Primary
  OidcUserService oidcUserService() {
    return new OidcUserService() {
      public OidcUser loadUser(final OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        return new RegistryOidcUser(super.loadUser(userRequest));
      }
    };
  }

}