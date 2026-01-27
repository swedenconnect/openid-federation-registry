/*
 * Copyright 2024-2025 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.registry.dto.oidfservice;

import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

import java.io.Serializable;
import java.util.Map;

/**
 * Data class for trust mark source.
 *
 * @param issuer of the trust mark
 * @param trustMarkId of the trust mark
 * @author Felix Hellman
 */
public record TrustMarkSourceRecord(@SerializedName("issuer") EntityID issuer,
    @SerializedName("trust-mark-id") String trustMarkId) implements Serializable {

  /**
   * Creates a new instance from json object.
   *
   * @param tmsJson to create instance from
   * @return new instance
   */
  public static TrustMarkSourceRecord fromJson(final Map<String, Object> tmsJson) {
    return new TrustMarkSourceRecord(
        new EntityID((String) tmsJson.get(RecordFields.TrustMarkSource.ISSUER)),
        (String) tmsJson.get(RecordFields.TrustMarkSource.TRUST_MARK_ID)
    );
  }

  /**
   * Convert current instance to json structure for nimbus.
   *
   * @return json of this instance
   */
  public Map<String, Object> toJson() {
    return Map.of(
        RecordFields.TrustMarkSource.ISSUER, this.issuer.getValue(),
        RecordFields.TrustMarkSource.TRUST_MARK_ID, this.trustMarkId
    );
  }
}
