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
package se.swedenconnect.oidf.registry.guioperations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantDto;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantOrganizationDto;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantsResponse;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrgRightsService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.FunctionRight;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRightEntry;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.Right;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  @Mock
  private InstanceRepository instanceRepository;

  @Mock
  private OrgRightsService orgRightsService;

  @Mock
  private InstancePlacementService instancePlacementService;

  @Mock
  private Authentication authentication;

  private TenantService service;

  private static final URI TEST_BASE_URL = URI.create("https://registry.example.se/oidf");

  private RegistryProperties.InstanceProperties instanceProperties(
      final UUID id, final String name, final String functionGroup) {
    return new RegistryProperties.InstanceProperties(id, name, TEST_BASE_URL, null,
        functionGroup, null);
  }

  private RegistryProperties registryPropertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  private Instance instanceWithOrgs(final UUID instanceId, final String... orgNumbers) {
    final Instance instance = new Instance();
    instance.setInstanceId(instanceId);
    instance.setOrganizations(Arrays.stream(orgNumbers)
        .map(orgNumber -> {
          final Organization organization = new Organization();
          organization.setOrgNumber(orgNumber);
          organization.setInstance(instance);
          return organization;
        })
        .collect(Collectors.toSet()));
    return instance;
  }

  private Instance instanceWithOrg(final UUID instanceId, final String orgNumber, final String persistedOrgName) {
    final Instance instance = new Instance();
    instance.setInstanceId(instanceId);
    final Organization organization = new Organization();
    organization.setOrgNumber(orgNumber);
    organization.setOrgName(persistedOrgName);
    organization.setInstance(instance);
    instance.setOrganizations(Set.of(organization));
    return instance;
  }

  private OrgRights orgRights(final OrgRightEntry... entries) {
    return new OrgRights(false, List.of(entries));
  }

  private OrgRightEntry orgEntry(final String orgNumber, final FunctionRight... functions) {
    return new OrgRightEntry(orgNumber, "Org " + orgNumber, "Org " + orgNumber, List.of(functions));
  }

  private OrgRightEntry orgEntryWithNames(
      final String orgNumber, final String nameSv, final String nameEn, final FunctionRight... functions) {
    return new OrgRightEntry(orgNumber, nameSv, nameEn, List.of(functions));
  }

  private List<TenantOrganizationDto> organizationsFor(final TenantsResponse response, final String tenant) {
    return response.tenants().stream()
        .filter(t -> t.tenant().equals(tenant))
        .findFirst()
        .map(TenantDto::organizations)
        .orElseThrow(() -> new AssertionError("No tenant '" + tenant + "' in response"));
  }

  // --- Superuser leg: tenants and organizations come exclusively from the database ---

  @Test
  @DisplayName("Superuser gets every configured tenant, with organizations loaded from the database")
  void superuserGetsAllConfiguredTenantsFromDatabase() {
    final UUID instanceIdA = UUID.randomUUID();
    final UUID instanceIdB = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(
            instanceProperties(instanceIdA, "Swedenconnect", "swedenconnect"),
            instanceProperties(instanceIdB, "Ena", "ena")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRights(true, List.of()));
    when(instanceRepository.findAllById(Set.of(instanceIdA, instanceIdB)))
        .thenReturn(List.of(
            instanceWithOrgs(instanceIdA, "4444", "55555"),
            instanceWithOrgs(instanceIdB, "7777")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactlyInAnyOrder("Swedenconnect", "Ena");
    // Superuser leg never reads org_rights, so persisted names (or the org number, absent one) are used as-is.
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto("4444", "4444", null),
            new TenantOrganizationDto("55555", "55555", null));
    assertThat(organizationsFor(result, "Ena")).containsExactly(new TenantOrganizationDto("7777", "7777", null));
  }

  @Test
  @DisplayName("Superuser: tenant with no persisted organizations is included with an empty list")
  void superuserIncludesTenantWithNoOrganizations() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Ena", "ena")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRights(true, List.of()));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId)));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("Ena");
    assertThat(organizationsFor(result, "Ena")).isEmpty();
  }

  @Test
  @DisplayName("Superuser: organization name uses the persisted org name when present")
  void superuserOrganizationNameUsesPersistedName() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRights(true, List.of()));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrg(instanceId, "4444", "Persisted Org Name")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "Persisted Org Name", null));
  }

  @Test
  @DisplayName("Superuser: entityPrefix is resolved per organization via InstancePlacementService")
  void superuserEntityPrefixIsResolvedPerOrganization() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRights(true, List.of()));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444", "55555")));
    when(instancePlacementService.resolveEntityPrefixForPlacedOrg("4444"))
        .thenReturn(Optional.of("https://registry.example.se/oidf/4444"));
    when(instancePlacementService.resolveEntityPrefixForPlacedOrg("55555"))
        .thenReturn(Optional.empty());

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto("4444", "4444", "https://registry.example.se/oidf/4444"),
            new TenantOrganizationDto("55555", "55555", null));
  }

  // --- Regular-user leg: tenants and organizations come exclusively from org_rights (no database access) ---

  @Test
  @DisplayName("Tenant is identified by the instance name backing the userfunktion (function group)")
  void tenantIsIdentifiedByInstanceNameBackingFunctionGroup() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(
            orgEntry("4444", new FunctionRight("swedenconnect", Right.READ)),
            orgEntry("55555", new FunctionRight("swedenconnect", Right.READ))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("Swedenconnect");
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto("4444", "Org 4444", null),
            new TenantOrganizationDto("55555", "Org 55555", null));
    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("Userfunktions (function groups) not backing any configured instance are filtered out")
  void filtersOutUnconfiguredTenants() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry("2021003948", new FunctionRight("stale-group", Right.READ))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).isEmpty();
    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("Organization name is taken from the Swedish org_rights name when present")
  void organizationNameUsesSwedishNameFirst() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("4444", "Svenskt namn", "English name",
            new FunctionRight("swedenconnect", Right.READ))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "Svenskt namn", null));
    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("Organization name falls back to the English org_rights name when the Swedish name is blank")
  void organizationNameFallsBackToEnglishName() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("4444", "", "English name",
            new FunctionRight("swedenconnect", Right.READ))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "English name", null));
  }

  @Test
  @DisplayName("Organization name falls back to the org number when neither org_rights name is present")
  void organizationNameFallsBackToOrgNumber() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("4444", "", "",
            new FunctionRight("swedenconnect", Right.READ))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "4444", null));
  }

  @Test
  @DisplayName("An org_rights entry naming the same organization twice for the same tenant is listed once")
  void sameOrganizationListedOnceWhenMultipleFunctionRightsMatchTheSameTenant() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry("4444",
            new FunctionRight("swedenconnect", Right.READ),
            new FunctionRight("swedenconnect", Right.ADMIN))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "Org 4444", null));
  }

  @Test
  @DisplayName("Organization entityPrefix is resolved via InstancePlacementService's config-only lookup")
  void organizationEntityPrefixIsResolvedPerOrganization() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(
            orgEntry("4444", new FunctionRight("swedenconnect", Right.READ)),
            orgEntry("55555", new FunctionRight("swedenconnect", Right.READ))));
    when(instancePlacementService.resolveEntityPrefix("4444", "swedenconnect"))
        .thenReturn(Optional.of("https://registry.example.se/oidf/4444"));
    when(instancePlacementService.resolveEntityPrefix("55555", "swedenconnect"))
        .thenReturn(Optional.empty());

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto("4444", "Org 4444", "https://registry.example.se/oidf/4444"),
            new TenantOrganizationDto("55555", "Org 55555", null));
  }
}
