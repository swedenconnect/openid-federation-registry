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
import se.swedenconnect.oidf.registry.organization.model.Organization;
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

  private Instance instance;

  @BeforeEach
  void setUp() {
    instance = new Instance();
    instance.setInstanceId(instanceId);
  }

  private static final URI TEST_BASE_URL = URI.create("https://registry.example.se/oidf");

  private RegistryProperties.InstanceProperties functionGroupInstance(final UUID id, final String group) {
    return new RegistryProperties.InstanceProperties(id, "Instance " + id, TEST_BASE_URL, null,
        group, null);
  }

  private RegistryProperties propertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  private OrganizationRecord org(final String orgNumber, final String functionGroup) {
    return new OrganizationRecord(orgNumber, "Test Org", "https://example.com/", functionGroup);
  }

  @Test
  @DisplayName("Empty instances list returns empty")
  void emptyInstancesReturnsEmpty() {
    service = new InstancePlacementService(
        new RegistryProperties(null, List.of(), null), instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("5566778899", "digg-admin"));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Function group match returns the corresponding instance")
  void functionGroupMatchReturnsInstance() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

    final Optional<Instance> result = service.resolveInstance(org("9999999999", "digg-admin"));

    assertThat(result).contains(instance);
  }

  @Test
  @DisplayName("Returns empty when function group does not match any instance")
  void noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("0000000000", "unknown-group"));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Repository returning empty propagates as empty optional")
  void repositoryReturningEmptyPropagates() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

    final Optional<Instance> result = service.resolveInstance(org("5566778899", "digg-admin"));

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveAttachedFunctionGroup
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveAttachedFunctionGroup returns the function group of the org's persisted instance")
  void resolveAttachedFunctionGroup_returnsFunctionGroup() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);
    final Organization organization = new Organization();
    organization.setInstance(instance);

    final Optional<String> result = service.resolveAttachedFunctionGroup(organization);

    assertThat(result).contains("digg-admin");
  }

  @Test
  @DisplayName("resolveAttachedFunctionGroup returns empty when no configured instance matches")
  void resolveAttachedFunctionGroup_noMatchReturnsEmpty() {
    service = new InstancePlacementService(propertiesWith(), instanceRepository);
    final Organization organization = new Organization();
    organization.setInstance(instance);

    final Optional<String> result = service.resolveAttachedFunctionGroup(organization);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveFunctionGroupForTenant
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveFunctionGroupForTenant returns the function group of the instance with a matching name")
  void resolveFunctionGroupForTenant_returnsFunctionGroup() {
    service = new InstancePlacementService(
        propertiesWith(new RegistryProperties.InstanceProperties(
            instanceId, "Swedenconnect", TEST_BASE_URL, null, "swedenconnect", null)), instanceRepository);

    final Optional<String> result = service.resolveFunctionGroupForTenant("Swedenconnect");

    assertThat(result).contains("swedenconnect");
  }

  @Test
  @DisplayName("resolveFunctionGroupForTenant returns empty when no instance has that name")
  void resolveFunctionGroupForTenant_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(new RegistryProperties.InstanceProperties(
            instanceId, "Swedenconnect", TEST_BASE_URL, null, "swedenconnect", null)), instanceRepository);

    final Optional<String> result = service.resolveFunctionGroupForTenant("unknown-tenant");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveFunctionGroupForTenant returns empty for a null tenant name")
  void resolveFunctionGroupForTenant_nullTenantReturnsEmpty() {
    service = new InstancePlacementService(propertiesWith(), instanceRepository);

    final Optional<String> result = service.resolveFunctionGroupForTenant(null);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveBaseUrl
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveBaseUrl returns base URL for function group match")
  void resolveBaseUrl_functionGroupMatch() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("9999999999", "digg-admin"));

    assertThat(result).contains(TEST_BASE_URL);
    verify(instanceRepository, never()).findById(any());
  }

  @Test
  @DisplayName("resolveBaseUrl returns empty when no function group matches")
  void resolveBaseUrl_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("0000000000", "unknown-group"));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveBaseUrl returns empty when instance list is empty")
  void resolveBaseUrl_emptyInstancesReturnsEmpty() {
    service = new InstancePlacementService(new RegistryProperties(null, List.of(), null), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("5566778899", "digg-admin"));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveBaseUrl does not access the database")
  void resolveBaseUrl_noDatabaseAccess() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    service.resolveBaseUrl(org("9999999999", "digg-admin"));

    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("resolveBaseUrl by instanceId returns the base URL of the matching instance")
  void resolveBaseUrl_byInstanceId() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(instanceId);

    assertThat(result).contains(TEST_BASE_URL);
  }

  @Test
  @DisplayName("resolveBaseUrl by instanceId returns empty when no instance matches")
  void resolveBaseUrl_byInstanceId_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(functionGroupInstance(instanceId, "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(UUID.randomUUID());

    assertThat(result).isEmpty();
  }
}
