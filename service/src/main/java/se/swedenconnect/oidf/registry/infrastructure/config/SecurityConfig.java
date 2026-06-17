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

import com.nimbusds.jose.jwk.JWK;
import jakarta.validation.constraints.NotNull;
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
import se.swedenconnect.iam.security.autoconfigure.IamSecurityProperties;
import se.swedenconnect.iam.security.claims.OrgRightsClaim;
import se.swedenconnect.iam.security.claims.OrgRightsClaimParser;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryJwtConverter;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient.RegistryOidcUser;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.config.properties.PkiCredentialConfigurationProperties;
import se.swedenconnect.security.credential.factory.PkiCredentialFactory;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
  SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http,
      final ClientRegistrationRepository clientRegistrationRepository,
      final NimbusJwtClientAuthenticationParametersConverter<OAuth2AuthorizationCodeGrantRequest>
          authCodeJwtConverter) {

    authCodeJwtConverter
        .setJwtClientAssertionCustomizer(
            jwt -> jwt.getHeaders().keyId("f4xXv-74w2zMDgeODCHNobm7ht3RuwfS50OQkWtsiL0"));

    final RestClientAuthorizationCodeTokenResponseClient tokenResponseClient =
        new RestClientAuthorizationCodeTokenResponseClient();
    tokenResponseClient.addParametersConverter(authCodeJwtConverter);

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
                .tokenEndpoint(token -> token
                    .accessTokenResponseClient(tokenResponseClient)
                )
                .failureHandler((request, response, exception) -> {
                  log.error("Authentication failed", exception);
                  response.sendRedirect("/login?errorcode=backend.login.failed");
                })
            // .userInfoEndpoint(userInfo -> userInfo
            //     .oidcUserService(oidcUserService)
            // )
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
            .requestMatchers(HttpMethod.GET, "/entities/**",
                "/registration-flows/**", "/registrations/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/*").permitAll()

            .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
            .authenticated()
            .requestMatchers(HttpMethod.GET, "/userinfo").authenticated()
            .requestMatchers(HttpMethod.PUT, "/userinfo").authenticated()

            .anyRequest().denyAll()
        );
    return http.build();
  }

  //@Bean
  Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter() {
    return new RegistryJwtConverter();
  }

  @Bean
  @Primary
  OidcUserService oidcUserService(final OrgRightsClaimParser claimParser) {
    return new OidcUserService() {
      public OidcUser loadUser(@NotNull final OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        final OrgRightsClaim claim = claimParser.parse(oidcUser.getClaim("org_rights"));

        return new RegistryOidcUser(oidcUser);
      }
    };
  }

  @Bean
  JWK oidcClientJwk(
      final IamSecurityProperties properties,
      final PkiCredentialFactory pkiCredentialFactory) throws Exception {

    final PkiCredentialConfigurationProperties credentialProps = properties.getClient().getCredential();
    if (credentialProps == null) {
      throw new IllegalStateException(
          "iam.security.client.credential is not configured — cannot create OIDC client JWK");
    }
    final PkiCredential credential = pkiCredentialFactory.createCredential(credentialProps);
    return JwkTransformerFunction.function()
        // Overriding existing JWK bean so that the kid can be set.
        .withKeyIdFunction(pkiCredential -> this.createKeyId(pkiCredential.getPublicKey()))
        .apply(credential);
  }

  /**
   * Generates a unique key identifier based on the SHA-256 hash of the input key's encoded form. This is the same
   * algoritm used in keycloak, when generating a kid for the public key.
   *
   * @param key the cryptographic key for which the unique identifier is to be generated
   * @return a URL-safe Base64 encoded string representation of the SHA-256 hash of the key
   * @throws RuntimeException if the SHA-256 algorithm is not available
   */
  private String createKeyId(final Key key) {
    try {
      final byte[] sha256Digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
      return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256Digest);
    }
    catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

}