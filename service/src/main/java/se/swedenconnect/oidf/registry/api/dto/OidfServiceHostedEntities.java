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

package se.swedenconnect.oidf.registry.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Representation of the HostedEntities in OidcService controller
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OidfServiceHostedEntities {

  @JsonProperty("entity_records")
  @SerializedName("entity_records")
  private List<Record> entityRecords;

  /**
   * Represents a Record containing details of a hosted record.
   * <p>
   * Fields: - `hostedRecord`: Information about the hosted entity's metadata. - `policyRecord`: Details regarding the
   * policy configuration for the entity. - `subject`: The subject associated with the entity. - `issuer`: The entity
   * acting as the issuer. - `jwks`: The JSON Web Key Set containing public keys.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Record {
    @JsonProperty("hosted_record")
    @SerializedName("hosted_record")
    private HostedRecord hostedRecord;

    @JsonProperty("policy_record")
    @SerializedName("policy_record")
    private Map<String, Object> policyRecord;

    @JsonProperty("override_configuration_location")
    @SerializedName("override_configuration_location")
    private String overrideConfigurationLocation;

    @JsonProperty("metadata_policy_crit")
    @SerializedName("metadata_policy_crit")
    private List<String> metadataPolicyCrit;

    @JsonProperty("crit")
    @SerializedName("crit")
    private List<String> crit;

    private String subject;
    private String issuer;

    private Map<String, Object> jwks;
  }

  /**
   * Represents a hosted record containing metadata details.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class HostedRecord {
    private Map<String, Object> metadata;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FederationEntity {
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
  }

  /**
   * Represents metadata associated with a federation entity.
   * <p>
   * This metadata includes detailed information about the federation entity, making it possible to manage and retrieve
   * entity-specific data for OpenID Connect Federation services.
   * <p>
   * Serialization and deserialization annotations are included to support the integration with frameworks like Jackson
   * and Gson, ensuring compatibility with JSON-based APIs.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Metadata {
    // @JsonProperty("federation_entity")
    // @SerializedName("federation_entity")
    private FederationEntity federationEntity;

    // How to handle the other metadatatypes ?

    /**
     * Represents a Federation Entity Metadata
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FederationEntity {
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
  }

}