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
package se.swedenconnect.oidf.registry.registrationflow.process;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersistedContextTest {

  @Test
  @DisplayName("Saved values survive a round trip through JSON and come back with their original types")
  void roundTripThroughJson() throws Exception {
    final JWKSet jwks = new JWKSet(new ECKeyGenerator(Curve.P_256).keyID("k1").generate().toPublicJWK());
    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.ENTITY_CONFIGURATION_METADATA,
        new JSONObject(Map.of("openid_relying_party", Map.of("client_name", "Min tjänst"))));
    ctx.put(ContextKey.METADATA_POLICY,
        new JSONObject(Map.of("openid_relying_party", Map.of("logo_uri", Map.of("value", "https://x.se/l.svg")))));
    ctx.put(ContextKey.REQUEST_METADATA, new JSONObject(Map.of("openid_relying_party", Map.of())));
    ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, jwks);
    ctx.put(ContextKey.ENTITY_ID, "https://rp.example.se");

    final JsonMapper mapper = JsonMapper.builder().build();
    final String stored = mapper.writeValueAsString(PersistedContext.capture(ctx));
    final Map<String, Object> loaded = mapper.readValue(stored, new TypeReference<Map<String, Object>>() {});

    final ProcessContext resumed = new ProcessContext();
    PersistedContext.restore(loaded, resumed);

    assertThat(resumed.<JSONObject>get(ContextKey.ENTITY_CONFIGURATION_METADATA)).get()
        .isEqualTo(ctx.<JSONObject>get(ContextKey.ENTITY_CONFIGURATION_METADATA).get());
    assertThat(resumed.<JSONObject>get(ContextKey.METADATA_POLICY)).get()
        .isEqualTo(ctx.<JSONObject>get(ContextKey.METADATA_POLICY).get());
    assertThat(resumed.<JSONObject>get(ContextKey.REQUEST_METADATA)).isPresent();
    assertThat(resumed.<JWKSet>get(ContextKey.ENTITY_CONFIGURATION_JWKS)).get()
        .satisfies(restored -> assertThat(restored.getKeyByKeyId("k1")).isEqualTo(jwks.getKeyByKeyId("k1")));
    assertThat(resumed.<String>get(ContextKey.ENTITY_ID))
        .as("Values identifying the registration are rebuilt from the registration, not saved")
        .isEmpty();
  }

  @Test
  @DisplayName("Nothing to save gives an empty map, and restoring null or unknown keys is a no-op")
  void emptyAndUnknown() {
    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.REGISTRATION_ID, UUID.randomUUID());
    assertThat(PersistedContext.capture(ctx)).isEmpty();

    final ProcessContext resumed = new ProcessContext();
    PersistedContext.restore(null, resumed);
    PersistedContext.restore(Map.of("removed_in_later_version", "x"), resumed);
    assertThat(resumed.snapshot()).isEmpty();
  }
}
