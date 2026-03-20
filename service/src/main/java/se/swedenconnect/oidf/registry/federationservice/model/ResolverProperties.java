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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;

/**
 * Properties for the resolver.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ToString
@Builder
public final class ResolverProperties {
  @SerializedName("trust-anchor")
  private String trustAnchor;
  @SerializedName("resolver-response-duration")
  private Duration resolveResponseDuration;
  @SerializedName("trusted-keys")
  private JWKSet trustedKeys;
  @SerializedName("entity-identifier")
  private String entityIdentifier;
  @SerializedName("step-retry-time")
  private Duration stepRetryTime;
  @SerializedName("use-cached-value")
  private int useCachedValue;

  /**
   * @param trustAnchor The trust anchor used by this resolve entity
   * @param resolveResponseDuration The validity duration of issued resolve responses
   * @param trustedKeys Keys trusted by this resolver to validate Entity Statement chains
   * @param entityIdentifier for the resolver
   * @param stepRetryTime time to wait before retrying a step that has failed
   * @param useCachedValue threshold for error context
   */
  public ResolverProperties(
      final String trustAnchor,
      final Duration resolveResponseDuration,
      final JWKSet trustedKeys,
      final String entityIdentifier,
      final Duration stepRetryTime,
      final int useCachedValue) {
    this.trustAnchor = trustAnchor;
    this.resolveResponseDuration = resolveResponseDuration;
    this.trustedKeys = trustedKeys;
    this.entityIdentifier = entityIdentifier;
    this.stepRetryTime = stepRetryTime;
    this.useCachedValue = useCachedValue;
  }
}

