package se.swedenconnect.oidf.entity.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http
          .oauth2ResourceServer(oauth2 -> oauth2
              .jwt(jwt -> jwt
                  .jwtAuthenticationConverter(new JwtAuthenticationConverter())
              ))
          .csrf(AbstractHttpConfigurer::disable)

          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/secure/").authenticated()
              .requestMatchers(HttpMethod.GET,"/secure/policies/**").hasAuthority("SCOPE_policies_read")
              .requestMatchers(HttpMethod.POST,"/secure/policies/**").hasAuthority("SCOPE_policies_write")


              .requestMatchers("/registry/v1/entities/**").permitAll()
              .requestMatchers("/registry/v1/trustmarksubjects/**").permitAll()
              .requestMatchers("/registry/v1/policies/**").permitAll()

              .requestMatchers("/api/v1/federationservice/**").permitAll() // Always open
              .requestMatchers("/actuator/**").permitAll()
              .anyRequest().denyAll()
          );
      return http.build();
    }

  @Bean
  public JwtDecoder jwtDecoder() {

    byte[] keyBytes = "00000000000000000000000000000000".getBytes(StandardCharsets.UTF_8);
    SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HMAC");

    // Initialize NimbusJwtDecoder with the SecretKey
    return NimbusJwtDecoder.withSecretKey(secretKey).build();
    };
  }
