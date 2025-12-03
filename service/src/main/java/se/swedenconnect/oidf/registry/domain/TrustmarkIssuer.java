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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustmarkIssuer implements FederationModule {

  /** Entity config */
  private EntityID entityId;

  /** If this module instance should be active or not */
  private Boolean active;

  /** Validity for the token representing the trustmark. Expressed in hours. */
  @Builder.Default
  private String trustMarkTokenValidityDuration = Duration.ofHours(1).toString();
}
