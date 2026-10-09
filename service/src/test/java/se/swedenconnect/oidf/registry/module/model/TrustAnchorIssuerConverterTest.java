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
package se.swedenconnect.oidf.registry.module.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule.TrustAnchorIssuerConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for how the trust mark issuers of a trust anchor are stored as JSON.
 */
class TrustAnchorIssuerConverterTest {

  private final TrustAnchorIssuerConverter converter = new TrustAnchorIssuerConverter(JsonMapper.builder().build());

  @Test
  @DisplayName("The issuers are stored as a JSON list of objects and read back unchanged")
  void roundTrip() {
    final List<TrustAnchorIssuer> issuers = List.of(
        new TrustAnchorIssuer("https://tmi1.example.com", true, List.of()),
        new TrustAnchorIssuer("https://external.example.org", false,
            List.of("https://ta.example.com/tm/a", "https://ta.example.com/tm/b")));

    final String json = this.converter.convertToDatabaseColumn(issuers);

    assertThat(json).contains("\"issuer\"", "\"auto\"", "\"trustMarkTypes\"");
    assertThat(this.converter.convertToEntityAttribute(json)).isEqualTo(issuers);
  }

  @Test
  @DisplayName("The JSON that migration V32 makes from the old list of entity identifiers can be read")
  void readsTheJsonThatTheMigrationWrites() {
    final String migrated = "[{\"issuer\": \"https://tmi1.example.com\", \"auto\": true, \"trustMarkTypes\": []},"
        + "{\"issuer\": \"https://tmi2.example.com\", \"auto\": true, \"trustMarkTypes\": []}]";

    assertThat(this.converter.convertToEntityAttribute(migrated)).containsExactly(
        new TrustAnchorIssuer("https://tmi1.example.com", true, List.of()),
        new TrustAnchorIssuer("https://tmi2.example.com", true, List.of()));
  }

  @Test
  @DisplayName("Missing trust mark types and an empty list are read as empty lists")
  void readsMissingTypesAndEmptyList() {
    assertThat(this.converter.convertToEntityAttribute("[{\"issuer\": \"https://tmi1.example.com\", \"auto\": true}]"))
        .containsExactly(new TrustAnchorIssuer("https://tmi1.example.com", true, List.of()));
    assertThat(this.converter.convertToEntityAttribute("[]")).isEmpty();
  }

  @Test
  @DisplayName("No value in the database gives no issuers, and a module without issuers gives an empty list")
  void nullAndBlank() {
    assertThat(this.converter.convertToEntityAttribute(null)).isNull();
    assertThat(this.converter.convertToEntityAttribute(" ")).isNull();
    assertThat(this.converter.convertToDatabaseColumn(null)).isNull();

    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setTrustMarkIssuers(null);
    assertThat(module.getTrustMarkIssuers()).isEmpty();
  }
}
