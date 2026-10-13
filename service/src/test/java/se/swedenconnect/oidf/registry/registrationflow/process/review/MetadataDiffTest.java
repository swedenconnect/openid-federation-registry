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
package se.swedenconnect.oidf.registry.registrationflow.process.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataDiffTest {

  @Test
  @DisplayName("Added, changed and removed parameters are listed per entity type; unchanged ones are not")
  void listsChangesPerParameter() {
    final Map<String, Object> before = Map.of("openid_relying_party", Map.of(
        "client_name", "Mina sidor",
        "logo_uri", "https://kommunen.se/logo-old.png",
        "policy_uri", "https://kommunen.se/policy",
        "redirect_uris", List.of("https://minasidor.kommunen.se/cb")));
    final Map<String, Object> after = Map.of("openid_relying_party", Map.of(
        "client_name", "Mina sidor",
        "logo_uri", "https://kommunen.se/logo.png",
        "client_name#en", "My pages",
        "redirect_uris", List.of("https://minasidor.kommunen.se/cb")));

    assertThat(MetadataDiff.compare(before, after)).containsExactlyInAnyOrder(
        new PreviewSection.DiffEntry(PreviewSection.ChangeType.CHANGED, "openid_relying_party.logo_uri",
            "https://kommunen.se/logo-old.png", "https://kommunen.se/logo.png"),
        new PreviewSection.DiffEntry(PreviewSection.ChangeType.REMOVED, "openid_relying_party.policy_uri",
            "https://kommunen.se/policy", null),
        new PreviewSection.DiffEntry(PreviewSection.ChangeType.ADDED, "openid_relying_party.client_name#en",
            null, "My pages"));
  }

  @Test
  @DisplayName("A new entity type shows as one added entry, and identical documents give no changes")
  void wholeEntityTypes() {
    final Map<String, Object> rp = Map.of("openid_relying_party", Map.of("client_name", "A"));
    assertThat(MetadataDiff.compare(rp, rp)).isEmpty();
    assertThat(MetadataDiff.compare(null, rp)).singleElement().satisfies(entry -> {
      assertThat(entry.changeType()).isEqualTo(PreviewSection.ChangeType.ADDED);
      assertThat(entry.key()).isEqualTo("openid_relying_party");
    });
  }
}
