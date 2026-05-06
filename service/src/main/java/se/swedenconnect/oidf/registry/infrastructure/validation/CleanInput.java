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


import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Clean out values
 *
 * @author Per Fredrik Plars
 */
public final class CleanInput {

  /**
   * Remove values in jwks
   * @param dto Object to be cleaned from incoming values
   * @return The same object
   */
  public static SubordinateDto clean(final SubordinateDto dto) {
    Objects.requireNonNull(dto, "SubordinateDto cannot be null");
    dto.setJwks(removeExpIatNbfFromJwks(dto.getJwks()));
    return dto;
  }

  /**
   * Remove attributes exp,iat,nbf from keys in jwks
   *
   * @param jwks Jwks strukture
   * @return Cleaned verson
   */
  public static Map<String, Object> removeExpIatNbfFromJwks(final Map<String, Object> jwks) {
    if(jwks == null) {
      return jwks;
    }
    final List<Map<String,Object>> keys = (java.util.List)jwks.get("keys");
    if(keys == null || keys.isEmpty()) {
      return jwks;
    }
    keys.forEach(key ->{
      key.remove("exp");
      key.remove("iat");
      key.remove("nbf");
    } );
    return jwks;
  }


}

