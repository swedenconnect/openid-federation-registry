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

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the {@link JsonMapper} the JPA converters write to the database with.
 *
 * @author Per Fredrik Plars
 */
final class PersistenceJsonMapper {

  private PersistenceJsonMapper() {
  }

  /**
   * Copies the application's mapper but keeps map entries whose value is {@code null}.
   *
   * <p>{@code spring.jackson.default-property-inclusion: non_null} also drops null map entries, which would turn
   * {@code {"value": null}} in a metadata policy into {@code {}}. Null POJO properties are still left out.</p>
   *
   * @param mapper the application's mapper
   * @return a mapper that keeps null map entries
   */
  static JsonMapper keepingNullMapEntries(final JsonMapper mapper) {
    return mapper.rebuild()
        .changeDefaultPropertyInclusion(inclusion -> inclusion.withContentInclusion(JsonInclude.Include.ALWAYS))
        .build();
  }
}
