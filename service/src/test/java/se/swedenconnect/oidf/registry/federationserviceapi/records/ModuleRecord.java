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

import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.registry.federationserviceapi.ResolverModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustAnchorModuleResponse;
import se.swedenconnect.oidf.registry.federationserviceapi.TrustMarkIssuerModuleResponse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data response from registry to configure runtime modules from.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class ModuleRecord implements Serializable {
  private List<ResolverModuleResponse> resolvers;
  private List<TrustAnchorModuleResponse> trustAnchors;
  private List<TrustMarkIssuerModuleResponse> trustMarkIssuers;

  /**
   * Default constructor.
   */
  public ModuleRecord() {
    this.resolvers = List.of();
    this.trustAnchors = List.of();
    this.trustMarkIssuers = List.of();
  }

  /**
   * Creates instance from json object {@link HashMap}
   *
   * @param json to read
   * @return new instance
   */
  public static ModuleRecord fromJson(final Map<String, Object> json) {
    final ModuleRecord response = new ModuleRecord();
    response.resolvers = Optional.ofNullable(json.get("resolvers")).map(resolvers -> {
          return ((List<Map<String, Object>>) resolvers)
              .stream()
              .map(ResolverModuleResponse::fromJson)
              .toList();
        })
        .orElse(List.of());
    response.trustAnchors = Optional.ofNullable(json.get("trust_anchors")).map(ta -> {
      return ((List<Map<String, Object>>) ta)
          .stream()
          .map(TrustAnchorModuleResponse::fromJson)
          .toList();
    }).orElse(List.of());

    response.trustMarkIssuers = Optional.ofNullable(json.get("trust_mark_issuers")).map(tmi -> {
      return ((List<Map<String, Object>>) tmi)
          .stream()
          .map(TrustMarkIssuerModuleResponse::fromJson)
          .toList();
    }).orElse(List.of());
    return response;
  }

}
