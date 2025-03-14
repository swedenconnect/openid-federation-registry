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

import com.nimbusds.jose.jwk.JWKSet;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Data class for resolver modules from registry.
 *
 * @author Felix Hellman
 */
@Getter
@ToString
public class ResolverModuleResponse {
  private String trustAnchors;
  private Duration resolveResponseDuration;
  private JWKSet trustedKeys;
  private String entityIdentifier;
  private Duration stepRetryTime;
  private String alias;
  private Boolean active;

  /**
   * Creates new instance from a json object {@link HashMap}
   *
   * @param json to read
   * @return new instance
   */
  public static ResolverModuleResponse fromJson(final Map<String, Object> json) {
    try {
      final ResolverModuleResponse resolver = new ResolverModuleResponse();
      final Boolean isModuleActive = (Boolean) json.get("active");
      resolver.active = isModuleActive;
      if (isModuleActive) {
        resolver.trustAnchors = (String) json.get("trust-anchor"); // expects trust-anchorS List<String>
        resolver.resolveResponseDuration = Duration.parse((String) json.get("resolve-response-duration"));
        resolver.trustedKeys = JWKSet.parse((String) json.get("trusted-keys"));
        resolver.entityIdentifier = (String) json.get("entity-identifier");
        resolver.stepRetryTime =
            Duration.parse((String) json.get("step-retry-duration")); // changed from step-retry-time
        resolver.alias = (String) json.get("alias");

      }
      return resolver;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }

  }

  public void validate() {
    Assert.notNull(entityIdentifier, "entityIdentifier");
    Assert.notNull(active, "active");
    Assert.notNull(trustAnchors, "trustAnchors");
    Assert.notNull(resolveResponseDuration, "resolveResponseDuration");
    Assert.notNull(trustedKeys, "trustedKeys");
    Assert.notNull(stepRetryTime, "stepRetryTime");
    Assert.notNull(trustAnchors, "trustAnchors");

  }
}