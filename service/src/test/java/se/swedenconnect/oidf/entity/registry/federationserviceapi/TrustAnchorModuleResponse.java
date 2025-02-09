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
package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * TrustAnchor Module from Registry.
 *
 * @author Felix Hellman
 */
@ToString
@Getter
public class TrustAnchorModuleResponse {
  /** Alias for the given module */
  private String alias;
  /** EntityId for the trust anchor */
  private String entityIdentifier;
  /** Is the module qctive */
  private Boolean active;

  /**
   * Converts json object to new instance.
   *
   * @param json to read
   * @return new instance
   */
  public static TrustAnchorModuleResponse fromJson(final Map<String, Object> json) {
    final TrustAnchorModuleResponse trustAnchorModuleResponse = new TrustAnchorModuleResponse();
    trustAnchorModuleResponse.alias = (String) json.get("alias");
    trustAnchorModuleResponse.entityIdentifier = (String) json.get("entity-identifier");
    trustAnchorModuleResponse.active = (Boolean) json.get("active");
    return trustAnchorModuleResponse;
  }

}
