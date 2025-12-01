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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import se.swedenconnect.oidf.registry.service.OrganizationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link SecurityConfig}.
 *
 * @author David Goldring
 */
class SecurityConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(SecurityConfig.class, MockConfig.class);

  @TestConfiguration
  static class MockConfig {
    @Bean
    OrganizationService organizationService() {
      return mock(OrganizationService.class);
    }

    @Bean
    @Primary
    JwtDecoder mockJwtDecoder() {
      return mock(JwtDecoder.class);
    }
  }

  @Test
  @DisplayName("DevJwtDecoder is created when devMode = true and public key is missing")
  void devJwtDecoderIsCreatedWhenDevModeIsEnabledAndPublicKeyIsMissing() {
    this.contextRunner
        .withPropertyValues("openid.federation.registry.dev-mode=true")
        .run(context -> assertThat(context).hasBean("devJwtDecoder"));
  }

  @Test
  @DisplayName("DevJwtDecoder is not created when devMode = false")
  void devJwtDecoderIsNotCreatedWhenDevModeIsDisabled() {
    this.contextRunner
        .withPropertyValues("openid.federation.registry.dev-mode=false")
        .run(context -> assertThat(context).doesNotHaveBean("devJwtDecoder"));
  }

  @Test
  @DisplayName("DevJwtDecoder is not created when public key is present")
  void devJwtDecoderIsNotCreatedWhenPublicKeyIsPresent() {
    this.contextRunner
        .withPropertyValues(
            "openid.federation.registry.dev-mode=true",
            "spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public.key"
        )
        .run(context -> assertThat(context).doesNotHaveBean("devJwtDecoder"));
  }
}