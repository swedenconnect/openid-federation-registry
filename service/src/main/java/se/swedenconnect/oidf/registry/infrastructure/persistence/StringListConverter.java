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

package se.swedenconnect.oidf.registry.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

/**
 * JPA Converter to handle list data
 *
 * @author Per Fredrik Plars
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
  final JsonMapper mapper;

  /**
   * Constructor
   *
   * @param objectMapper mapper to convert values
   */
  public StringListConverter(final JsonMapper objectMapper) {
    this.mapper = objectMapper;
  }

  /**
   * Convert list to string
   * @param attribute List to be converted to string
   * @return String value of list
   */
  @Override
  public String convertToDatabaseColumn(final List<String> attribute) {
    return attribute == null ? null : this.writeListJson(attribute);
  }

  /**
   * Convert string to list
   * @param dbData string data from database
   * @return List
   */
  @Override
  public List<String> convertToEntityAttribute(final String dbData) {
    return dbData == null || dbData.isEmpty()
        ? List.of()
        : this.readListJson(dbData);
  }

  private List<String> readListJson(final String jsonStr) {
    if (jsonStr == null || jsonStr.isBlank()) {
      return Collections.emptyList();
    }
      return this.mapper.readValue(jsonStr, new TypeReference<List<String>>() {});
  }

  private String writeListJson(final List<String> sources) {
    if (sources == null) {
      return null;
    }
      return this.mapper.writeValueAsString(sources);
  }
}
