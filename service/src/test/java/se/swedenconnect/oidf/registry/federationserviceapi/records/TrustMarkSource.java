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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

/**
 * Data class for a trust mark source.
 *
 * @author Felix Hellman
 */
@Getter
public class TrustMarkSource implements Serializable {
  private final EntityID issuer;
  private final String trustMarkId;

  /**
   * Constructor.
   *
   * @param issuer of the trust mark
   * @param trustMarkId of the trust mark
   */
  public TrustMarkSource(final EntityID issuer, final String trustMarkId) {
    this.issuer = issuer;
    this.trustMarkId = trustMarkId;
  }

  /**
   * Creates a new instance from JSON object.
   *
   * @param tmsJson to create an instance from
   * @return new instance
   */
  public static TrustMarkSource fromJson(final Map<String, Object> tmsJson) {
    return new TrustMarkSource(
        new EntityID((String) tmsJson.get("issuer")),
        (String) tmsJson.get("trust_mark_id")
    );
  }

  /**
   * Convert current instance to JSON structure for nimbus.
   *
   * @return JSON of this instance
   */
  public Map<String, Object> toJson() {
    return Map.of(
        "issuer", this.issuer.getValue(),
        "trust_mark_id", this.trustMarkId
    );
  }
}
