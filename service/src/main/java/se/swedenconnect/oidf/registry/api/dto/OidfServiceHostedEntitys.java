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
import java.util.Objects;

/**
 * Representation of the HostedEntities in OidcService controller
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OidfServiceHostedEntitys {

  @JsonProperty("entity_records")
  @SerializedName("entity_records")
  private List<Record> entityRecords;

  /**
   * Represents a Record containing details of a hosted record,
   *
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
    private PolicyRecord policyRecord;

    private String subject;
    private String issuer;

    private JWKS jwks;
  }

  /**
   * Represents a hosted record containing metadata details.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class HostedRecord {
    private Metadata metadata;
  }

  /**
   * Represents metadata associated with a federation entity.
   *
   * This metadata includes detailed information about the federation entity, making it possible to manage and retrieve
   * entity-specific data for OpenID Connect Federation services.
   *
   * Serialization and deserialization annotations are included to support the integration with frameworks like Jackson
   * and Gson, ensuring compatibility with JSON-based APIs.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Metadata {
    @JsonProperty("federation_entity")
    @SerializedName("federation_entity")
    private FederationEntity federationEntity;
  }

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

  }

  /**
   * Represents a record containing policy details for a hosted entity.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class PolicyRecord {
    private Map<String, Objects> policy;
  }

  /**
   * Represents a JSON Web Key Set (JWKS), which is a JSON data structure that represents a set of public keys.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class JWKS {
    private List<Map<String, Objects>> keys;
  }

}

