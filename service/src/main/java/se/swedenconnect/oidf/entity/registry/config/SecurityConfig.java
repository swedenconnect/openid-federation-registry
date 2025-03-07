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
package se.swedenconnect.oidf.entity.registry.config;

import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.service.OrganizationService;

import java.util.function.Supplier;
/**
 * Security configuration class that defines security-related settings for the application. This class integrates OAuth2
 * Resource Server and configures security rules for specific HTTP endpoints. It utilizes Spring Security to define the
 * security rules, such as enabling JWT token-based authentication, disabling CSRF for stateless APIs, and specifying
 * role-based access controls for various endpoints.
 *
 * @author Per Fredrik Plars
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  /**
   * Configures a {@link SecurityFilterChain} bean for securing the application using OAuth2 Resource
   * Server capabilities.
   * This method sets up JWT authentication with a custom JWT authentication converter, disables CSRF, and defines
   * authorization rules for specific request matchers and HTTP methods.
   *
   * @param http the {@link HttpSecurity} object allowing configuration of security for HTTP requests.
   * @param customJwtAuthenticationConverter a custom {@link Converter} that converts a {@link Jwt} into an
   *        {@link AbstractAuthenticationToken} for authentication purposes.
   * @return a configured {@link SecurityFilterChain} used to enforce security policies.
   * @throws Exception if an error occurs during configuration of the security chain.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http,
      final Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter) throws Exception {
    http
        .oauth2ResourceServer(oauth2 -> oauth2
            //.jwt(Customizer.withDefaults())
            .jwt(jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter(customJwtAuthenticationConverter))
        )
        .csrf(AbstractHttpConfigurer::disable)

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/registry/v1/entities/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_entities_read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/entities/**")
            .hasAuthority("SCOPE_entities_write")

            .requestMatchers("/registry/v1/trustmarksubjects/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/trustmarksubjects/**")
            .hasAuthority("SCOPE_trustmarksubjects_read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/trustmarksubjects/**")
            .hasAuthority("SCOPE_trustmarksubjects_write")

            .requestMatchers("/registry/v1/policies/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_policies_read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/policies/**")
            .hasAuthority("SCOPE_policies_write")

            .requestMatchers("/registry/v1/options/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/registry/v1/options/**")
            .hasAuthority("SCOPE_options_read")
            .requestMatchers(HttpMethod.POST, "/registry/v1/options/**")
            .hasAuthority("SCOPE_options_create")
            .requestMatchers(HttpMethod.PUT, "/registry/v1/options/**")
            .hasAuthority("SCOPE_options_update")
            .requestMatchers(HttpMethod.DELETE, "/registry/v1/options/**")
            .hasAuthority("SCOPE_options_delete")

            .requestMatchers(HttpMethod.GET, "/api/v1/federationservice/**").permitAll() // Always open
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            .anyRequest().denyAll()
        );
    return http.build();
  }

  /**
   * Provides a {@link Supplier} that retrieves the organization entity associated with the currently authenticated
   * user. If the authentication principal is an instance of {@link RegistryClaims}, the corresponding
   * {@link OrganizationEntity} is returned. If the principal is of another type or null, no organization is returned.
   *
   * @return a {@link Supplier} that supplies the {@link OrganizationEntity} associated with the current user, or null
   *     if the user does not have an associated organization.
   */
  @Bean(name = "userAssignedOrganization")
  public Supplier<OrganizationEntity> userAssignedOrganization() {
    return () -> {
      final Object principal = SecurityContextHolder.getContext().getAuthentication();
      if (principal instanceof RegistryClaims) {
        return ((RegistryClaims) principal).getOrg();
      }
      return null;
    };
  }

  /**
   * Defines a custom JWT authentication converter bean to process JWT claims and generate an authentication token with
   * additional claims specific to the application, such as organization details and domain prefix.
   *
   * @param organizationService the service used to retrieve or create organization entities based on the JWT
   *     claims
   * @return a {@link Converter} that converts a {@link Jwt} to an {@link AbstractAuthenticationToken}. The token
   *     contains additional claims and organization information for authentication purposes.
   */
  @Bean
  public Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter(
      final OrganizationService organizationService) {

    return new Converter<Jwt, AbstractAuthenticationToken>() {

      @Override
      public AbstractAuthenticationToken convert(final Jwt jwt) {
        final String orgNumber = jwt.getClaimAsString("orgNumber");
        final String orgName = jwt.getClaimAsString("orgName");
        final String entityPrefix = jwt.getClaimAsString("entity_prefix");

        Assert.hasText(orgNumber, "Missing orgNumber claim in JWT");
        Assert.hasText(orgName, "Missing orgName claim in JWT");
        Assert.hasText(entityPrefix, "Missing entity_prefix claim in JWT");

        final OrganizationEntity org = organizationService.findCreate(orgNumber, orgName);
        final RegistryClaims registryClaims = new RegistryClaims(jwt,
            org, entityPrefix);
        registryClaims.setAuthenticated(true);
        return registryClaims;
      }
    };
  }

  /**
   * The RegistryClaims class extends JwtAuthenticationToken and provides additional
   * information about the authenticated
   * client, including the associated organization and domain prefix.
   */
  @Getter
  public static class RegistryClaims extends JwtAuthenticationToken {
    private final OrganizationEntity org;
    private final String domainPrefix;

    /**
     * Constructs a new instance of the RegistryClaims class, which extends JwtAuthenticationToken to
     * include additional information such as the associated organization and domain prefix.
     *
     * @param jwt the {@link Jwt} object containing the token's claims and headers.
     * @param org the {@link OrganizationEntity} representing the authenticated client's associated organization.
     * @param domainPrefix a {@link String} representing the domain prefix of the organization.
     */
    public RegistryClaims(final Jwt jwt, final OrganizationEntity org, final String domainPrefix) {
      super(jwt);
      this.org = org;
      this.domainPrefix = domainPrefix;
    }
  }

}
