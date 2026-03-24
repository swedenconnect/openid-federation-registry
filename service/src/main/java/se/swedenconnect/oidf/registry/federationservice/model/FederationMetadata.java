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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FederationMetadata {
  @JsonProperty("federation_trust_mark_endpoint")
  @SerializedName("federation_trust_mark_endpoint")
  private String federationTrustMarkEndpoint;

  @JsonProperty("federation_trust_mark_list_endpoint")
  @SerializedName("federation_trust_mark_list_endpoint")
  private String federationTrustMarkListEndpoint;

  @JsonProperty("organization_name")
  @SerializedName("organization_name")
  private String organizationName;

  @JsonProperty("federation_trust_mark_status_endpoint")
  @SerializedName("federation_trust_mark_status_endpoint")
  private String federationTrustMarkStatusEndpoint;

  @JsonProperty("federation_discovery_endpoint")
  @SerializedName("federation_discovery_endpoint")
  private String federationDiscoveryEndpoint;

  @JsonProperty("federation_resolve_endpoint")
  @SerializedName("federation_resolve_endpoint")
  private String federationResolveEndpoint;

  @JsonProperty("federation_list_endpoint")
  @SerializedName("federation_list_endpoint")
  private String federationListEndpoint;

  @JsonProperty("federation_fetch_endpoint")
  @SerializedName("federation_fetch_endpoint")
  private String federationFetchEndpoint;

}
