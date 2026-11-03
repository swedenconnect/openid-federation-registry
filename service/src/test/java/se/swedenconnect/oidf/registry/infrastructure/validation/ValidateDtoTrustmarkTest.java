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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.UUID;

class ValidateDtoTrustmarkTest {

  private final ValidateDto validator =
      ValidateDto.init(new OrganizationRecord("55555", "PM", "https://www.pm.se/oidf/", null));

  private static TrustmarkDto dto(final String duration) {
    final TrustmarkDto dto = new TrustmarkDto();
    dto.setTrustmarkissuerId(UUID.randomUUID());
    dto.setTrustmarkType("https://www.pm.se/oidf/trustmark/loa3");
    dto.setTrustMarkTokenValidityDuration(duration);
    return dto;
  }

  @Test
  void validityDurationIsOptional() {
    assertThatCode(() -> this.validator.validate(dto(null))).doesNotThrowAnyException();
    assertThatCode(() -> this.validator.validate(dto(""))).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT5M", "PT1H", "P2D"})
  void validityDurationOfAtLeastFiveMinutesIsValid(final String duration) {
    assertThatCode(() -> this.validator.validate(dto(duration))).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT4M59S", "PT1M", "PT0S", "-PT1H"})
  void validityDurationShorterThanFiveMinutesIsRejected(final String duration) {
    assertThatThrownBy(() -> this.validator.validate(dto(duration)))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  void validityDurationMustBeAnIsoDuration() {
    assertThatThrownBy(() -> this.validator.validate(dto("one hour")))
        .isInstanceOf(PropertyValidationFailException.class);
  }
}
