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

  private RegistryProperties.InstanceProperties tenant(
      final UUID id, final String name, final String... functionGroups) {
    return new RegistryProperties.InstanceProperties(id, name, TEST_BASE_URL, null,
        List.of(functionGroups), null);
  }

  private RegistryProperties propertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  private OrganizationRecord org(final String orgNumber, final String tenant) {
    return new OrganizationRecord(orgNumber, "Test Org", "https://example.com/", tenant);
  }

  @Test
  @DisplayName("Empty instances list returns empty")
  void emptyInstancesReturnsEmpty() {
    service = new InstancePlacementService(
        new RegistryProperties(null, List.of(), null), instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("5566778899", "digg"));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Tenant match returns the corresponding instance")
  void tenantMatchReturnsInstance() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

    final Optional<Instance> result = service.resolveInstance(org("9999999999", "digg"));

    assertThat(result).contains(instance);
  }

  @Test
  @DisplayName("Returns empty when the tenant does not match any instance")
  void noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("0000000000", "unknown-tenant"));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("A function group name is not a tenant name: it does not resolve an instance")
  void functionGroupIsNotATenantKey() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    final Optional<Instance> result = service.resolveInstance(org("9999999999", "digg-admin"));

    assertThat(result).isEmpty();
    verify(instanceRepository, never()).findById(instanceId);
  }

  @Test
  @DisplayName("Two tenants sharing a function group each resolve to their own instance, keyed on the tenant")
  void sharedFunctionGroupResolvesPerTenant() {
    final UUID otherInstanceId = UUID.randomUUID();
    final Instance otherInstance = new Instance();
    otherInstance.setInstanceId(otherInstanceId);
    service = new InstancePlacementService(
        propertiesWith(
            tenant(instanceId, "Swedenconnect", "shared"),
            tenant(otherInstanceId, "Ena", "shared")),
        instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
    when(instanceRepository.findById(otherInstanceId)).thenReturn(Optional.of(otherInstance));

    assertThat(service.resolveInstance(org("4444", "swedenconnect"))).contains(instance);
    assertThat(service.resolveInstance(org("4444", "ena"))).contains(otherInstance);
  }

  @Test
  @DisplayName("Repository returning empty propagates as empty optional")
  void repositoryReturningEmptyPropagates() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);
    when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

    final Optional<Instance> result = service.resolveInstance(org("5566778899", "digg"));

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveTenantForPlacedOrg
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveTenantForPlacedOrg returns the tenant slug of the org's persisted instance")
  void resolveTenantForPlacedOrg_returnsTenantSlug() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Sweden Connect", "digg-admin")), instanceRepository);
    final Organization organization = new Organization();
    organization.setInstance(instance);

    final Optional<String> result = service.resolveTenantForPlacedOrg(organization);

    assertThat(result).contains("sweden-connect");
  }

  @Test
  @DisplayName("resolveTenantForPlacedOrg returns empty when no configured instance matches")
  void resolveTenantForPlacedOrg_noMatchReturnsEmpty() {
    service = new InstancePlacementService(propertiesWith(), instanceRepository);
    final Organization organization = new Organization();
    organization.setInstance(instance);

    final Optional<String> result = service.resolveTenantForPlacedOrg(organization);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveEntityPrefixForPlacedOrg
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveEntityPrefixForPlacedOrg builds the prefix from the instance the org is placed on")
  void resolveEntityPrefixForPlacedOrg_buildsPrefix() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Swedenconnect", "swedenconnect")), instanceRepository);
    final Organization organization = new Organization();
    organization.setInstance(instance);
    organization.setOrgNumber("4444");

    assertThat(service.resolveEntityPrefixForPlacedOrg(organization))
        .contains("https://registry.example.se/oidf/4444");
  }

  // -------------------------------------------------------------------------
  // resolveTenant
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveTenant canonicalises the configured name to the tenant slug")
  void resolveTenant_returnsSlug() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Swedenconnect", "swedenconnect")), instanceRepository);

    assertThat(service.resolveTenant("Swedenconnect")).contains("swedenconnect");
  }

  @Test
  @DisplayName("resolveTenant matches on the tenant slug: lowercased, spaces replaced by hyphens")
  void resolveTenant_matchesOnSlug() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Sweden Connect", "swedenconnect")), instanceRepository);

    assertThat(service.resolveTenant("sweden-connect")).contains("sweden-connect");
    assertThat(service.resolveTenant("Sweden Connect")).contains("sweden-connect");
    assertThat(service.resolveTenant("swedenconnect")).isEmpty();
  }

  @Test
  @DisplayName("resolveTenant returns empty when no instance has that name")
  void resolveTenant_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Swedenconnect", "swedenconnect")), instanceRepository);

    assertThat(service.resolveTenant("unknown-tenant")).isEmpty();
  }

  @Test
  @DisplayName("resolveTenant returns empty for a null tenant name")
  void resolveTenant_nullTenantReturnsEmpty() {
    service = new InstancePlacementService(propertiesWith(), instanceRepository);

    assertThat(service.resolveTenant(null)).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveFunctionGroupsForTenant
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveFunctionGroupsForTenant returns every function group of the instance with a matching name")
  void resolveFunctionGroupsForTenant_returnsAllFunctionGroups() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Swedenconnect", "ena", "sc", "digg")), instanceRepository);

    final Optional<List<String>> result = service.resolveFunctionGroupsForTenant("Swedenconnect");

    assertThat(result).contains(List.of("ena", "sc", "digg"));
  }

  @Test
  @DisplayName("Two tenants sharing a function group each report it as their own")
  void resolveFunctionGroupsForTenant_sharedFunctionGroup() {
    service = new InstancePlacementService(
        propertiesWith(
            tenant(instanceId, "Swedenconnect", "shared", "sc"),
            tenant(UUID.randomUUID(), "Ena", "shared", "ena")),
        instanceRepository);

    assertThat(service.resolveFunctionGroupsForTenant("swedenconnect")).contains(List.of("shared", "sc"));
    assertThat(service.resolveFunctionGroupsForTenant("ena")).contains(List.of("shared", "ena"));
  }

  @Test
  @DisplayName("resolveFunctionGroupsForTenant returns empty when no instance has that name")
  void resolveFunctionGroupsForTenant_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Swedenconnect", "swedenconnect")), instanceRepository);

    final Optional<List<String>> result = service.resolveFunctionGroupsForTenant("unknown-tenant");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveFunctionGroupsForTenant returns empty for a null tenant name")
  void resolveFunctionGroupsForTenant_nullTenantReturnsEmpty() {
    service = new InstancePlacementService(propertiesWith(), instanceRepository);

    final Optional<List<String>> result = service.resolveFunctionGroupsForTenant(null);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // resolveBaseUrl
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("resolveBaseUrl returns base URL for a tenant match")
  void resolveBaseUrl_tenantMatch() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("9999999999", "digg"));

    assertThat(result).contains(TEST_BASE_URL);
    verify(instanceRepository, never()).findById(any());
  }

  @Test
  @DisplayName("resolveBaseUrl returns empty when no tenant matches")
  void resolveBaseUrl_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("0000000000", "unknown-tenant"));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveBaseUrl returns empty when instance list is empty")
  void resolveBaseUrl_emptyInstancesReturnsEmpty() {
    service = new InstancePlacementService(new RegistryProperties(null, List.of(), null), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(org("5566778899", "digg"));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveBaseUrl does not access the database")
  void resolveBaseUrl_noDatabaseAccess() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    service.resolveBaseUrl(org("9999999999", "digg"));

    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("resolveBaseUrl by instanceId returns the base URL of the matching instance")
  void resolveBaseUrl_byInstanceId() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(instanceId);

    assertThat(result).contains(TEST_BASE_URL);
  }

  @Test
  @DisplayName("resolveBaseUrl by instanceId returns empty when no instance matches")
  void resolveBaseUrl_byInstanceId_noMatchReturnsEmpty() {
    service = new InstancePlacementService(
        propertiesWith(tenant(instanceId, "Digg", "digg-admin")), instanceRepository);

    final Optional<URI> result = service.resolveBaseUrl(UUID.randomUUID());

    assertThat(result).isEmpty();
  }
}
