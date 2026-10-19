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

  @Test
  @DisplayName("Tells which parameters of the entity configuration a pinned value comes from")
  void entityParameters() {
    final MetadataPolicyTemplate template = new MetadataPolicyTemplate("sv", null, null, Map.of(
        "openid_relying_party", List.of(
            new MetadataPolicyTemplate.Rule("organization_name", MetadataPolicyTemplate.Claim.METADATA, null,
                MetadataPolicyTemplate.Source.ENTITY, null, null, null, null, null, true,
                MetadataPolicyTemplate.Untagged.ORGANIZATION, MetadataPolicyTemplate.OrganizationField.LEGAL_NAME,
                null),
            new MetadataPolicyTemplate.Rule("display_name", MetadataPolicyTemplate.Claim.METADATA_POLICY, "value",
                MetadataPolicyTemplate.Source.ENTITY, null, null, "client_name", null, null, true,
                MetadataPolicyTemplate.Untagged.ENTITY_OR_DEFAULT, null, null),
            new MetadataPolicyTemplate.Rule("logo_uri", MetadataPolicyTemplate.Claim.METADATA_POLICY, "value",
                MetadataPolicyTemplate.Source.ENTITY, null, null, null, null, null, null, null, null, null),
            new MetadataPolicyTemplate.Rule("organization_number", MetadataPolicyTemplate.Claim.METADATA, null,
                MetadataPolicyTemplate.Source.ORGANIZATION,
                MetadataPolicyTemplate.OrganizationField.ORGANIZATION_NUMBER,
                null, null, null, null, null, null, null, null))));

    assertThat(template.entityParameters("openid_relying_party", "organization_name")).isEmpty();
    assertThat(template.entityParameters("openid_relying_party", "organization_name#en"))
        .containsExactly("organization_name#en");
    assertThat(template.entityParameters("openid_relying_party", "display_name"))
        .containsExactly("display_name", "display_name#sv", "client_name", "client_name#sv");
    assertThat(template.entityParameters("openid_relying_party", "display_name#sv"))
        .containsExactly("display_name#sv", "client_name#sv");
    assertThat(template.entityParameters("openid_relying_party", "logo_uri")).containsExactly("logo_uri");
    assertThat(template.entityParameters("openid_relying_party", "organization_number")).isEmpty();
    assertThat(template.entityParameters("openid_relying_party", "unknown")).isEmpty();
    assertThat(template.entityParameters("openid_provider", "logo_uri")).isEmpty();
  }
}
