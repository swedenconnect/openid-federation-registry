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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Abstract base class for JPA converters that serialize a typed object as JSON text.
 *
 * <p>Use the {@code Class<T>} constructor for simple types and the {@code TypeReference<T>}
 * constructor for generic types such as {@code List<MyObject>}.</p>
 *
 * <pre>{@code
 * // Simple type
 * public class MyObjectConverter extends JsonConverter<MyObject> {
 *   public MyObjectConverter(JsonMapper mapper) { super(mapper, MyObject.class); }
 * }
 *
 * // Generic type
 * public class MyListConverter extends JsonConverter<List<MyObject>> {
 *   public MyListConverter(JsonMapper mapper) {
 *     super(mapper, new TypeReference<List<MyObject>>() {});
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
  private final TypeReference<T> typeReference;

  /**
   * Constructs a converter for a simple (non-generic) type.
   *
   * @param mapper the JSON mapper
   * @param type the target class to deserialize into
   */
  protected JsonConverter(final JsonMapper mapper, final Class<T> type) {
    this.mapper = mapper;
    this.type = type;
    this.typeReference = null;
  }

  /**
   * Constructs a converter for a generic type such as {@code List<Foo>}.
   *
   * @param mapper the JSON mapper
   * @param typeReference Jackson type reference capturing the full generic type
   */
  protected JsonConverter(final JsonMapper mapper, final TypeReference<T> typeReference) {
    this.mapper = mapper;
    this.type = null;
    this.typeReference = typeReference;
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
    if (this.type != null) {
      return this.mapper.readValue(dbData, this.type);
    }
    return this.mapper.readValue(dbData, this.typeReference);
  }
}