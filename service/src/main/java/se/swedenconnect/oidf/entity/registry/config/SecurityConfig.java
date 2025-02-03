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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

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
   * Configures and constructs a {@link SecurityFilterChain} bean to define security policies for the application. This
   * method sets up OAuth2 Resource Server with JWT authentication, disables CSRF, and configures role-based and
   * endpoint-specific access control rules.
   *
   * @param http the {@link HttpSecurity} object that allows configuring web-based security. It provides methods to
   *     define security configurations such as authentication mechanisms, endpoint permissions, and additional
   *     filters.
   * @return the configured {@link SecurityFilterChain} that applies the defined security configurations to incoming
   *     requests.
   * @throws Exception if an error occurs while setting up the security configurations.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    http
        /*.exceptionHandling(conf ->
            conf.authenticationEntryPoint((req, res, ex) -> {
              throw ex;
            }))
*/

        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults()))
        .csrf(AbstractHttpConfigurer::disable)

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/secure/").authenticated()
            .requestMatchers(HttpMethod.GET, "/secure/policies/**").hasAuthority("SCOPE_policies_read")
            .requestMatchers(HttpMethod.POST, "/secure/policies/**").hasAuthority("SCOPE_policies_write")

            .requestMatchers("/registry/v1/options/**").permitAll()
            .requestMatchers("/registry/v1/entities/**").permitAll()
            .requestMatchers("/registry/v1/trustmarksubjects/**").permitAll()
            .requestMatchers("/registry/v1/policies/**").permitAll()

            .requestMatchers("/api/v1/federationservice/**").permitAll() // Always open
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().denyAll()

        );
    return http.build();
  }

}
