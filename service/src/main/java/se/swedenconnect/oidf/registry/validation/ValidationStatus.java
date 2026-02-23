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



/**
 * Status class for validations
 *
 * @author Per Fredrik Plars
 */
public interface ValidationStatus {

  /**
   * Field name that the status is connected to
   *
   * @return String
   */
  String getFieldName();

  /**
   * String that represents the rules used to validate.
   *
   * @return String in the format of min(34).max(111)
   */
  String getValidationRule();

  /**
   * A message that reflects the validation result
   *
   * @return ok if the validaton was ok.
   */
  String getValidationMessage();


}
