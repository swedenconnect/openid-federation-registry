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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResourceServerIssuerStartupCheck}.
 */
class ResourceServerIssuerStartupCheckTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(ResourceServerIssuerStartupCheck.class);

  private static final String JWK_SET_URI =
      ResourceServerIssuerStartupCheck.JWK_SET_URI_PROPERTY + "=https://idp.example.se/realm/certs";
  private static final String ISSUER_URI =
      ResourceServerIssuerStartupCheck.ISSUER_URI_PROPERTY + "=https://idp.example.se/realm";
  private static final String PUBLIC_KEY_LOCATION =
      ResourceServerIssuerStartupCheck.PUBLIC_KEY_LOCATION_PROPERTY + "=classpath:public_auth_cert.pem";

  @Test
  @DisplayName("Context starts when the issuer is configured together with a JWK set URI")
  void startsWithJwkSetUriAndIssuer() {
    this.runner
        .withPropertyValues(JWK_SET_URI, ISSUER_URI)
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  @DisplayName("Context starts with a public key and no issuer")
  void startsWithPublicKeyWithoutIssuer() {
    this.runner
        .withPropertyValues(PUBLIC_KEY_LOCATION)
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  @DisplayName("Context fails to start when a JWK set URI is configured without an issuer")
  void failsWithJwkSetUriWithoutIssuer() {
    this.runner
        .withPropertyValues(JWK_SET_URI)
        .run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .rootCause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(ResourceServerIssuerStartupCheck.ISSUER_URI_PROPERTY));
  }

  @Test
  @DisplayName("Context fails to start when the issuer of a JWK set URI is blank")
  void failsWithJwkSetUriAndBlankIssuer() {
    this.runner
        .withPropertyValues(JWK_SET_URI, ResourceServerIssuerStartupCheck.ISSUER_URI_PROPERTY + "= ")
        .run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .rootCause()
            .isInstanceOf(IllegalStateException.class));
  }

  @Test
  @DisplayName("Context fails to start when the issuer is combined with a public key only")
  void failsWithPublicKeyAndIssuer() {
    this.runner
        .withPropertyValues(PUBLIC_KEY_LOCATION, ISSUER_URI)
        .run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .rootCause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(ResourceServerIssuerStartupCheck.PUBLIC_KEY_LOCATION_PROPERTY));
  }
}
