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
package se.swedenconnect.oidf.registry.federationserviceapi.records;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonObject {
  private final Map<String, Object> json;

  public JsonObject(final Map<String, Object> json) {
    this.json = json;
  }

  public String getStringValue(final String key) {
    return (String) this.json.get(key);
  }

  public List<String> getStringListClaim(final String key) {
    return Optional.ofNullable((List<String>) this.json.get(key))
        .orElse(List.of());
  }

  public List<Object> getObjectMapClaim(final String key) {
    return Optional.ofNullable((List<Object>) this.json.get(key))
        .orElse(List.of());
  }
}
