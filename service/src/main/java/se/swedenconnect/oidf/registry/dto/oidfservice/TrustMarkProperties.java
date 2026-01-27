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
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class TrustMarkProperties {
  @SerializedName("trust-mark-id")
  private EntityID trustMarkId;
  @SerializedName("logo-uri")
  private String logoUri;
  @SerializedName("ref-uri")
  private String refUri;
  @SerializedName("delegation")
  private TrustMarkDelegation delegation;
  @SerializedName("trust-mark-subjects")
  private List<TrustMarkSubjectProperty> trustMarkSubjects;

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    FederationAssert.assertNotEmpty(this.trustMarkId, "TrustMarkId is expected");
  }
}
