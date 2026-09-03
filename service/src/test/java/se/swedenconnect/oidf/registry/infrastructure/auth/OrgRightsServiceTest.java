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
package se.swedenconnect.oidf.registry.infrastructure.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import se.swedenconnect.iam.commons.types.LocalizedString;
import se.swedenconnect.iam.commons.types.OrganizationID;
import se.swedenconnect.iam.security.claims.OrgRightsClaim;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OrgRightsService}.
 */
@ExtendWith(MockitoExtension.class)
class OrgRightsServiceTest {

  private static final URI TEST_BASE_URL = URI.create("https://registry.example.se/oidf");
  private static final String ORG_A = "5566778899";
  private static final String ORG_44 = "5520009480";
  private static final String ORG_55 = "5520010850";

  @Mock
  private InstanceRepository instanceRepository;

  private RegistryProperties.InstanceProperties instanceProperties(
      final String name, final String... functionGroups) {
    return new RegistryProperties.InstanceProperties(
        UUID.randomUUID(), name, TEST_BASE_URL, null, List.of(functionGroups), null);
  }

  private RegistryProperties registryPropertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  private OrgRightsService serviceWith(final RegistryProperties registryProperties) {
    return new OrgRightsService(
        new InstancePlacementService(registryProperties, this.instanceRepository));
  }

  private RegistryClaims authenticationWith(final OrgRightsClaim orgRights) {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org_rights", List.of())
        .build();
    return new RegistryClaims(jwt, orgRights, "test-subject", List.of());
  }

  private OrgRightsClaim.OrgEntry orgEntry(final String orgNumber, final OrgRightsClaim.FunctionEntry... functions) {
    final LocalizedString name = new LocalizedString();
    name.add("sv", "Org " + orgNumber);
    name.add("en", "Org " + orgNumber);
    return new OrgRightsClaim.OrgEntry(OrganizationID.of(orgNumber), name, null, List.of(functions));
  }

  private static OrgRightsClaim.FunctionEntry functionRight(final String function, final String right) {
    return new OrgRightsClaim.FunctionEntry(function, right);
  }

  @Test
  @DisplayName("canRead resolves the tenant's function group and grants access when the org has a matching right")
  void canReadGrantsAccessWhenFunctionGroupMatches() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(
        false, List.of(this.orgEntry(ORG_A, functionRight("swedenconnect", "read"))));

    final boolean result = service.canRead(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("canWrite denies access when the org's right on the function group is lower than required")
  void canWriteDeniesAccessWhenRightIsInsufficient() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(
        false, List.of(this.orgEntry(ORG_A, functionRight("swedenconnect", "read"))));

    final boolean result = service.canWrite(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("A right on a different function group than the tenant's does not grant access")
  void rightOnUnrelatedFunctionGroupDoesNotGrantAccess() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(
        false, List.of(this.orgEntry(ORG_A, functionRight("other-function-group", "admin"))));

    final boolean result = service.canRead(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("An unknown tenant slug, not backed by any configured instance, denies access")
  void unknownTenantDeniesAccess() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(
        false, List.of(this.orgEntry(ORG_A, functionRight("swedenconnect", "admin"))));

    final boolean result = service.canRead(authenticationWith(orgRights), ORG_A, "unconfigured-tenant");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("canAdmin always grants access for a superuser, regardless of tenant or org")
  void superuserAlwaysGrantsAccess() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(true, List.of());

    final boolean result = service.canAdmin(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("extractOrgRights throws AccessDeniedException for a null authentication")
  void extractOrgRightsThrowsForNullAuthentication() {
    final OrgRightsService service = this.serviceWith(this.registryPropertiesWith());

    assertThatThrownBy(() -> service.extractOrgRights(null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("When a tenant has multiple function groups, the effective right is the max across whichever "
      + "of them the org has rights on")
  void multipleFunctionGroupsOnTheSameTenantGrantTheMaxRight() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(
            this.instanceProperties("swedenconnect-tenant", "swedenconnect-a", "swedenconnect-b")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(false, List.of(this.orgEntry(ORG_A,
        functionRight("swedenconnect-a", "read"),
        functionRight("swedenconnect-b", "write"))));

    final boolean canWrite =
        service.canWrite(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant");
    final boolean canAdmin =
        service.canAdmin(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant");

    assertThat(canWrite).isTrue();
    assertThat(canAdmin).isFalse();
  }

  @Test
  @DisplayName("The same function group may back two tenants: a right on it grants access to both")
  void sharedFunctionGroupGrantsAccessToEveryTenantBackedByIt() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(
            this.instanceProperties("swedenconnect-tenant", "shared", "sc"),
            this.instanceProperties("ena-tenant", "shared", "ena")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(
        false, List.of(this.orgEntry(ORG_A, functionRight("shared", "write"))));

    assertThat(service.canWrite(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant")).isTrue();
    assertThat(service.canWrite(authenticationWith(orgRights), ORG_A, "ena-tenant")).isTrue();
  }

  @Test
  @DisplayName("Rights are still evaluated per tenant: a right held only on one tenant's own function group "
      + "does not leak to the tenant that merely shares another one")
  void rightsRemainScopedPerTenantDespiteASharedFunctionGroup() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(
            this.instanceProperties("swedenconnect-tenant", "shared", "sc"),
            this.instanceProperties("ena-tenant", "shared", "ena")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(
        false, List.of(this.orgEntry(ORG_A, functionRight("sc", "admin"))));

    assertThat(service.canAdmin(authenticationWith(orgRights), ORG_A, "swedenconnect-tenant")).isTrue();
    assertThat(service.canRead(authenticationWith(orgRights), ORG_A, "ena-tenant")).isFalse();
  }

  @Test
  @DisplayName("Tenant swedenconnect (function_groups: ena, sc, digg) grants read when the org has read on sc, "
      + "but a right on an unrelated function group (pm) does not grant access")
  void matchesAgainstAnyConfiguredFunctionGroupOfTheTenant() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect", "ena", "sc", "digg")));
    final OrgRightsClaim orgRights = new OrgRightsClaim(false, List.of(
        this.orgEntry(ORG_44, functionRight("sc", "read")),
        this.orgEntry(ORG_55, functionRight("pm", "admin"))));

    final boolean org44CanRead = service.canRead(authenticationWith(orgRights), ORG_44, "swedenconnect");
    final boolean org55CanRead = service.canRead(authenticationWith(orgRights), ORG_55, "swedenconnect");

    assertThat(org44CanRead).isTrue();
    assertThat(org55CanRead).isFalse();
  }
}
