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

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformation;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.auth.RegistryJwtConverter;

import java.util.Collection;

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

  /**
   * Security filter chain for REST API endpoints. Uses JWT Bearer token authentication (OAuth2 Resource Server).
   * Stateless — no HTTP session is created.
   */
  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http,
      final Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter) {
    http
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter(customJwtAuthenticationConverter))
        )
        .csrf(AbstractHttpConfigurer::disable)

        .oauth2Login(login -> login
                .loginPage("/")
                .defaultSuccessUrl("/", true)
                .failureHandler((request, response, exception) -> {
                  log.error("Authentication failed", exception);
                  response.sendRedirect("/?errorMsg=backend.login.failed");
                })

            // This will be used to mapp the authority to Malids authorication setup.
            //.userInfoEndpoint(uuid ->
            //    uuid.userAuthoritiesMapper(authorities ->
            //        authorities.stream().toList()))

        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/")
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID", "SESSION")
        )

        .authorizeHttpRequests(auth -> auth
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

            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/entities/federation/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/subordinates/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")

            .requestMatchers(HttpMethod.GET, "/registry/v1/entityconfiguration/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entityconfiguration/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")

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

  /**
   * The RegistryClaims class extends JwtAuthenticationToken and provides additional information about the authenticated
   * client, including the associated organization and domain prefix.
   */
  @Getter
  public static class RegistryClaims extends JwtAuthenticationToken {
    private final OrganizationInformation organizationInformation;

    /**
     * Constructs a new instance of the RegistryClaims class.
     *
     * @param jwt the JWT object representing the JSON Web Token used for authentication and authorization.
     * @param information an instance of OrganizationInformation
     * @param userName to be used JwtAuthenticationToken
     * @param authorities to be used
     */
    public RegistryClaims(final Jwt jwt,
        final OrganizationInformation information,
        final String userName,
        final Collection<? extends GrantedAuthority> authorities) {
      super(jwt, authorities, userName);
      this.organizationInformation = information;
    }

    /**
     * Retrieves an {@link OrganizationRecord} that matches the given organization number from the list of available
     * organizations. If no matching record is found, an exception is thrown.
     *
     * @param orgNumber the unique identifier of the organization for which the record is to be retrieved
     * @return the {@link OrganizationRecord} corresponding to the specified organization number
     */
    public OrganizationRecord getOrganizationRecordByOrgNumber(@NonNull final String orgNumber) {
      return this.organizationInformation
          .organizations()
          .stream()
          .filter(e -> e.orgNumber().equals(orgNumber))
          .findFirst()
          .orElseThrow();
    }
  }

}