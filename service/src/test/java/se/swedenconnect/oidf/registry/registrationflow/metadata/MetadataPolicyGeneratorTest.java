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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Applies the Sweden Connect template to the non-normative examples of the OIDC metadata requirements, section 6.2,
 * and checks the outcome claim by claim.
 */
class MetadataPolicyGeneratorTest {

  private static MetadataPolicyTemplate swedenConnect;

  private static final Function<String, FetchedResource> AN_IMAGE =
      url -> new FetchedResource(url, "image/svg+xml", 14_000L, "data:image/svg+xml;base64,PHN2Zy8+", null);

  @BeforeAll
  static void loadPreset() throws Exception {
    try (InputStream preset = MetadataPolicyGeneratorTest.class.getResourceAsStream(
        "/metadata-policy/sweden-connect.json")) {
      swedenConnect = JsonMapper.builder().build().readValue(preset, MetadataPolicyTemplate.class);
    }
  }

  @Test
  @DisplayName("The shipped Sweden Connect template has no problems")
  void presetIsValid() {
    assertThat(swedenConnect.problems()).isEmpty();
  }

  @Test
  @DisplayName("Relying Party example: names pinned per language, display_name derived from client_name")
  void relyingPartyExample() {
    final Map<String, Object> metadata = Map.of("openid_relying_party", Map.of(
        "organization_name#sv", "Myndigheten för digital förvaltning",
        "organization_name#en", "The Swedish Agency for Digital Government",
        "organization_identifier", "urn:glue:iso6523:0007:2021006883",
        "client_name#sv", "Min tjänst",
        "client_name#en", "My service",
        "logo_uri", "https://www.example.se/logo.svg",
        "redirect_uris", List.of("https://rp.example.se/cb")));

    final MetadataPolicyGenerator.Result result = MetadataPolicyGenerator.generate(swedenConnect, metadata,
        new OrganizationData("Myndigheten för digital förvaltning", "2021006883"), AN_IMAGE);

    assertThat(result.failures()).isEmpty();
    assertThat(result.metadata()).isEqualTo(Map.of("openid_relying_party", Map.of(
        "organization_name#sv", "Myndigheten för digital förvaltning",
        "organization_name#en", "The Swedish Agency for Digital Government",
        "organization_name", "Myndigheten för digital förvaltning",
        "organization_identifier", "urn:glue:iso6523:0007:2021006883")));
    assertThat(result.metadataPolicy()).isEqualTo(Map.of("openid_relying_party", Map.of(
        "display_name#sv", Map.of("value", "Min tjänst"),
        "display_name#en", Map.of("value", "My service"),
        "display_name", Map.of("value", "Min tjänst"),
        "client_name#sv", Map.of("value", "Min tjänst"),
        "client_name#en", Map.of("value", "My service"),
        "client_name", Map.of("value", "Min tjänst"),
        "logo_uri", Map.of("value", "https://www.example.se/logo.svg"))));
    assertThat(result.checks()).extracting(PreviewSection.Check::message)
        .contains("display_name is not declared; taken from client_name");
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("OpenID Provider example: its own display_name and the checked acr values are pinned")
  void openIdProviderExample() {
    final List<String> acr = List.of("http://id.elegnamnden.se/loa/1.0/loa3", "http://id.elegnamnden.se/loa/1.0/loa2");
    final Map<String, Object> metadata = Map.of("openid_provider", Map.of(
        "organization_name#sv", "Acme AB",
        "organization_name#en", "Acme Inc.",
        "organization_identifier", "urn:glue:iso6523:0007:2021006883",
        "display_name#sv", "Exampel OP:n",
        "display_name#en", "Example Authenticator",
        "logo_uri", "https://www.acme.se/logo.svg",
        "acr_values_supported", acr));

    final MetadataPolicyGenerator.Result result = MetadataPolicyGenerator.generate(swedenConnect, metadata,
        new OrganizationData("Acme AB", "2021006883"), AN_IMAGE);

    assertThat(result.failures()).isEmpty();
    assertThat((Map<String, Object>) result.metadata().get("openid_provider"))
        .containsEntry("organization_name", "Acme AB");
    assertThat((Map<String, Object>) result.metadataPolicy().get("openid_provider"))
        .containsEntry("display_name", Map.of("value", "Exampel OP:n"))
        .containsEntry("acr_values_supported", Map.of("value", acr))
        .doesNotContainKey("client_name");
  }

  @Test
  @DisplayName("Missing languages, a wrong organization identifier and an http logo are each reported")
  void failuresAreReported() {
    final Map<String, Object> metadata = Map.of("openid_relying_party", Map.of(
        "organization_name#en", "Example Municipality",
        "organization_identifier", "urn:glue:iso6523:0007:2120000002",
        "client_name#en", "Citizen portal",
        "logo_uri", "http://bad.example.se/logo.png"));

    final MetadataPolicyGenerator.Result result = MetadataPolicyGenerator.generate(swedenConnect, metadata,
        new OrganizationData("Exempelkommun", "2120000001"),
        url -> FetchedResource.notFetched(url, "Not fetched: only HTTPS URLs are fetched"));

    assertThat(result.failures()).containsExactlyInAnyOrder(
        "organization_name is missing language #sv",
        "organization_identifier does not match the organization's organization identifier: expected "
            + "urn:glue:iso6523:0007:2120000001",
        "client_name is missing language #sv",
        "display_name is missing language #sv",
        "logo_uri is not an HTTPS URL: http://bad.example.se/logo.png",
        "logo_uri does not resolve to an image: Not fetched: only HTTPS URLs are fetched");
  }

  @Test
  @DisplayName("An entity type the template has no rules for is left out; organization and static sources work")
  void organizationAndStaticSources() {
    final MetadataPolicyTemplate template = new MetadataPolicyTemplate(null, null, null, Map.of(
        "federation_entity", List.of(
            new MetadataPolicyTemplate.Rule("organization_name", MetadataPolicyTemplate.Claim.METADATA, null,
                MetadataPolicyTemplate.Source.ORGANIZATION, MetadataPolicyTemplate.OrganizationField.LEGAL_NAME,
                null, null, null, true, null, null, null, null),
            new MetadataPolicyTemplate.Rule("contacts", MetadataPolicyTemplate.Claim.METADATA_POLICY, "essential",
                MetadataPolicyTemplate.Source.STATIC, null, null, null, true, null, null, null, null, null))));

    final MetadataPolicyGenerator.Result result = MetadataPolicyGenerator.generate(template,
        Map.of("federation_entity", Map.of(), "openid_relying_party", Map.of("client_name", "x")),
        new OrganizationData("Acme AB", "5560000001"), AN_IMAGE);

    assertThat(result.metadata()).isEqualTo(Map.of("federation_entity", Map.of("organization_name", "Acme AB")));
    assertThat(result.metadataPolicy())
        .isEqualTo(Map.of("federation_entity", Map.of("contacts", Map.of("essential", true))));
  }
}
