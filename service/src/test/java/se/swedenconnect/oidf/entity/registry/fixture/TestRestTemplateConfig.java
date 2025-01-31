package se.swedenconnect.oidf.entity.registry.fixture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Test configuration for adding a selfmade and self-signed token to
 * the {@link TestRestTemplate} used for integration tests.
 *
 * @author David Goldring
 */
@Configuration
public class TestRestTemplateConfig {

  @Autowired
  JwtTestUtils jwtTestUtils;

  @Bean
  public RestTemplateCustomizer restTemplateCustomizer() {
    return restTemplate -> {
      restTemplate.getInterceptors().add((request, body, execution) -> {
        // Get our "fake" token
        String token = jwtTestUtils.createJwt();

        // Add to the Authorization header
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        return execution.execute(request, body);
      });
    };
  }
}