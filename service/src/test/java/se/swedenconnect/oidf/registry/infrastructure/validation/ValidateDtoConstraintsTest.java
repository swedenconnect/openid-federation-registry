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
import java.util.Map;

class ValidateDtoConstraintsTest {

  @Test
  void nothingIsRequired() {
    assertThatCode(() -> ValidateDto.validateConstraints(null)).doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateConstraints(Map.of())).doesNotThrowAnyException();
  }

  @Test
  void validConstraintsAreAccepted() {
    assertThatCode(() -> ValidateDto.validateConstraints(Map.of(
        "max_path_length", 0,
        "naming_constraints", Map.of("permitted", List.of(".example.com"), "excluded", List.of("a.example.com")),
        "allowed_entity_types", List.of("openid_provider", "openid_relying_party"))))
        .doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateConstraints(Map.of("max_path_length", 3L))).doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateConstraints(Map.of("naming_constraints", Map.of("permitted", List.of()))))
        .doesNotThrowAnyException();
  }

  @Test
  void unknownKeysAreRejected() {
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("max-path-length", 1)))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> ValidateDto.validateConstraints(
        Map.of("naming_constraints", Map.of("allowed", List.of("x")))))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  void maxPathLengthMustBeANonNegativeInteger() {
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("max_path_length", -1)))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("max_path_length", "2")))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("max_path_length", 1.5)))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  void listsMustContainNonEmptyStrings() {
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("allowed_entity_types", "openid_provider")))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("allowed_entity_types", List.of(" "))))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> ValidateDto.validateConstraints(
        Map.of("naming_constraints", Map.of("excluded", List.of(1)))))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> ValidateDto.validateConstraints(Map.of("naming_constraints", "permitted")))
        .isInstanceOf(PropertyValidationFailException.class);
  }
}
