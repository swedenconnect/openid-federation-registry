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

package se.swedenconnect.oidf.registry.validation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Status class for validations
 *
 * @author Per Fredrik Plars
 */
public class ValidationStatusImpl implements ValidationStatus {

  List<ValidationStatus> status;

  /**
   * Constructor.
   *
   * @param status list of validation statuses to aggregate
   */
  public ValidationStatusImpl(final List<ValidationStatus> status) {
    this.status = status;
  }

  /**
   * Creates a successful validation status.
   *
   * @param filedName the field name that was validated
   * @param validatorName the name of the validator
   * @return a ValidationStatus representing a successful validation
   */
  public static ValidationStatus ok(
      final String filedName,
      final String validatorName) {

    return new ValidationStatus() {
      @Override
      public String getFieldName() {
        return filedName;
      }

      @Override
      public String getValidationRule() {
        return validatorName;
      }

      @Override
      public String getValidationMessage() {
        return "ok";
      }
    };
  }

  /**
   * Creates a no-evaluation validation status (value was blank or null, validation skipped).
   *
   * @param filedName the field name that was not evaluated
   * @param validatorName the name of the validator
   * @return a ValidationStatus representing a skipped validation
   */
  public static ValidationStatus noEval(
      final String filedName,
      final String validatorName) {
    return new ValidationStatus() {
      @Override
      public String getFieldName() {
        return filedName;
      }

      @Override
      public String getValidationRule() {
        return validatorName;
      }

      @Override
      public String getValidationMessage() {
        return "noEval";
      }
    };
  }

  @Override
  public String getFieldName() {
    return this.status.stream().findFirst().map(ValidationStatus::getFieldName).orElse(null);
  }

  @Override
  public String getValidationRule() {
    return this.status.stream()
        .map(ValidationStatus::getValidationRule)
        .collect(Collectors.joining("."));
  }

  @Override
  public String getValidationMessage() {
    return this.status.stream()
        .map(ValidationStatus::getValidationMessage)
        .collect(Collectors.joining("."));
  }
}
