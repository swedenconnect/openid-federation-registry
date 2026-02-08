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

package se.swedenconnect.oidf.registry.entity.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.infrastructure.persistence.StringListConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringListConverterTest {

  private final StringListConverter converter = new StringListConverter(new ObjectMapper());

  @Test
  void convertToDatabaseColumn_withValues() {
    final String result = this.converter.convertToDatabaseColumn(List.of("alpha", "beta", "gamma"));

    assertThat(result).isEqualTo("[\"alpha\",\"beta\",\"gamma\"]");
  }

  @Test
  void convertToDatabaseColumn_withEmptyList() {
    final String result = this.converter.convertToDatabaseColumn(List.of());

    assertThat(result).isEqualTo("[]");
  }

  @Test
  void convertToDatabaseColumn_withNull() {
    final String result = this.converter.convertToDatabaseColumn(null);

    assertThat(result).isNull();
  }

  @Test
  void convertToEntityAttribute_withValues() {
    final List<String> result = this.converter.convertToEntityAttribute("[\"alpha\",\"beta\",\"gamma\"]");

    assertThat(result).containsExactly("alpha", "beta", "gamma");
  }

  @Test
  void convertToEntityAttribute_withEmptyArray() {
    final List<String> result = this.converter.convertToEntityAttribute("[]");

    assertThat(result).isEmpty();
  }

  @Test
  void convertToEntityAttribute_withNull() {
    final List<String> result = this.converter.convertToEntityAttribute(null);

    assertThat(result).isEmpty();
  }

  @Test
  void convertToEntityAttribute_withEmptyString() {
    final List<String> result = this.converter.convertToEntityAttribute("");

    assertThat(result).isEmpty();
  }

  @Test
  void convertToEntityAttribute_withInvalidJson() {
    assertThatThrownBy(() -> this.converter.convertToEntityAttribute("not-json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to parse");
  }

  @Test
  void roundTrip() {
    final List<String> original = List.of("one", "two", "three");

    final String json = this.converter.convertToDatabaseColumn(original);
    final List<String> restored = this.converter.convertToEntityAttribute(json);

    assertThat(restored).isEqualTo(original);
  }
}
