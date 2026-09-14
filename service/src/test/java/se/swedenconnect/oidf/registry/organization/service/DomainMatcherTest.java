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
package se.swedenconnect.oidf.registry.organization.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the host/domain matching rule shared by registration enforcement and the rejection cascade.
 */
class DomainMatcherTest {

  @Test
  @DisplayName("A host equal to the domain matches")
  void exactHostMatches() {
    assertThat(DomainMatcher.matches("example.com", "example.com")).isTrue();
  }

  @Test
  @DisplayName("A subdomain of the domain matches, at any depth")
  void subdomainMatches() {
    assertThat(DomainMatcher.matches("sp.example.com", "example.com")).isTrue();
    assertThat(DomainMatcher.matches("a.b.example.com", "example.com")).isTrue();
  }

  @Test
  @DisplayName("A host that merely ends with the domain's characters does not match")
  void suffixWithoutDotDoesNotMatch() {
    assertThat(DomainMatcher.matches("notexample.com", "example.com")).isFalse();
    assertThat(DomainMatcher.matches("badexample.com", "example.com")).isFalse();
  }

  @Test
  @DisplayName("A parent of the domain does not match")
  void parentDoesNotMatch() {
    assertThat(DomainMatcher.matches("example.com", "sp.example.com")).isFalse();
  }

  @Test
  @DisplayName("Matching is case insensitive on both sides")
  void matchingIsCaseInsensitive() {
    assertThat(DomainMatcher.matches("SP.Example.COM", "example.com")).isTrue();
    assertThat(DomainMatcher.matches("sp.example.com", "Example.Com")).isTrue();
  }

  @Test
  @DisplayName("Null and blank inputs never match")
  void nullAndBlankNeverMatch() {
    assertThat(DomainMatcher.matches(null, "example.com")).isFalse();
    assertThat(DomainMatcher.matches("example.com", null)).isFalse();
    assertThat(DomainMatcher.matches("", "example.com")).isFalse();
    assertThat(DomainMatcher.matches("example.com", " ")).isFalse();
  }

  @Test
  @DisplayName("matchesAny is true when at least one domain covers the host")
  void matchesAnyCoversTheHost() {
    assertThat(DomainMatcher.matchesAny("sp.example.com", List.of("other.com", "example.com"))).isTrue();
    assertThat(DomainMatcher.matchesAny("sp.example.com", List.of("other.com"))).isFalse();
    assertThat(DomainMatcher.matchesAny("sp.example.com", List.of())).isFalse();
    assertThat(DomainMatcher.matchesAny("sp.example.com", null)).isFalse();
  }

  @Test
  @DisplayName("The host of an entity identifier is extracted lower-cased, without port or path")
  void hostOfExtractsTheHost() {
    assertThat(DomainMatcher.hostOf("https://SP.Example.com:8443/oidf/entity"))
        .isEqualTo(Optional.of("sp.example.com"));
    assertThat(DomainMatcher.hostOf("https://localhost:6890/testOrg1"))
        .isEqualTo(Optional.of("localhost"));
  }

  @Test
  @DisplayName("An identifier with no resolvable host yields no host")
  void hostOfRejectsUnparseableIdentifiers() {
    assertThat(DomainMatcher.hostOf(null)).isEmpty();
    assertThat(DomainMatcher.hostOf("  ")).isEmpty();
    assertThat(DomainMatcher.hostOf("not a uri")).isEmpty();
    assertThat(DomainMatcher.hostOf("/relative/path")).isEmpty();
  }
}
