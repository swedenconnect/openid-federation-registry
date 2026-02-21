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

import lombok.Getter;

/**
 * Exception that is thrown when a property validation fails.
 *
 * @author Per Fredrik Plars
 */
@Getter
public class PropertyValidationFailException extends RuntimeException {
  final String filedName;
  final String inputValue;
  final String validationFailMessage;

  /**
   * Constructs a PropertyValidationFailException with the specified field name, input value, and validation failure
   * message.
   *
   * @param filedName the name of the field that failed validation
   * @param inputValue the value provided for the field that failed validation
   * @param validationFailMessage the message describing the reason for validation failure
   */
  public PropertyValidationFailException(final String filedName,
      final String inputValue,
      final String validationFailMessage) {
    super("Key:'%s' - '%s' Original value: '%s'".formatted(filedName, validationFailMessage, inputValue));
    this.validationFailMessage = validationFailMessage;
    this.filedName = filedName;
    this.inputValue = inputValue;
  }

  /**
   * Constructs a PropertyValidationFailException with the specified field name and validation failure message.
   *
   * @param filedName the name of the field that failed validation
   * @param validationFailMessage the message describing the validation failure
   */
  public PropertyValidationFailException(final String filedName,
      final String validationFailMessage) {
    super("Key:'%s' - %s".formatted(filedName, validationFailMessage));
    this.validationFailMessage = validationFailMessage;
    this.filedName = filedName;
    this.inputValue = null;
  }
}
