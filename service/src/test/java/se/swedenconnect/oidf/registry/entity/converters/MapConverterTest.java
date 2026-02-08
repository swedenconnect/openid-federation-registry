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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapConverterTest {

  private final MapConverter converter = new MapConverter(new ObjectMapper());

  @Test
  void convertToDatabaseColumn_withValues() {
    final Map<String, Object> input = new LinkedHashMap<>();
    input.put("key1", "value1");
    input.put("key2", 42);

    final String result = this.converter.convertToDatabaseColumn(input);

    assertThat(result).contains("\"key1\" : \"value1\"");
    assertThat(result).contains("\"key2\" : 42");
  }

  @Test
  void convertToDatabaseColumn_withEmptyMap() {
    final String result = this.converter.convertToDatabaseColumn(Map.of());

    assertThat(result).isEqualTo("{ }");
  }

  @Test
  void convertToDatabaseColumn_withNull() {
    final String result = this.converter.convertToDatabaseColumn(null);

    assertThat(result).isNull();
  }

  @Test
  void convertToEntityAttribute_withValues() {
    final Map<String, Object> result =
        this.converter.convertToEntityAttribute("{\"key1\":\"value1\",\"key2\":42}");

    assertThat(result)
        .containsEntry("key1", "value1")
        .containsEntry("key2", 42);
  }

  @Test
  void convertToEntityAttribute_withNestedObject() {
    final String json = "{\"outer\":{\"inner\":\"value\"},\"list\":[1,2,3]}";

    final Map<String, Object> result = this.converter.convertToEntityAttribute(json);

    assertThat(result.get("outer")).isInstanceOf(Map.class);
    assertThat(result).extracting("list").asList().containsExactly(1, 2, 3);
  }

  @Test
  void convertToEntityAttribute_withNull() {
    final Map<String, Object> result = this.converter.convertToEntityAttribute(null);

    assertThat(result).isEmpty();
  }

  @Test
  void convertToEntityAttribute_withEmptyString() {
    final Map<String, Object> result = this.converter.convertToEntityAttribute("");

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
    final Map<String, Object> original = new LinkedHashMap<>();
    original.put("name", "test");
    original.put("count", 5);
    original.put("active", true);

    final String json = this.converter.convertToDatabaseColumn(original);
    final Map<String, Object> restored = this.converter.convertToEntityAttribute(json);

    assertThat(restored).isEqualTo(original);
  }
}
