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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.FunctionRight;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRightEntry;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.Right;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;
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

  @Mock
  private InstanceRepository instanceRepository;

  @Mock
  private OrganizationRepository organizationRepository;

  private RegistryProperties.InstanceProperties instanceProperties(
      final String name, final String functionGroup) {
    return new RegistryProperties.InstanceProperties(
        UUID.randomUUID(), name, TEST_BASE_URL, null, functionGroup, null);
  }

  private RegistryProperties registryPropertiesWith(final RegistryProperties.InstanceProperties... instances) {
    return new RegistryProperties(null, List.of(instances), null);
  }

  private OrgRightsService serviceWith(final RegistryProperties registryProperties) {
    return new OrgRightsService(
        new InstancePlacementService(registryProperties, this.instanceRepository, this.organizationRepository));
  }

  private RegistryClaims authenticationWith(final OrgRights orgRights) {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org_rights", List.of())
        .build();
    return new RegistryClaims(jwt, orgRights, "test-subject", List.of());
  }

  private OrgRightEntry orgEntry(final String orgNumber, final FunctionRight... functions) {
    return new OrgRightEntry(orgNumber, "Org " + orgNumber, "Org " + orgNumber, List.of(functions));
  }

  @Test
  @DisplayName("canRead resolves the tenant's function group and grants access when the org has a matching right")
  void canReadGrantsAccessWhenFunctionGroupMatches() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRights orgRights = new OrgRights(
        false, List.of(orgEntry("5566778899", new FunctionRight("swedenconnect", Right.READ))));

    final boolean result = service.canRead(authenticationWith(orgRights), "5566778899", "swedenconnect-tenant");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("canWrite denies access when the org's right on the function group is lower than required")
  void canWriteDeniesAccessWhenRightIsInsufficient() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRights orgRights = new OrgRights(
        false, List.of(orgEntry("5566778899", new FunctionRight("swedenconnect", Right.READ))));

    final boolean result = service.canWrite(authenticationWith(orgRights), "5566778899", "swedenconnect-tenant");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("A right on a different function group than the tenant's does not grant access")
  void rightOnUnrelatedFunctionGroupDoesNotGrantAccess() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRights orgRights = new OrgRights(
        false, List.of(orgEntry("5566778899", new FunctionRight("other-function-group", Right.ADMIN))));

    final boolean result = service.canRead(authenticationWith(orgRights), "5566778899", "swedenconnect-tenant");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("An unknown tenant slug, not backed by any configured instance, denies access")
  void unknownTenantDeniesAccess() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRights orgRights = new OrgRights(
        false, List.of(orgEntry("5566778899", new FunctionRight("swedenconnect", Right.ADMIN))));

    final boolean result = service.canRead(authenticationWith(orgRights), "5566778899", "unconfigured-tenant");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("canAdmin always grants access for a superuser, regardless of tenant or org")
  void superuserAlwaysGrantsAccess() {
    final OrgRightsService service = this.serviceWith(
        this.registryPropertiesWith(this.instanceProperties("swedenconnect-tenant", "swedenconnect")));
    final OrgRights orgRights = new OrgRights(true, List.of());

    final boolean result = service.canAdmin(authenticationWith(orgRights), "5566778899", "swedenconnect-tenant");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("extractOrgRights throws AccessDeniedException for a null authentication")
  void extractOrgRightsThrowsForNullAuthentication() {
    final OrgRightsService service = this.serviceWith(this.registryPropertiesWith());

    assertThatThrownBy(() -> service.extractOrgRights(null))
        .isInstanceOf(AccessDeniedException.class);
  }
}