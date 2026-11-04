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
import org.springframework.core.convert.support.GenericConversionService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the conversion of session attributes in {@link SessionConfig}.
 *
 * @author Per Fredrik Plars
 */
class SessionConfigTest {

  private final GenericConversionService conversionService = SessionConfig.conversionService();

  @Test
  @DisplayName("A session attribute is read back as it was stored")
  void attributeSurvivesConversion() {
    final byte[] bytes = this.conversionService.convert(List.of("a", "b"), byte[].class);

    assertThat(this.conversionService.convert(bytes, Object.class)).isEqualTo(List.of("a", "b"));
  }

  @Test
  @DisplayName("A session attribute that cannot be read is read as missing")
  void unreadableAttributeIsMissing() {
    final byte[] bytes = {0x01, 0x02, 0x03};

    assertThat(this.conversionService.convert(bytes, Object.class)).isNull();
  }
}
