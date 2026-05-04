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
import tools.jackson.databind.json.JsonMapper;

/**
 * Abstract base class for JPA converters that serialize a typed object as JSON text.
 *
 * <p>Subclasses supply the target type via the constructor, keeping serialization logic
 * in one place while allowing one concrete converter class per entity field.</p>
 *
 * <pre>{@code
 * @Converter
 * public class MyObjectConverter extends JsonConverter<MyObject> {
 *   public MyObjectConverter(JsonMapper mapper) {
 *     super(mapper, MyObject.class);
 *   }
 * }
 * }</pre>
 *
 * @param <T> the Java type to persist as JSON
 * @author Per Fredrik Plars
 */
public abstract class JsonConverter<T> implements AttributeConverter<T, String> {

  private final JsonMapper mapper;
  private final Class<T> type;

  /**
   * Constructs a converter for the given type.
   *
   * @param mapper the JSON mapper
   * @param type the target class to deserialize into
   */
  protected JsonConverter(final JsonMapper mapper, final Class<T> type) {
    this.mapper = mapper;
    this.type = type;
  }

  /**
   * Serializes the attribute to a JSON string.
   *
   * @param attribute the object to serialize, may be {@code null}
   * @return JSON string, or {@code null} if attribute is {@code null}
   */
  @Override
  public String convertToDatabaseColumn(final T attribute) {
    if (attribute == null) {
      return null;
    }
    return this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(attribute);
  }

  /**
   * Deserializes a JSON string back to the typed object.
   *
   * @param dbData the JSON string from the database, may be {@code null} or empty
   * @return the deserialized object, or {@code null} if dbData is blank
   */
  @Override
  public T convertToEntityAttribute(final String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    return this.mapper.readValue(dbData, this.type);
  }
}