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
package se.swedenconnect.oidf.registry.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetadataPolicies}.
 */
class MetadataPoliciesTest {

  @Test
  @DisplayName("value null and one_of with a boolean are kept when combined with the superior's policy")
  void combineKeepsValueNullAndBooleanOneOf() {
    final Map<String, Object> removed = new HashMap<>();
    removed.put("value", null);
    final Map<String, Object> superior = Map.of(MetadataPolicies.ROOT, Map.of("openid_relying_party", Map.of(
        "id_token_signed_response_alg", removed,
        "require_auth_time", Map.of("one_of", List.of(true)))));
    final Map<String, Object> subordinate = Map.of(MetadataPolicies.ROOT, Map.of("openid_relying_party", Map.of(
        "userinfo_signed_response_alg", removed,
        "contacts", Map.of("add", List.of("ops@example.se")))));

    final Map<String, Object> combined = MetadataPolicies.combine(superior, subordinate);

    assertThat(MetadataPolicies.problems(superior)).isEmpty();
    assertThat(MetadataPolicies.problems(subordinate)).isEmpty();
    @SuppressWarnings("unchecked")
    final Map<String, Object> rp = (Map<String, Object>) ((Map<String, Object>) combined.get(MetadataPolicies.ROOT))
        .get("openid_relying_party");
    assertThat(rp.get("id_token_signed_response_alg")).isEqualTo(removed);
    assertThat(rp.get("userinfo_signed_response_alg")).isEqualTo(removed);
    assertThat(rp.get("require_auth_time")).isEqualTo(Map.of("one_of", List.of(true)));
  }
}
