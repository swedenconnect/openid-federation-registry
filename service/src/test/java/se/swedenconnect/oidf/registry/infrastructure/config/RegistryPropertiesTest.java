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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RegistryPropertiesTest {

  private static final URI TEST_BASE_URL = URI.create("https://registry.example.se/oidf");

  private RegistryProperties.InstanceProperties instance(final String name, final String... functionGroups) {
    return new RegistryProperties.InstanceProperties(UUID.randomUUID(), name, TEST_BASE_URL, null,
        List.of(functionGroups), null);
  }

  private RegistryProperties.FederationAPIProperties federationApiProperties() {
    return new RegistryProperties.FederationAPIProperties(
        "sign-key", "https://issuer.example.se", Duration.ofMinutes(5), List.of(), false, null, null);
  }

  private RegistryProperties propertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(this.federationApiProperties(), List.of(instances), null);
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
  @DisplayName("Duplicate instance names fail validation, since name identifies the tenant")
  void duplicateInstanceNamesFailValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "swedenconnect"),
        this.instance("Swedenconnect", "ena"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("name must be unique")
        .withMessageContaining("Swedenconnect");
  }

  @Test
  @DisplayName("Duplicate function groups across instances fail validation")
  void duplicateFunctionGroupsFailValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "swedenconnect"),
        this.instance("Ena", "swedenconnect"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("function_groups must not be reused")
        .withMessageContaining("swedenconnect");
  }

  @Test
  @DisplayName("A function group reused across two instances' lists fails validation, even when each list "
      + "also has other, distinct entries")
  void duplicateFunctionGroupAcrossFlattenedListsFailsValidation() {
    final RegistryProperties properties = this.propertiesWith(
        this.instance("Swedenconnect", "a", "b"),
        this.instance("Ena", "b"));

    assertThatIllegalArgumentException()
        .isThrownBy(properties::validate)
        .withMessageContaining("function_groups must not be reused")
        .withMessageContaining("b");
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
}