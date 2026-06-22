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
package se.swedenconnect.oidf.registry.guioperations.dto;

import com.nimbusds.jose.jwk.JWKSet;

import java.util.List;

/**
 * Represents the verified payload from the oidf-service {@code /jwks} endpoint.
 * <p>
 * The JWT payload has this structure:
 * <pre>
 * {
 *   "federation": { "keys": [...] },
 *   "hosted":     { "keys": [...] },
 *   "name": {
 *     "federation": ["federation:sign-key-1"],
 *     "hosted":     ["hosted:sign-key-2"]
 *   }
 * }
 * </pre>
 *
 * @param federation keys used to sign federation entity statements
 * @param hosted keys used to sign hosted entity statements
 * @param names human-readable aliases for the keys, grouped by type
 * @author Per Fredrik Plars
 */
public record JwksPayloadDto(JWKSet federation, JWKSet hosted, KeyNames names) {

  /**
   * Human-readable aliases for each key group, sourced from the {@code name} claim.
   *
   * @param federation aliases for the federation signing keys
   * @param hosted aliases for the hosted signing keys
   */
  public record KeyNames(List<String> federation, List<String> hosted) {

    /** Returns an empty {@link KeyNames} when the {@code name} claim is absent. */
    public static KeyNames empty() {
      return new KeyNames(List.of(), List.of());
    }
  }
}
