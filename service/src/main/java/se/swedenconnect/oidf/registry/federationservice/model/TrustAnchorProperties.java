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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Property class for trust anchor.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrustAnchorProperties {
  @SerializedName("entity-identifier")
  private EntityID entityIdentifier;

  @SerializedName("trust-mark-issuers")
  private Map<EntityID, List<EntityID>> trustMarkIssuers;

  @SerializedName("trust-mark-owners")
  private List<TrustMarkOwner> trustMarkOwners;

  @SerializedName("subordinates")
  private List<SubordinateListingProperty> subordinates;

  /**
   * Constructor.
   *
   * @param entityIdentifier of the trust anchor
   * @param trustMarkIssuers that are valid for this trust anchor
   * @param trustMarkOwners to trust
   */
  public TrustAnchorProperties(
      final EntityID entityIdentifier,
      final Map<EntityID, List<EntityID>> trustMarkIssuers,
      final List<TrustMarkOwner> trustMarkOwners) {
    this.entityIdentifier = entityIdentifier;
    this.trustMarkIssuers = trustMarkIssuers;
    this.trustMarkOwners = trustMarkOwners;
  }

  /**
   * @return json of trust mark owners
   */
  public Map<String, Object> trustMarkOwnersJson() {
    if (Objects.isNull(this.trustMarkOwners)) {
      return Map.of();
    }
    return this.getTrustMarkOwners().stream()
        .collect(Collectors.toMap(
            k -> k.getTrustmarkIdentifier().getValue(),
            k -> Map.of(
                "sub", k.getSub().getValue(),
                "jwks", k.getJwks().toPublicJWKSet().toJSONObject()
            )
        ));
  }

  /**
   * Entity of an individual subordinate.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  @ToString
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class SubordinateListingProperty {

    @SerializedName("crit")
    private List<String> crit;
    @SerializedName("metadata-policy-crit")
    private List<String> metadataPolicyCrit;
    @SerializedName("entity-identifier")
    private EntityID entityIdentifier;
    @SerializedName("ec-location")
    private String overrideConfigurationLocation;
    @SerializedName("virtual-entity-id")
    private String virtualEntityId;
    @SerializedName("policy")
    private PolicyRecord policy;
    @SerializedName("jwks")
    private JWKSet jwks;
    @SerializedName("constraints")
    private ConstraintRecord constraints;

    /**
     * Create SubordinateListingProperty from entityId
     *
     * @param entityIdentifier to create
     * @return new subordinate listing property
     */
    public static SubordinateListingProperty fromEntityId(final EntityID entityIdentifier) {
      final SubordinateListingProperty subordinateListingProperty = new SubordinateListingProperty();
      subordinateListingProperty.entityIdentifier = entityIdentifier;
      return subordinateListingProperty;
    }
  }
}
