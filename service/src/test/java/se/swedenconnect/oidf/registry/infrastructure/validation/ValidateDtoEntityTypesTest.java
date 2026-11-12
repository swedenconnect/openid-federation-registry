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

package se.swedenconnect.oidf.registry.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.List;

class ValidateDtoEntityTypesTest {

  @Test
  void validEntityTypesAreAccepted() {
    assertThatCode(() -> ValidateDto.validateEntityTypes(List.of("openid_relying_party"), null))
        .doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateEntityTypes(List.of("openid_relying_party"), false))
        .doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateEntityTypes(List.of("federation_entity", "openid_provider"), true))
        .doesNotThrowAnyException();
  }

  @Test
  void entityTypesAreRequired() {
    assertThatThrownBy(() -> ValidateDto.validateEntityTypes(null, null))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("entityTypes");
    assertThatThrownBy(() -> ValidateDto.validateEntityTypes(List.of(), null))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("entityTypes");
  }

  @Test
  void entityTypesMustNotBeBlank() {
    assertThatThrownBy(() -> ValidateDto.validateEntityTypes(List.of("openid_provider", " "), null))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("entityTypes");
  }

  @Test
  void intermediateRequiresFederationEntity() {
    assertThatThrownBy(() -> ValidateDto.validateEntityTypes(List.of("openid_provider"), true))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("isIntermediate");
  }
}
