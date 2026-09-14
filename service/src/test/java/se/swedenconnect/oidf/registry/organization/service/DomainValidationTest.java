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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRequestDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the organization-domain validator: a bare hostname, never a URL, a port, a wildcard or an IP
 * address, and {@code localhost} only where local address ranges are enabled.
 */
class DomainValidationTest {

  private static final OrganizationRecord ORG =
      new OrganizationRecord("5520012229", "TestOrg1", "https://registry.example.com/oidf/5520012229",
          "swedenconnect");

  private static void validate(final String domain, final boolean allowLocalhost) {
    ValidateDto.init(ORG).validate(new DomainRequestDto(domain), allowLocalhost);
  }

  @ParameterizedTest
  @ValueSource(strings = {"example.com", "sp.example.com", "a.b.c.example.com", "xn--exmple-cua.com",
      "example-with-hyphen.com", "e1.example.com"})
  @DisplayName("A bare, fully qualified hostname is accepted")
  void acceptsHostnames(final String domain) {
    assertThatCode(() -> validate(domain, false)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("A URL, a path or a port is rejected")
  void rejectsUrlsPathsAndPorts() {
    assertThatThrownBy(() -> validate("https://example.com", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("scheme");
    assertThatThrownBy(() -> validate("example.com/oidf", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("scheme");
    assertThatThrownBy(() -> validate("example.com:8443", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("port");
  }

  @Test
  @DisplayName("A wildcard domain is rejected")
  void rejectsWildcards() {
    assertThatThrownBy(() -> validate("*.example.com", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("Wildcard");
  }

  @ParameterizedTest
  @ValueSource(strings = {"192.168.0.1", "127.0.0.1", "8.8.8.8"})
  @DisplayName("An IP address is not a domain")
  void rejectsIpAddresses(final String domain) {
    assertThatThrownBy(() -> validate(domain, false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("IP address");
  }

  @ParameterizedTest
  @ValueSource(strings = {"-example.com", "example-.com", "exa mple.com", "exam_ple.com", "example..com"})
  @DisplayName("A malformed hostname is rejected")
  void rejectsMalformedHostnames(final String domain) {
    assertThatThrownBy(() -> validate(domain, false))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  @DisplayName("A blank domain is rejected as missing")
  void rejectsBlank() {
    assertThatThrownBy(() -> validate(null, false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("required");
    assertThatThrownBy(() -> validate("   ", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("required");
  }

  @Test
  @DisplayName("A single-label name is rejected, localhost included, unless local ranges are enabled")
  void rejectsSingleLabelNames() {
    assertThatThrownBy(() -> validate("intranet", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("fully qualified");
    assertThatThrownBy(() -> validate("localhost", false))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("fully qualified");
  }

  @Test
  @DisplayName("localhost — and only localhost — is accepted where local address ranges are enabled")
  void acceptsLocalhostWhenLocalRangesAreEnabled() {
    assertThatCode(() -> validate("localhost", true)).doesNotThrowAnyException();
    assertThatThrownBy(() -> validate("intranet", true))
        .isInstanceOf(PropertyValidationFailException.class)
        .hasMessageContaining("fully qualified");
  }

  @Test
  @DisplayName("The stored form is trimmed, lower-cased and free of a trailing root dot")
  void normalizesToStoredForm() {
    assertThat(OrganizationApiService.normalizeDomain("  Example.COM. ")).isEqualTo("example.com");
    assertThat(OrganizationApiService.normalizeDomain(null)).isNull();
  }
}
