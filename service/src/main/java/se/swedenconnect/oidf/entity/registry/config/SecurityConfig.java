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
import lombok.NonNull;
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
import se.swedenconnect.oidf.entity.registry.auth.OrganizationInformation;
import se.swedenconnect.oidf.entity.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.entity.registry.auth.RegistryJwtConverter;
import se.swedenconnect.oidf.entity.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.entity.registry.service.OrganizationService;

import java.util.List;
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


  @Bean(name = "userAssignedOrganization")
  Supplier<OrganizationEntity> userAssignedOrganization() {
    return () -> {
      final Object principal = SecurityContextHolder.getContext().getAuthentication();
      if (principal instanceof RegistryClaims) {
        return ((RegistryClaims) principal).getOrg().getFirst();
      }
      return null;
    };
  }


  @Bean
  Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter(
      final OrganizationService service) {
    return new RegistryJwtConverter(service);
  }

  /**
   * The RegistryClaims class extends JwtAuthenticationToken and provides additional
   * information about the authenticated
   * client, including the associated organization and domain prefix.
   */
  @Getter
  public static class RegistryClaims extends JwtAuthenticationToken {
    private final List<OrganizationEntity> org;

    /**
     * Constructs a new instance of the RegistryClaims class, which extends JwtAuthenticationToken to
     * include additional information such as the associated organization and domain prefix.
     *
     * @param jwt the {@link Jwt} object containing the token's claims and headers.
     * @param org the {@link OrganizationEntity} representing the authenticated client's associated organization.
     */
    public RegistryClaims(final Jwt jwt, final List<OrganizationEntity> org) {
      super(jwt);
      this.org = org;
    }

    /**
     * @param orgNumber to find
     * @return entity or throws
     */
    public OrganizationEntity getByOrgNumber(@NonNull final String orgNumber) {
      return this.org.stream().filter(e -> e.getOrgNumber().equals(orgNumber))
          .findFirst()
          .orElseThrow();
    }
  }

}
