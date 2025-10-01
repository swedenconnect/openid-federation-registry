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
package se.swedenconnect.oidf.registry.federationserviceapi;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Data class for resolver modules from registry.
 *
 * @author Felix Hellman
 */
@Getter
@ToString
public class ResolverModuleResponse {
  private List<String> trustAnchors;
  private Duration resolveResponseDuration;
  private JWKSet trustedKeys;
  private String entityIdentifier;
  private Duration stepRetryTime;

  public static ResolverModuleResponse fromJson(final Map<String, Object> json) {
    final ResolverModuleResponse resolver = new ResolverModuleResponse();
    resolver.trustAnchors = List.of((String) json.get(RecordFields.ResolverModule.TRUST_ANCHORS));
    resolver.resolveResponseDuration =
        Duration.parse((String) json.get(RecordFields.ResolverModule.RESOLVE_RESPONSE_DURATION));

    resolver.entityIdentifier = (String) json.get(RecordFields.ResolverModule.ENTITY_IDENTIFIER);
    resolver.stepRetryTime = Duration.parse((String) json.get(RecordFields.ResolverModule.STEP_RETRY_TIME));

    try {
      resolver.trustedKeys = JWKSet.parse((String) json.get(RecordFields.ResolverModule.TRUSTED_KEYS));
    }
    catch (final ParseException e) {
      throw new IllegalArgumentException("Unable to parse trusted-keys in to a JWKSet.", e);
    }
    return resolver;
  }


  public void validate() {
    Assert.notNull(entityIdentifier, "entityIdentifier");
    Assert.notNull(trustAnchors, "trustAnchors");
    Assert.notNull(resolveResponseDuration, "resolveResponseDuration");
    Assert.notNull(trustedKeys, "trustedKeys");
    Assert.notNull(stepRetryTime, "stepRetryTime");
    Assert.notNull(trustAnchors, "trustAnchors");

  }
}