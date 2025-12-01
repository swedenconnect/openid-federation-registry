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

package se.swedenconnect.oidf.registry.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for the {@link VariableValueResolver} class.
 *
 * @author Per Fredrik Plars
 */
class VariableValueResolverTest {

  @Test
  @DisplayName("Insert template values - should succeed")
  void insertTemplateValues() {
    final VariableValueResolver r = VariableValueResolver.defaultResolver(Map.of("orgId", "2345"));

    assertThat(r.insertTemplateValues("https://example.com/entity/@{orgId}"))
        .isNotBlank().isEqualTo("https://example.com/entity/2345");

    assertThat(r.insertTemplateValues("https://example.com/entity/@{orgId}/@{orgId}"))
        .isNotBlank().isEqualTo("https://example.com/entity/2345/2345");

    assertThat(r.insertTemplateValues("https://example.com/entity/@{orgId}/@{orgId}/@{unknown}"))
        .isNotBlank().isEqualTo("https://example.com/entity/2345/2345/");

  }
}