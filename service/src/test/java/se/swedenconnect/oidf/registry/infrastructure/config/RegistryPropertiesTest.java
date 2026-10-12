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
package se.swedenconnect.oidf.registry.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RegistryPropertiesTest {

  private static final URI TEST_BASE_URL = URI.create("https://registry.example.se/oidf");

  private RegistryProperties.InstanceProperties instance(final String name, final String... functionGroups) {
    return new RegistryProperties.InstanceProperties(UUID.randomUUID(), name, TEST_BASE_URL, null,
        List.of(functionGroups), null, null);
  }

  private RegistryProperties.FederationAPIProperties federationApiProperties() {
    return new RegistryProperties.FederationAPIProperties(
        "sign-key", "https://issuer.example.se", Duration.ofMinutes(5), List.of(), false, null, null);
  }

  private RegistryProperties propertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(this.federationApiProperties(), List.of(instances), null, null, null);
  }

  private RegistryProperties propertiesWithSegment(final String segment) {
    return new RegistryProperties(this.federationApiProperties(),
        List.of(this.instance("Swedenconnect", "swedenconnect")), null, null, segment);
  }

  @Test
  @DisplayName("The hosted path segment is hosted when not configured, and can be changed or left out")
  void hostedPathSegment() {
    assertThat(this.propertiesWithSegment(null).effectiveHostedPathSegment()).isEqualTo("hosted");
    assertThat(this.propertiesWithSegment("ec").effectiveHostedPathSegment()).isEqualTo("ec");
    assertThat(this.propertiesWithSegment("").effectiveHostedPathSegment()).isEmpty();
    assertThat(this.propertiesWithSegment("  ").effectiveHostedPathSegment()).isEmpty();
    assertThatCode(this.propertiesWithSegment("ec-1.x")::validate).doesNotThrowAnyException();
    assertThatCode(this.propertiesWithSegment("")::validate).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("A hosted path segment with a slash or other unsafe characters fails validation")
  void invalidHostedPathSegmentFailsValidation() {
    for (final String segment : List.of("a/b", "/hosted", "hosted/", "a b", "..%2f", "..", ".", "å")) {
      assertThatThrownBy(this.propertiesWithSegment(segment)::validate)
          .as(segment)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("hosted-path-segment");
    }
  }

  @Test
  @DisplayName("Distinct instance names and function groups pass validation")
  void distinctNamesAndFunctionGroupsPassValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "swedenconnect"),
        this.instance("Ena", "ena"));

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Duplicate instance names fail validation, since the tenant slug identifies the tenant")
  void duplicateInstanceNamesFailValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "swedenconnect"),
        this.instance("Swedenconnect", "ena"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("unique tenant slug")
        .withMessageContaining("swedenconnect");
  }

  @Test
  @DisplayName("Two names that differ only in case or whitespace collide on the same tenant slug and fail "
      + "validation")
  void namesCollidingOnTheSameSlugFailValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Sweden Connect", "a"),
        this.instance("sweden connect", "b"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("unique tenant slug")
        .withMessageContaining("sweden-connect");
  }

  @Test
  @DisplayName("The same function group may back two different tenants — it carries rights, not routing")
  void sameFunctionGroupOnTwoTenantsPassesValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "swedenconnect"),
        this.instance("Ena", "swedenconnect"));

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("A function group shared between two instances passes validation even when each list also has "
      + "other, distinct entries")
  void sharedFunctionGroupAcrossFlattenedListsPassesValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "a", "b"),
        this.instance("Ena", "b"));

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("A duplicate function group value within one instance's own list fails validation")
  void duplicateFunctionGroupWithinSameInstanceFailsValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "swedenconnect", "swedenconnect"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("must not contain duplicate values within the same instance")
        .withMessageContaining("swedenconnect");
  }

  @Test
  @DisplayName("A tenant may be backed by more than one function group")
  void multipleFunctionGroupsPerTenantPassValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "ena", "sc", "digg"));

    assertThatCode(properties::validate).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("An instance without any function group fails validation")
  void missingFunctionGroupFailsValidation() {
    final RegistryProperties properties = this.propertiesWith(this.instance("Swedenconnect"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("function_groups");
  }

  @Test
  @DisplayName("A blank operator organization fails validation")
  void blankOperatorOrganizationFailsValidation() {
    final RegistryProperties properties = this.propertiesWith(new RegistryProperties.InstanceProperties(
        UUID.randomUUID(), "Swedenconnect", TEST_BASE_URL, null, List.of("swedenconnect"),
        List.of("5520001263", " "), null));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("operator_organizations");
  }
}
