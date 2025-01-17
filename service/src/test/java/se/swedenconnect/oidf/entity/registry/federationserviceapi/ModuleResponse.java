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
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Data response from registry to configure runtime modules from.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class ModuleResponse {
  private List<ResolverModuleResponse> resolvers;
  private List<TrustAnchorModuleResponse> trustAnchors;
  private List<TrustMarkIssuerModuleResponse> trustMarkIssuers;

  /**
   * Creates instance from json object {@link java.util.HashMap}
   *
   * @param json to read
   * @return new instance
   */
  public static ModuleResponse fromJson(final Map<String, Object> json) {
    final ModuleResponse response = new ModuleResponse();
    response.resolvers = ((List<Map<String, Object>>) json.get("resolvers"))
        .stream()
        .map(ResolverModuleResponse::fromJson)
        .toList();
    response.trustAnchors = ((List<Map<String, Object>>) json.get("trust-anchors"))
        .stream()
        .map(TrustAnchorModuleResponse::fromJson)
        .toList();
    response.trustMarkIssuers = ((List<Map<String, Object>>) json.get("trust-mark-issuers"))
        .stream()
        .map(TrustMarkIssuerModuleResponse::fromJson)
        .toList();
    return response;
  }
}
