/*
 * Copyright 2025 Sweden Connect
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
package se.swedenconnect.oidf.registry.config;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import se.swedenconnect.oidf.registry.auth.OrganizationInformation;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.auth.RegistryJwtConverter;
import se.swedenconnect.oidf.registry.service.OrganizationService;

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
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(final HttpSecurity http,
      final Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter) throws Exception {
    http
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter(customJwtAuthenticationConverter))
        )
        .csrf(AbstractHttpConfigurer::disable)

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/registry/v1/entities/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/entity/write")

            .requestMatchers("/registry/v1/modules/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/modules/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")

            .requestMatchers("/registry/v1/trustmarks/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/modules/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/trustmarks/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarks/write")

            .requestMatchers("/registry/v1/trustmarks/subjects/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/trustmarks/subjects/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/trustmarksubjects/write")



            .requestMatchers("/registry/v1/policies/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/write")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/write")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_http://registry.swedenconnect.se/policies/write")


            .requestMatchers(HttpMethod.GET, "/api/v1/federationservice/**").permitAll() // Always open
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/swagger-ui.html",
                "/swagger-ui/**", "/v3/api-docs/**", "/*").permitAll()
            .anyRequest().denyAll()
        );
    return http.build();
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter(
      final OrganizationService service) {
    return new RegistryJwtConverter(service);
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
     */
    public RegistryClaims(final Jwt jwt,
        final OrganizationInformation information) {
      super(jwt);
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
