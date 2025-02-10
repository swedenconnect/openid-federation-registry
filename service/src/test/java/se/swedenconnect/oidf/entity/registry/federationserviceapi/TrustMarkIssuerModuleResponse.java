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

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  private String alias;
  private Boolean active;
  private List<TrustMarkResponse> trustMarks;
  /**
   * Converts json object {@link java.util.HashMap} to new instance
   *
   * @param json to read
   * @return new instance
   */
  public static TrustMarkIssuerModuleResponse fromJson(final Map<String, Object> json) {
    final TrustMarkIssuerModuleResponse response = new TrustMarkIssuerModuleResponse();

    try {
      response.entityIdentifier = EntityID.parse((String) json.get("entity-identifier"));

      response.alias = (String) json.get("alias");
      response.active = (Boolean) json.get("active");
      response.trustMarkTokenValidityDuration =
          Duration.parse((String) json.get("trust-mark-token-validity-duration"));
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }

    return response;
  }

  public void validate() {
    Assert.notNull(entityIdentifier, "entityIdentifier");
    Assert.notNull(alias, "alias");
    Assert.notNull(active, "active");
    Assert.notNull(trustMarkTokenValidityDuration, "trustMarkTokenValidityDuration");
  }

  public record TrustMarkResponse(String trustMarkId, Optional<String> logoUri, Optional<String> refUri,
      Optional<String> delegation) {}
}
