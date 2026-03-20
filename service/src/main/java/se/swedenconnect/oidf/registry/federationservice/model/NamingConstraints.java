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
package se.swedenconnect.oidf.registry.federationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data class for naming constraints.
 *
 * @author Felix Hellman
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NamingConstraints {
  private List<String> permitted;
  private List<String> excluded;

  /**
   * @param json to read values from
   * @return new instance
   */
  public static NamingConstraints fromJson(final Map<String, Object> json) {
    return NamingConstraints.builder()
        .permitted((List<String>) Optional.ofNullable(json.get("permitted")).orElse(List.of()))
        .excluded((List<String>) Optional.ofNullable(json.get("excluded")).orElse(List.of()))
        .build();
  }

  /**
   * @return this object as json
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();
    json.put("permitted", this.permitted);
    json.put("excluded", this.excluded);
    return json;
  }
}
