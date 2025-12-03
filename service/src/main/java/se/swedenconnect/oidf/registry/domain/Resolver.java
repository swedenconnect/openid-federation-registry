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

package se.swedenconnect.oidf.registry.domain;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;

/**
 * Represents a resolver module that is connected to an entity
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resolver implements FederationModule, Serializable, ToJson {

  /** Entity config */
  private EntityID entityId;

  /** If this module instance should be active or not */
  @Builder.Default
  private boolean active = true;

  /** Duration of the response. Expressed in hours. */
  @Builder.Default
  private String resolveResponseDuration = Duration.ofHours(1).toString();

  /** URL to trustanchor that will be used to build trust chain */
  private EntityID trustAnchor;

  /** Trusted keys, JWKS format */
  private JWKSet trustedKeys;

  /** Time between a failed step and retry. */
  @Builder.Default
  private String stepRetryDuration = Duration.ofHours(1).toString();
}
