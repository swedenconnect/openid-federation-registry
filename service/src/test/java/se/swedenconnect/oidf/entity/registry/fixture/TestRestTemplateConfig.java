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

package se.swedenconnect.oidf.entity.registry.fixture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Test configuration for adding a selfmade and self-signed token to the {@link TestRestTemplate} used for integration
 * tests.
 *
 * @author David Goldring
 */
@Configuration
public class TestRestTemplateConfig {

  @Autowired
  JwtTestUtils jwtTestUtils;

  @Bean
  public RestTemplateCustomizer restTemplateCustomizer() {
    return restTemplate -> restTemplate.getInterceptors()
        .add((request, body, execution) -> {
          final String token = this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM);
          request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
          return execution.execute(request, body);
        });
  }
}