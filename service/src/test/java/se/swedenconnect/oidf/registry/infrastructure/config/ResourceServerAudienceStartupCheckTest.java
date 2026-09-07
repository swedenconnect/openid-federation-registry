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
 * Unit tests for {@link ResourceServerAudienceStartupCheck}.
 */
class ResourceServerAudienceStartupCheckTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(ResourceServerAudienceStartupCheck.class);

  @Test
  @DisplayName("Context starts when at least one audience is configured")
  void startsWithAudience() {
    this.runner
        .withPropertyValues(ResourceServerAudienceStartupCheck.AUDIENCES_PROPERTY + "=oidf-entity-registry")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  @DisplayName("Context starts when several audiences are configured")
  void startsWithSeveralAudiences() {
    this.runner
        .withPropertyValues(ResourceServerAudienceStartupCheck.AUDIENCES_PROPERTY + "=account,oidf-entity-registry")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  @DisplayName("Context fails to start when no audience is configured")
  void failsWithoutAudience() {
    this.runner.run(context -> assertThat(context)
        .hasFailed()
        .getFailure()
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(ResourceServerAudienceStartupCheck.AUDIENCES_PROPERTY));
  }

  @Test
  @DisplayName("Context fails to start when the configured audience is blank")
  void failsWithBlankAudience() {
    this.runner
        .withPropertyValues(ResourceServerAudienceStartupCheck.AUDIENCES_PROPERTY + "= ")
        .run(context -> assertThat(context)
            .hasFailed()
            .getFailure()
            .rootCause()
            .isInstanceOf(IllegalStateException.class));
  }
}
