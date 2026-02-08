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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;

/**
 * Data class for entity record.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class EntityRecord implements Serializable {
  public static final ExclusionStrategy EXCLUSION_STRATEGY = new ExclusionStrategy() {
    @Override
    public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
      return fieldAttributes.getDeclaringClass().equals(KeyStore.class);
    }

    @Override
    public boolean shouldSkipClass(final Class<?> aClass) {
      return aClass.equals(KeyStore.class);
    }
  };

  @SerializedName("entity-identifier")
  private EntityID entityIdentifier;
  @SerializedName("metadata")
  private Map<String, Object> metadata;
  @SerializedName("trust-mark-source")
  private List<TrustMarkSourceProperty> trustMarkSource;
  @SerializedName("ec-location")
  private String ecLocation;
  @SerializedName("authority-hints")
  private List<String> authorityHints;
  @SerializedName("jwks")
  private JWKSet jwks;
  @SerializedName("crit")
  private List<String> crit;
}
