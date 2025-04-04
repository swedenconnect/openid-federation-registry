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

package se.swedenconnect.oidf.registry.fixture;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Spring test configurations
 *
 * @author Per Fredrik Plars
 */
@Configuration
public class SpringTestConfiguation {

  @Bean
  public RestTemplateCustomizer restTemplateCustomizer(final JwtTestUtils jwtTestUtils) {
    return restTemplate -> restTemplate.getInterceptors()
        .add((request, body, execution) -> {
          final String token = jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM);
          request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
          return execution.execute(request, body);
        });
  }

  @Bean
  public TestDataOperations testDataOperations(TestRestTemplate restTemplate) {
    return new TestDataOperations(restTemplate);
  }

  @Bean
  public FederationAPIOperations testFederationAPIOperations(TestRestTemplate restTemplate) {
    return new FederationAPIOperations(restTemplate);
  }

}
