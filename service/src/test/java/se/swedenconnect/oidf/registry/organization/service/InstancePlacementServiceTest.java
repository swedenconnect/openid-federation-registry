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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstancePlacementServiceTest {

  @Mock
  private InstanceRepository instanceRepository;

  private InstancePlacementService service;

  private final UUID instanceId = UUID.randomUUID();
  private final UUID defaultInstanceId = UUID.randomUUID();

  private Instance instance;
  private Instance defaultInstance;

  @BeforeEach
  void setUp() {
    instance = new Instance();
    instance.setInstanceId(instanceId);

    defaultInstance = new Instance();
    defaultInstance.setInstanceId(defaultInstanceId);
  }

  private static final URI TEST_BASE_URL = URI.create("https://registry.example.se/oidf");

  private RegistryProperties.InstanceProperties orgNumberInstance(UUID id, String... orgNumbers) {
    return new RegistryProperties.InstanceProperties(id, "Instance " + id, TEST_BASE_URL, null,
        new RegistryProperties.InstanceMatcherProperties(List.of(), false, List.of(orgNumbers)), null);
  }

  private RegistryProperties.InstanceProperties functionGroupInstance(UUID id, String... groups) {
    return new RegistryProperties.InstanceProperties(id, "Instance " + id, TEST_BASE_URL, null,
        new RegistryProperties.InstanceMatcherProperties(List.of(groups), false, List.of()), null);
  }

  private RegistryProperties.InstanceProperties defaultInstance(UUID id) {
    return new RegistryProperties.InstanceProperties(id, "Default instance", TEST_BASE_URL, null,
        new RegistryProperties.InstanceMatcherProperties(List.of(), true, List.of()), null);
  }

  private RegistryProperties propertiesWith(RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  private OrganizationRecord org(String orgNumber, String functionGroup) {
    return new OrganizationRecord(orgNumber, "Test Org", "https://example.com/", functionGroup);
  }

  @Test
  @DisplayName("Empty instances list returns empty")
  void emptyInstancesReturnsEmpty() {
    service = new InstancePlacementService(new RegistryProperties(null, List.of(), null), instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("5566778899", null));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Org number match returns the corresponding instance")
  void orgNumberMatchReturnsInstance() {
    service = new InstancePlacementService(
        propertiesWith(orgNumberInstance(instanceId, "5566778899", "1122334455")),
        instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

    final Optional<Instance> result = service.resolveInstance(org("5566778899", null));

    assertThat(result).contains(instance);
  }

  @Test
  @DisplayName("Function group match returns the corresponding instance when org number does not match")
  void functionGroupMatchReturnsInstance() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin", "digg-operator")),
        instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

    final Optional<Instance> result = service.resolveInstance(org("9999999999", "digg-admin"));

    assertThat(result).contains(instance);
  }

  @Test
  @DisplayName("Org number match takes precedence over function group on the same instance config")
  void orgNumberMatchTakesPrecedenceOverFunctionGroup() {
    final UUID secondId = UUID.randomUUID();
    final Instance secondInstance = new Instance();
    secondInstance.setInstanceId(secondId);

    service = new InstancePlacementService(
        propertiesWith(
            orgNumberInstance(instanceId, "5566778899"),
            functionGroupInstance(secondId, "digg-admin")),
        instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

    // Org matches first instance — function group also matches second, but first wins
    final Optional<Instance> result = service.resolveInstance(org("5566778899", "digg-admin"));

    assertThat(result).contains(instance);
    verify(instanceRepository, never()).findById(secondId);
  }

  @Test
  @DisplayName("Falls back to default instance when no org number or function group matches")
  void fallsBackToDefaultInstance() {
    service = new InstancePlacementService(
        propertiesWith(
            orgNumberInstance(instanceId, "5566778899"),
            defaultInstance(defaultInstanceId)),
        instanceRepository);
    when(instanceRepository.findById(defaultInstanceId)).thenReturn(Optional.of(defaultInstance));

    final Optional<Instance> result = service.resolveInstance(org("0000000000", null));

    assertThat(result).contains(defaultInstance);
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Returns empty when no matcher matches and no default is configured")
  void noMatchAndNoDefaultReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(orgNumberInstance(instanceId, "5566778899")),
        instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("0000000000", null));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Repository returning empty propagates as empty optional")
  void repositoryReturningEmptyPropagates() {
    service = new InstancePlacementService(
        propertiesWith(orgNumberInstance(instanceId, "5566778899")),
        instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

    final Optional<Instance> result = service.resolveInstance(org("5566778899", null));

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveBaseUrl
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveBaseUrl returns base URL for org number match")
  void resolveBaseUrl_orgNumberMatch() {
    service = new InstancePlacementService(
        propertiesWith(orgNumberInstance(instanceId, "5566778899")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("5566778899", null));

    assertThat(result).contains(TEST_BASE_URL);
    verify(instanceRepository, never()).findById(any());
  }

  @Test
  @DisplayName("resolveBaseUrl returns base URL via function group fallback")
  void resolveBaseUrl_functionGroupMatch() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("9999999999", "digg-admin"));

    assertThat(result).contains(TEST_BASE_URL);
    verify(instanceRepository, never()).findById(any());
  }

  @Test
  @DisplayName("resolveBaseUrl returns default instance base URL when no explicit match")
  void resolveBaseUrl_fallsBackToDefault() {
    final URI defaultUrl = URI.create("https://default.example.se/oidf");
    final RegistryProperties.InstanceProperties defaultProp =
        new RegistryProperties.InstanceProperties(defaultInstanceId, "Default", defaultUrl, null,
            new RegistryProperties.InstanceMatcherProperties(List.of(), true, List.of()), null);
    service = new InstancePlacementService(propertiesWith(defaultProp), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("0000000000", null));

    assertThat(result).contains(defaultUrl);
  }

  @Test
  @DisplayName("resolveBaseUrl returns empty when no instance matches and no default configured")
  void resolveBaseUrl_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(orgNumberInstance(instanceId, "5566778899")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("0000000000", null));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveBaseUrl returns empty when instance list is empty")
  void resolveBaseUrl_emptyInstancesReturnsEmpty() {
    service = new InstancePlacementService(new RegistryProperties(null, List.of(), null), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("5566778899", null));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveBaseUrl does not access the database")
  void resolveBaseUrl_noDatabaseAccess() {
    service = new InstancePlacementService(
        propertiesWith(orgNumberInstance(instanceId, "5566778899")), instanceRepository);

    service.resolveBaseUrl(org("5566778899", null));

    verifyNoInteractions(instanceRepository);
  }
}