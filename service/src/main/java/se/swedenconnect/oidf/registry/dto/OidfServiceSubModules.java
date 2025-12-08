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

package se.swedenconnect.oidf.registry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OidfServiceSubModules reply structure for federation api
 *
 * @author Per Fredrik Plars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OidfServiceSubModules {

  @JsonProperty("trust_anchors")
  @SerializedName("trust_anchors")
  private List<TrustAnchor> trustAnchors;

  @JsonProperty("resolvers")
  private List<Resolver> resolvers;

  @JsonProperty("trust_mark_issuers")
  @SerializedName("trust_mark_issuers")
  private List<TrustMarkIssuer> trustMarkIssuers;

  /**
   * Represents a trust anchor entity definition. This class includes annotations to support serialization and JSON
   * handling, allowing seamless integration with APIs using libraries like Jackson and Gson.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TrustAnchor {

    @JsonProperty("entity_identifier")
    @SerializedName("entity_identifier")
    private String entityIdentifier;

    @JsonProperty("trust_mark_issuer")
    @SerializedName("trust_mark_issuer")
    private String trustMarkIssuer;

    @JsonProperty("active")
    private Boolean active;

  }

  /**
   * Represents a Resolver configuration used in the OIDC Federation implementation.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Resolver {

    @JsonProperty("resolve_response_duration")
    @SerializedName("resolve_response_duration")
    private String resolveResponseDuration;

    @JsonProperty("entity_identifier")
    @SerializedName("entity_identifier")
    private String entityIdentifier;

    @JsonProperty("trusted_keys")
    @SerializedName("trusted_keys")
    private String trustedKeys;

    // This will later change to List<String>
    @JsonProperty("trust_anchors")
    @SerializedName("trust_anchors")
    private String trustAnchors;

    @JsonProperty("step_retry_time")
    @SerializedName("step_retry_time")
    private String stepRetryTime;

    @JsonProperty("step_cached_value_threshold")
    @SerializedName("step_cached_value_threshold")
    private Integer stepCachedValueThreshold;
  }

  /**
   * Representation of a TrustMarkIssuer which includes details about the entity issuing trust marks, validity duration,
   * and the list of trust marks issued by the entity.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TrustMarkIssuer {

    @JsonProperty("entity_identifier")
    @SerializedName("entity_identifier")
    private String entityIdentifier;

    @JsonProperty("trust_mark_token_validity_duration")
    @SerializedName("trust_mark_token_validity_duration")
    private String trustMarkTokenValidityDuration;

    @JsonProperty("trust_marks")
    @SerializedName("trust_marks")
    private List<TrustMark> trustMarks;

    /**
     * Represents a TrustMark issued by an entity.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrustMark {

      @JsonProperty("trust_mark_issuer_id")
      @SerializedName("trust_mark_issuer_id")
      private String trustMarkIssuerId;

      @JsonProperty("delegation")
      @SerializedName("delegation")
      private String delegation;

      @JsonProperty("ref")
      @SerializedName("ref")
      private String ref;

      @JsonProperty("logo_uri")
      @SerializedName("logo_uri")
      private String logoUri;

      @JsonProperty("trust_mark_id")
      @SerializedName("trust_mark_id")
      private String trustMarkId;

      @JsonProperty("trust_mark_subjects")
      @SerializedName("trust_mark_subjects")
      private List<TrustMarkSubject> trustMarkSubjects;

      /**
       * Represents a subject associated with a trust mark, including details about its status and validity period.
       *
       * The fields "expires" and "granted" should be serialized and exported in ISO_INSTANT format.
       */
      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class TrustMarkSubject {
        @JsonProperty("subject")
        private String subject;

        @JsonProperty("revoked")
        private Boolean revoked;
        // This data should be exported as ISO_INSTANT format
        @JsonProperty("expires")
        private String expires;
        // This data should be exported as ISO_INSTANT format
        @JsonProperty("granted")
        private String granted;
      }

    }
  }

}
