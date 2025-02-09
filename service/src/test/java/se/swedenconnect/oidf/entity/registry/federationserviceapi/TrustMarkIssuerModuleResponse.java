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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Map;

/**
 * TrustMarkIssuer module from registry.
 *
 * @author Felix Hellman
 */
@Getter
@ToString
public class TrustMarkIssuerModuleResponse {
  private Duration trustMarkTokenValidityDuration;
  private EntityID entityIdentifier;
  private JWK jwk;
  private String alias;
  private Boolean active;

  /**
   * Converts json object {@link java.util.HashMap} to new instance
   *
   * @param json to read
   * @return new instance
   */
  public static TrustMarkIssuerModuleResponse fromJson(final Map<String, Object> json) {
    final TrustMarkIssuerModuleResponse response = new TrustMarkIssuerModuleResponse();

    response.entityIdentifier = (EntityID) json.get("entityIdentifier");
    response.jwk = (JWK) json.get("jwk");
    response.alias = (String) json.get("alias");
    response.active = (Boolean) json.get("active");
    response.trustMarkTokenValidityDuration = (Duration) json.get("trustMarkTokenValidityDuration");
    return response;
  }

  public void validate() {
    Assert.notNull(entityIdentifier, "entityIdentifier");
    Assert.notNull(jwk, "jwk");
    Assert.notNull(alias, "alias");
    Assert.notNull(active, "active");
    Assert.notNull(trustMarkTokenValidityDuration, "trustMarkTokenValidityDuration");
  }


}
