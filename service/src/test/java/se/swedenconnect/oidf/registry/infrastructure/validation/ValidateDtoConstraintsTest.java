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
import se.swedenconnect.oidf.registry.subordinate.dto.ConstraintsDto;
import se.swedenconnect.oidf.registry.subordinate.dto.NamingConstraintsDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.Arrays;
import java.util.List;

class ValidateDtoConstraintsTest {

  private static ConstraintsDto constraints(final Integer maxPathLength, final List<String> permitted,
      final List<String> excluded, final List<String> entityTypes) {
    final ConstraintsDto dto = new ConstraintsDto();
    dto.setMaxPathLength(maxPathLength);
    if (permitted != null || excluded != null) {
      final NamingConstraintsDto naming = new NamingConstraintsDto();
      naming.setPermitted(permitted);
      naming.setExcluded(excluded);
      dto.setNamingConstraints(naming);
    }
    dto.setAllowedEntityTypes(entityTypes);
    return dto;
  }

  @Test
  void nothingIsRequired() {
    assertThatCode(() -> ValidateDto.validateConstraints(null)).doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateConstraints(new ConstraintsDto())).doesNotThrowAnyException();
  }

  @Test
  void validConstraintsAreAccepted() {
    assertThatCode(() -> ValidateDto.validateConstraints(constraints(2, List.of(".example.com"),
        List.of("east.example.com"), List.of("openid_provider", "openid_relying_party"))))
        .doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateConstraints(constraints(0, null, null, null)))
        .doesNotThrowAnyException();
    assertThatCode(() -> ValidateDto.validateConstraints(constraints(null, List.of(), null, List.of())))
        .doesNotThrowAnyException();
  }

  @Test
  void maxPathLengthMustNotBeNegative() {
    assertThatThrownBy(() -> ValidateDto.validateConstraints(constraints(-1, null, null, null)))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("max_path_length");
  }

  @Test
  void allowedEntityTypesMustNotBeBlank() {
    assertThatThrownBy(() -> ValidateDto.validateConstraints(
        constraints(null, null, null, List.of("openid_provider", " "))))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("allowed_entity_types");
    assertThatThrownBy(() -> ValidateDto.validateConstraints(
        constraints(null, null, null, Arrays.asList("openid_provider", null))))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  void namingConstraintsMustNotBeBlank() {
    assertThatThrownBy(() -> ValidateDto.validateConstraints(constraints(null, List.of(""), null, null)))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("naming_constraints.permitted");
    assertThatThrownBy(() -> ValidateDto.validateConstraints(constraints(null, null, List.of(" "), null)))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("naming_constraints.excluded");
  }
}
