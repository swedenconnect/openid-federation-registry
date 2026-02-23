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

import java.util.ArrayList;
import java.util.List;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
public class ValidationSuccess {
  List<ValidationStatus> status;

  public ValidationSuccess(List<ValidationStatus> status) {
    this.status = status;
  }

  public static ValidationSuccess ok(final String filedName, final String validatorName) {
    return new ValidationSuccess(List.of(new ValidationStatus(filedName, validatorName,"validated ok")));
  }

  public static ValidationSuccess ok(final String filedName, final String validatorName,final String msg) {
    return new ValidationSuccess(List.of(new ValidationStatus(filedName, validatorName,msg)));
  }

  public static ValidationSuccess err(final String filedName,
      final String validatorName,
    final String inputValue,
    final String validationFailMessage) {


    return new ValidationSuccess(List.of(new ValidationStatus(filedName, validatorName,msg)));
/*
    super("Key:'%s' - '%s' Original value: '%s'".formatted(filedName, validationFailMessage, inputValue));
      this.validationFailMessage = validationFailMessage;
      this.filedName = filedName;
      this.inputValue = inputValue;
    }
  */
  }
  public static ValidationSuccess noEval(final String key, final String validatorName) {
    return new ValidationSuccess(List.of(new ValidationStatus(key, validatorName,"not evaluated")));
  }


  public record ValidationStatus(String key, String validatorName, String msg ){}

}
