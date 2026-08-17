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
          // Deliberately left unset in most tests: the org_rights token name takes priority
          // over the persisted Organization.orgName when both are present.
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

  private String orgName(final String orgNumber) {
    return "Org " + orgNumber;
  }

  private TenantOrganizationDto organizationDto(final String orgNumber) {
    // entityPrefix is null unless the test explicitly stubs instancePlacementService for this orgNumber.
    return new TenantOrganizationDto(orgNumber, orgName(orgNumber), null);
  }

  /**
   * Expected DTO when the org number has no matching org_rights entry (or no usable name in it),
   * so the name falls back to the org number itself.
   */
  private TenantOrganizationDto organizationDtoFallenBackToNumber(final String orgNumber) {
    return new TenantOrganizationDto(orgNumber, orgNumber, null);
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

  @Test
  @DisplayName("Tenant is identified by the instance name, not the function group")
  void tenantIsIdentifiedByInstanceName() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(
            orgEntry("4444", new FunctionRight("swedenconnect", Right.READ)),
            orgEntry("55555", new FunctionRight("swedenconnect", Right.READ))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444", "55555")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("Swedenconnect");
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(organizationDto("4444"), organizationDto("55555"));
  }

  @Test
  @DisplayName("Function groups not backing any instance are filtered out")
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
  @DisplayName("Superuser gets every tenant the registry is configured to support")
  void superuserGetsAllConfiguredTenants() {
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
    // Superuser tokens carry no per-organization names (org_rights is empty), so names fall
    // back to the organization number itself.
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            organizationDtoFallenBackToNumber("4444"), organizationDtoFallenBackToNumber("55555"));
    assertThat(organizationsFor(result, "Ena")).containsExactly(organizationDtoFallenBackToNumber("7777"));
  }

  @Test
  @DisplayName("Tenant with no persisted organizations is included with an empty list")
  void includesTenantWithNoOrganizationsForSuperuser() {
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
  @DisplayName("Organization name is taken from the Swedish org_rights name when present")
  void organizationNameUsesSwedishNameFirst() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("4444", "Svenskt namn", "English name",
            new FunctionRight("swedenconnect", Right.READ))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "Svenskt namn", null));
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
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "English name", null));
  }

  @Test
  @DisplayName("Organization name falls back to the persisted org name when neither token name is present")
  void organizationNameFallsBackToPersistedOrgName() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("4444", "", "",
            new FunctionRight("swedenconnect", Right.READ))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrg(instanceId, "4444", "Persisted Org Name")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("4444", "Persisted Org Name", null));
  }

  @Test
  @DisplayName("Organization name falls back to the persisted org name when there is no matching org_rights "
      + "entry at all, e.g. an org administered by someone else")
  void organizationNameFallsBackToPersistedOrgNameWhenNoMatchingEntry() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry("other-org", new FunctionRight("swedenconnect", Right.READ))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrg(instanceId, "4444", "Persisted Org Name")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto("4444", "Persisted Org Name", null),
            new TenantOrganizationDto("other-org", "Org other-org", null));
  }

  @Test
  @DisplayName("Organization name falls back to the org number when neither token nor persisted name is present")
  void organizationNameFallsBackToOrgNumber() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("4444", "", "",
            new FunctionRight("swedenconnect", Right.READ))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(organizationDtoFallenBackToNumber("4444"));
  }

  @Test
  @DisplayName("An org_rights entry with an explicit function right matching a tenant is itself surfaced as one "
      + "of that tenant's organizations, even without a persisted Organization row")
  void explicitFunctionRightSurfacesRightHolderAsTenantOrganization() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames("2021000837", "Statistikmyndigheten SCB", "Statistics Sweden",
            new FunctionRight("swedenconnect", Right.ADMIN))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId)));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto("2021000837", "Statistikmyndigheten SCB", null));
  }

  @Test
  @DisplayName("A right holder that is also already persisted under the tenant is listed exactly once")
  void rightHolderAlreadyPersistedIsNotDuplicated() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry("4444", new FunctionRight("swedenconnect", Right.ADMIN))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444")));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect")).containsExactly(organizationDto("4444"));
  }

  @Test
  @DisplayName("Organization entityPrefix is resolved via InstancePlacementService, per organization")
  void organizationEntityPrefixIsResolvedPerOrganization() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(
            orgEntry("4444", new FunctionRight("swedenconnect", Right.READ)),
            orgEntry("55555", new FunctionRight("swedenconnect", Right.READ))));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444", "55555")));
    when(instancePlacementService.resolveEntityPrefixForPlacedOrg("4444"))
        .thenReturn(Optional.of("https://registry.example.se/oidf/4444"));
    when(instancePlacementService.resolveEntityPrefixForPlacedOrg("55555"))
        .thenReturn(Optional.empty());

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto("4444", orgName("4444"), "https://registry.example.se/oidf/4444"),
            new TenantOrganizationDto("55555", orgName("55555"), null));
  }
}
