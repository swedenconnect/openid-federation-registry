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
package se.swedenconnect.oidf.registry.federationservice.serde;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.federationservice.model.ConstraintRecord;
import se.swedenconnect.oidf.registry.federationservice.model.ModuleRecord;
import se.swedenconnect.oidf.registry.federationservice.model.NamingConstraints;
import se.swedenconnect.oidf.registry.federationservice.model.PolicyRecord;
import se.swedenconnect.oidf.registry.federationservice.model.TrustAnchorProperties;
import se.swedenconnect.oidf.registry.federationservice.model.TrustAnchorProperties.SubordinateListingProperty;

import java.util.List;
import java.util.Map;

/**
 * Verifies that module records are serialized without null values and empty objects or arrays.
 */
class JsonRegistryLoaderTest {

  private final JsonRegistryLoader loader = new JsonRegistryLoader();

  @Test
  void subordinateWithoutValuesIsSerializedWithoutNullOrEmptyValues() {
    final SubordinateListingProperty subordinate = SubordinateListingProperty.builder()
        .entityIdentifier(new EntityID("https://sub.example.com"))
        .virtualEntityId("https://sub.example.com")
        .policy(new PolicyRecord("policy-id", Map.of()))
        .metadata(Map.of())
        .constraints(ConstraintRecord.builder()
            .naming(NamingConstraints.builder().permitted(List.of()).excluded(List.of()).build())
            .allowedEntityTypes(List.of())
            .build())
        .build();
    final ModuleRecord record = new ModuleRecord();
    record.setTrustAnchors(List.of(TrustAnchorProperties.builder()
        .entityIdentifier(new EntityID("https://ta.example.com"))
        .subordinates(List.of(subordinate))
        .build()));

    final JsonObject json = JsonParser.parseString(this.loader.toJson(record)).getAsJsonObject();

    final JsonObject sub = json.getAsJsonArray("trust-anchors").get(0).getAsJsonObject()
        .getAsJsonArray("subordinates").get(0).getAsJsonObject();
    assertThat(sub.keySet()).containsExactlyInAnyOrder("entity-identifier", "virtual-entity-id", "policy");
    assertThat(sub.getAsJsonObject("policy").keySet()).containsExactly("id");
  }

  @Test
  void topLevelListsAreAlwaysPresent() {
    final JsonObject json = JsonParser.parseString(this.loader.toJson(new ModuleRecord())).getAsJsonObject();

    assertThat(json.keySet()).containsExactlyInAnyOrder("resolvers", "trust-anchors", "trust-mark-issuers");
    assertThat(json.get("resolvers")).isEqualTo(new JsonArray());
  }

  @Test
  void valuesWithContentAreKept() {
    final SubordinateListingProperty subordinate = SubordinateListingProperty.builder()
        .entityIdentifier(new EntityID("https://sub.example.com"))
        .crit(List.of("ec_location"))
        .constraints(ConstraintRecord.builder().maxPathLength(2L).build())
        .build();
    final ModuleRecord record = new ModuleRecord();
    record.setTrustAnchors(List.of(TrustAnchorProperties.builder()
        .entityIdentifier(new EntityID("https://ta.example.com"))
        .subordinates(List.of(subordinate))
        .build()));

    final ModuleRecord parsed = this.loader.parseModuleJson(this.loader.toJson(record));

    final SubordinateListingProperty result = parsed.getTrustAnchors().getFirst().getSubordinates().getFirst();
    assertThat(result.getCrit()).containsExactly("ec_location");
    assertThat(result.getConstraints().getMaxPathLength()).isEqualTo(2L);
  }
}
