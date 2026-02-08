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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.swedenconnect.oidf.registry.dto.TrustmarkSourceDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JPA Converter to handle Map<String,Object>
 *
 * @author Per Fredrik Plars
 */
@Converter
public class MapConverter implements AttributeConverter<Map<String,Object>, String> {
  final ObjectMapper mapper;

  /**
   * Constructor
   *
   * @param objectMapper mapper to convert values
   */
  public MapConverter(final ObjectMapper objectMapper) {
    this.mapper = objectMapper;
  }

  /**
   * Convert list to string
   * @param attribute List to be converted to string
   * @return String value of list
   */
  @Override
  public String convertToDatabaseColumn(final Map<String,Object> attribute) {
    return attribute == null ? null : this.writeMapToJsonPretty(attribute);
  }

  /**
   * Convert string to list
   * @param dbData string data from database
   * @return List
   */
  @Override
  public Map<String,Object> convertToEntityAttribute(final String dbData) {
    return dbData == null || dbData.isEmpty()
        ? Map.of()
        : this.readMapFromJson(dbData);
  }

  /**
   * Writes a Map to JSON string with pretty printing.
   *
   * @param map the map to serialize
   * @return the JSON string representation with pretty printing
   * @throws IllegalArgumentException if JSON serialization fails
   */
  private  String writeMapToJsonPretty(final Map<String, Object> map) {
    if (map == null) {
      return null;
    }
    try {
      return this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize map to JSON", e);
    }
  }

  private  Map<String, Object> readMapFromJson(final String jsonStr) {
    if (jsonStr == null || jsonStr.isBlank()) {
      return Collections.emptyMap();
    }
    try {
      return this.mapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
    }
    catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to parse JSON", e);
    }
  }


}