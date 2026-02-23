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
package se.swedenconnect.oidf.registry.federation.model;

import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * Properties for TrustMarkIssuer
 *
 * @param trustMarkValidityDuration The validity duration of issued Trust Marks
 * @param entityIdentifier IssuerEntityId
 * @param trustMarks TrustMark Issuer
 * @author Per Fredrik Plars
 */
@Builder
@Slf4j
public record TrustMarkIssuerProperties(
    @SerializedName("trust-mark-validity-duration") Duration trustMarkValidityDuration,
    @SerializedName("entity-identifier") EntityID entityIdentifier,
    @SerializedName("trust-marks") List<TrustMarkProperties> trustMarks) {

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    FederationAssert.assertNotEmpty(this.trustMarkValidityDuration, "TrustMarkValidityDuration is expected");
    FederationAssert.assertNotEmpty(this.entityIdentifier, "IssuerEntityId is expected");
    FederationAssert.assertNotEmpty(this.trustMarks, "TrustMarks is expected");
    FederationAssert.assertTrue(this.trustMarkValidityDuration.minus(Duration.ofMinutes(4)).isPositive(),
        "Expect trustMarkValidityDuration to be grater than 5 minutes. Current value:'%s'"
            .formatted(this.trustMarkValidityDuration));

    this.trustMarks.forEach(TrustMarkProperties::validate);
  }
}
