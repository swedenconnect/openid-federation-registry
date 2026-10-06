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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.UUID;

class ValidateDtoTrustmarkIssuerTest {

  private final ValidateDto validator =
      ValidateDto.init(new OrganizationRecord("55555", "PM", "https://www.pm.se/oidf/", null));

  private static TrustmarkIssuerDto dto(final String duration) {
    final TrustmarkIssuerDto dto = new TrustmarkIssuerDto();
    dto.setEntityId(UUID.randomUUID());
    dto.setActive(true);
    dto.setTrustMarkTokenValidityDuration(duration);
    return dto;
  }

  @Test
  void validityDurationIsOptional() {
    assertThatCode(() -> this.validator.validate(dto(null))).doesNotThrowAnyException();
    assertThatCode(() -> this.validator.validate(dto(""))).doesNotThrowAnyException();
  }

  @Test
  void givenValidityDurationIsStillValidated() {
    assertThatCode(() -> this.validator.validate(dto("PT1H"))).doesNotThrowAnyException();
    assertThatThrownBy(() -> this.validator.validate(dto("one hour")))
        .isInstanceOf(PropertyValidationFailException.class);
  }
}
