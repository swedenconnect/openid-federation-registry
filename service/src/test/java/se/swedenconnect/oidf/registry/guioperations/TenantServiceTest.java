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
import se.swedenconnect.iam.commons.types.LocalizedString;
import se.swedenconnect.iam.commons.types.OrganizationID;
import se.swedenconnect.iam.security.claims.OrgRightsClaim;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantDto;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantOrganizationDto;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantsResponse;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrgRightsService;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  // Valid (Luhn-checksum) Swedish organization numbers used wherever a value flows through the org_rights claim
  // (i.e. is built via orgEntry/orgEntryWithNames below) — se.swedenconnect.iam.commons.types.OrganizationID
  // validates this format and OrgRightsClaimParser silently drops entries that don't match it.
  private static final String ORG_4444 = "5520006742";
  private static final String ORG_55555 = "5520001263";
  private static final String ORG_44 = "5520009480";
  private static final String ORG_55 = "5520010850";

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
      final UUID id, final String name, final String... functionGroups) {
    return new RegistryProperties.InstanceProperties(id, name, TEST_BASE_URL, null,
        List.of(functionGroups), null);
  }

  private RegistryProperties registryPropertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  // Superuser-leg fixtures: these organization numbers are persisted DB data (Organization.orgNumber), never
  // parsed through org_rights/OrganizationID, so they don't need to be Luhn-valid.

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

  // Regular-user-leg fixtures: these build the org_rights claim, so the organization number must be a valid
  // Swedish organization number (OrganizationID.of(...) throws/parses-out anything else).

  private OrgRightsClaim orgRights(final OrgRightsClaim.OrgEntry... entries) {
    return new OrgRightsClaim(false, List.of(entries));
  }

  private OrgRightsClaim.OrgEntry orgEntry(final String orgNumber, final OrgRightsClaim.FunctionEntry... functions) {
    return this.orgEntryWithNames(orgNumber, "Org " + orgNumber, "Org " + orgNumber, functions);
  }

  private OrgRightsClaim.OrgEntry orgEntryWithNames(
      final String orgNumber, final String nameSv, final String nameEn,
      final OrgRightsClaim.FunctionEntry... functions) {
    final LocalizedString name = new LocalizedString();
    name.add("sv", nameSv);
    name.add("en", nameEn);
    return new OrgRightsClaim.OrgEntry(OrganizationID.of(orgNumber), name, null, List.of(functions));
  }

  private static OrgRightsClaim.FunctionEntry functionRight(final String function, final String right) {
    return new OrgRightsClaim.FunctionEntry(function, right);
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

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRightsClaim(true, List.of()));
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

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRightsClaim(true, List.of()));
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

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRightsClaim(true, List.of()));
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

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(new OrgRightsClaim(true, List.of()));
    when(instanceRepository.findAllById(Set.of(instanceId)))
        .thenReturn(List.of(instanceWithOrgs(instanceId, "4444", "55555")));
    when(instancePlacementService.resolveEntityPrefixForPlacedOrg(
        argThat(org -> org != null && "4444".equals(org.getOrgNumber()))))
        .thenReturn(Optional.of("https://registry.example.se/oidf/4444"));
    when(instancePlacementService.resolveEntityPrefixForPlacedOrg(
        argThat(org -> org != null && "55555".equals(org.getOrgNumber()))))
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
            orgEntry(ORG_4444, functionRight("swedenconnect", "read")),
            orgEntry(ORG_55555, functionRight("swedenconnect", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("Swedenconnect");
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, null),
            new TenantOrganizationDto(ORG_55555, "Org " + ORG_55555, null));
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
        orgRights(orgEntry("2021003948", functionRight("stale-group", "read"))));

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
        orgRights(orgEntryWithNames(ORG_4444, "Svenskt namn", "English name",
            functionRight("swedenconnect", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, "Svenskt namn", null));
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
        orgRights(orgEntryWithNames(ORG_4444, "", "English name",
            functionRight("swedenconnect", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, "English name", null));
  }

  @Test
  @DisplayName("Organization name falls back to the org number when neither org_rights name is present")
  void organizationNameFallsBackToOrgNumber() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntryWithNames(ORG_4444, "", "",
            functionRight("swedenconnect", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, ORG_4444, null));
  }

  @Test
  @DisplayName("An org_rights entry naming the same organization twice for the same tenant is listed once")
  void sameOrganizationListedOnceWhenMultipleFunctionRightsMatchTheSameTenant() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry(ORG_4444,
            functionRight("swedenconnect", "read"),
            functionRight("swedenconnect", "admin"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, null));
  }

  @Test
  @DisplayName("Two function-group hits belonging to the same tenant are merged into one tenant bucket, "
      + "not duplicated")
  void multipleFunctionGroupsOfTheSameTenantMergeIntoOneTenantBucket() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "Swedenconnect", "swedenconnect-a", "swedenconnect-b")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(
            orgEntry(ORG_4444, functionRight("swedenconnect-a", "read")),
            orgEntry(ORG_55555, functionRight("swedenconnect-b", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("Swedenconnect");
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, null),
            new TenantOrganizationDto(ORG_55555, "Org " + ORG_55555, null));
    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("Tenant swedenconnect (function_groups: ena, sc, digg): org with a right on sc is included, "
      + "org with a right only on an unrelated group (pm) is not")
  void tenantWithMultipleFunctionGroupsOnlyIncludesOrgsMatchingOneOfThem() {
    final UUID instanceId = UUID.randomUUID();
    service = new TenantService(
        registryPropertiesWith(instanceProperties(instanceId, "swedenconnect", "ena", "sc", "digg")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(
            orgEntry(ORG_44, functionRight("sc", "read")),
            orgEntry(ORG_55, functionRight("pm", "admin"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("swedenconnect");
    assertThat(organizationsFor(result, "swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_44, "Org " + ORG_44, null));
    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("A function group backing two tenants surfaces the organization under both of them")
  void sharedFunctionGroupListsTheOrganizationUnderEveryTenantBackedByIt() {
    service = new TenantService(
        registryPropertiesWith(
            instanceProperties(UUID.randomUUID(), "Swedenconnect", "shared", "sc"),
            instanceProperties(UUID.randomUUID(), "Ena", "shared", "ena")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry(ORG_4444, functionRight("shared", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactlyInAnyOrder("Swedenconnect", "Ena");
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, null));
    assertThat(organizationsFor(result, "Ena"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, null));
    verifyNoInteractions(instanceRepository);
  }

  @Test
  @DisplayName("With a shared function group, an org holding a right only on one tenant's own group is listed "
      + "under that tenant alone")
  void rightOnATenantsOwnFunctionGroupDoesNotLeakToTheTenantSharingAnother() {
    service = new TenantService(
        registryPropertiesWith(
            instanceProperties(UUID.randomUUID(), "Swedenconnect", "shared", "sc"),
            instanceProperties(UUID.randomUUID(), "Ena", "shared", "ena")),
        instanceRepository, orgRightsService, instancePlacementService);

    when(orgRightsService.extractOrgRights(authentication)).thenReturn(
        orgRights(orgEntry(ORG_4444, functionRight("sc", "read"))));

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(result.tenants()).extracting(TenantDto::tenant).containsExactly("Swedenconnect");
    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactly(new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, null));
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
            orgEntry(ORG_4444, functionRight("swedenconnect", "read")),
            orgEntry(ORG_55555, functionRight("swedenconnect", "read"))));
    when(instancePlacementService.resolveEntityPrefix(ORG_4444, "swedenconnect"))
        .thenReturn(Optional.of("https://registry.example.se/oidf/4444"));
    when(instancePlacementService.resolveEntityPrefix(ORG_55555, "swedenconnect"))
        .thenReturn(Optional.empty());

    final TenantsResponse result = service.resolveTenants(authentication);

    assertThat(organizationsFor(result, "Swedenconnect"))
        .containsExactlyInAnyOrder(
            new TenantOrganizationDto(ORG_4444, "Org " + ORG_4444, "https://registry.example.se/oidf/4444"),
            new TenantOrganizationDto(ORG_55555, "Org " + ORG_55555, null));
  }
}
