/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.registry.validation;

import java.util.List;
import java.util.Optional;

/**
 * An interface for defining property validation rules. Implementations of this interface are used to apply specific
 * validation logic to key-value pairs, ensuring that the data adheres to specified constraints or requirements.
 *
 * @author Per Fredrik Plars
 */
public interface PropertyValidator {
  /**
   * Validates a property based on the provided key and value. The method applies predefined validation rules and throws
   * an exception if the validation fails.
   *
   * @param key the property key to be validated
   * @param value the property value to be validated
   * @throws PropertyValidationFailException if the validation for the property fails
   */
  void validate(String key, String value) throws PropertyValidationFailException;

  /**
   * Evaluates the validity of a property using its key and value pair by invoking the validation logic. If the
   * validation fails, it returns an Optional containing the exception. If the validation succeeds, an empty Optional is
   * returned.
   *
   * @param key the property key to be validated
   * @param value the property value to be validated
   * @return an Optional containing {@code PropertyValidationFailException} if validation fails, or an empty Optional if
   *     validation succeeds
   */
  default Optional<PropertyValidationFailException> eval(String key, Object value) {
    try {
      if (value instanceof final List<?> values) {
        for (int i = 0; i < values.size(); i++) {
          this.validate(key + "[" + i + "]", values.get(i) == null ? null : values.get(i).toString());
        }
        if (values.isEmpty()) {
          this.validate(key, null);
        }
      }
      else {
        this.validate(key, value == null ? null : value.toString());
      }
    }
    catch (PropertyValidationFailException e) {
      return Optional.of(e);
    }
    return Optional.empty();
  }
}
