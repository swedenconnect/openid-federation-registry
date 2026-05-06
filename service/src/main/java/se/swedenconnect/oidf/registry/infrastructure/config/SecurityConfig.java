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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryJwtConverter;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient.RegistryOidcUser;

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
@Slf4j
public class SecurityConfig {


  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http, final OidcUserService oidcUserService,
      final ClientRegistrationRepository clientRegistrationRepository) {

    final OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
        new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

    http
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter(this.customJwtAuthenticationConverter()))
        )
        .csrf(AbstractHttpConfigurer::disable)

        .oauth2Login(login -> login
                .loginPage("/")
                .defaultSuccessUrl("/", true)
                .failureHandler((request, response, exception) -> {
                  log.error("Authentication failed", exception);
                  response.sendRedirect("/login?errorcode=backend.login.failed");
                })
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(oidcUserService)
            )
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(logoutSuccessHandler)
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID", "SESSION")
        )

        .authorizeHttpRequests(auth -> auth
            // Hosted entities
            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/hosted/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/hosted/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entities/hosted/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/hosted/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/entities/hosted/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/hosted/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/entities/hosted/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/hosted/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/hosted/read")
            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/read")
            // Federation entities
            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            // FederaionModules
            .requestMatchers(HttpMethod.GET, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            // Trustmark
            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")
            // Trustmark Subjects
            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")

            // Subordinates
            .requestMatchers(HttpMethod.GET, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            // GUI Services
            .requestMatchers(HttpMethod.GET, "/registry/v1/entityconfiguration/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entityconfiguration/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            // Registration-Flows
            .requestMatchers(HttpMethod.GET, "/registration-flow/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/read")
            .requestMatchers(HttpMethod.POST, "/registration-flow/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.PUT, "/registration-flow/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.DELETE, "/registration-flow/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            // Registration
            .requestMatchers(HttpMethod.GET, "/registration/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/registration/read")
            .requestMatchers(HttpMethod.POST, "/registration/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/registration/write")
            .requestMatchers(HttpMethod.PUT, "/registration/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/registration/write")
            .requestMatchers(HttpMethod.DELETE, "/registration/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/registration/write")

            // Registration-Admin
            .requestMatchers(HttpMethod.GET, "/registration-admin/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/read")
            .requestMatchers(HttpMethod.POST, "/registration-admin/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.PUT, "/registration-admin/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.DELETE, "/registration-admin/v1/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")


            .requestMatchers(HttpMethod.GET, "/logout/frontchannel").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/federationservice/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/*").permitAll()

            .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
            .authenticated()
            .requestMatchers(HttpMethod.GET, "/userinfo").authenticated()
            .requestMatchers(HttpMethod.PUT, "/userinfo").authenticated()

            .anyRequest().denyAll()
        );
    return http.build();
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter() {
    return new RegistryJwtConverter();
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