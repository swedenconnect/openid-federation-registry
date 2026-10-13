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
package se.swedenconnect.oidf.registry.registrationflow.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataPolicyTemplateTest {

  private static MetadataPolicyTemplate.Rule rule(final MetadataPolicyTemplate.Claim claim, final String operator,
      final MetadataPolicyTemplate.Source source, final Boolean multilingual) {
    return new MetadataPolicyTemplate.Rule("client_name", claim, operator, source, null, null, null, null, null,
        multilingual, null, null, null);
  }

  @Test
  @DisplayName("A template without entity types cannot be used")
  void needsEntityTypes() {
    assertThat(new MetadataPolicyTemplate(null, null, null, Map.of()).problems())
        .containsExactly("entity_types must name at least one entity type");
  }

  @Test
  @DisplayName("Each inconsistent rule is reported with where it is")
  void reportsInconsistentRules() {
    final MetadataPolicyTemplate template = new MetadataPolicyTemplate(null, null, null, Map.of(
        "openid_relying_party", List.of(
            rule(MetadataPolicyTemplate.Claim.METADATA, "add", MetadataPolicyTemplate.Source.ENTITY, null),
            rule(MetadataPolicyTemplate.Claim.METADATA_POLICY, "pin", MetadataPolicyTemplate.Source.ENTITY, null),
            rule(MetadataPolicyTemplate.Claim.METADATA, null, MetadataPolicyTemplate.Source.ORGANIZATION, true),
            rule(MetadataPolicyTemplate.Claim.METADATA, null, MetadataPolicyTemplate.Source.STATIC, null))));

    assertThat(template.problems()).containsExactly(
        "openid_relying_party[0]: a metadata rule sets the value and takes no operator",
        "openid_relying_party[1]: operator must be one of " + MetadataPolicyTemplate.OPERATORS,
        "openid_relying_party[2]: organization_field must be given for source organization",
        "openid_relying_party[2]: only values from the entity's metadata can be multilingual",
        "openid_relying_party[3]: static_value must be given for source static");
  }
}
