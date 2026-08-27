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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import se.swedenconnect.iam.security.claims.OrgRightsClaim;
import se.swedenconnect.iam.security.claims.OrganizationRight;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient.RegistryOidcUser;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Spring Security service for evaluating {@code org_rights} in {@code @PreAuthorize} expressions.
 *
 * <p>Used in controllers as {@code @PreAuthorize("@orgRightsService.canRead(...)")} etc.
 * The org-membership check (throwing 403 if org not in token) is handled earlier by
 * {@link OrganizationRecordClaimSelector}; this service only verifies right level.
 *
 * <p>A right is granted when: the tenant slug from the request path resolves to a configured
 * {@link RegistryProperties.InstanceProperties#functionGroups()}, and the {@code org_rights} claim holds an entry
 * for the given organization number with a function right on any of the tenant's configured function groups
 * covering the required level.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OrgRightsService {

  private final InstancePlacementService instancePlacementService;

  /**
   * Creates a new service.
   *
   * @param instancePlacementService service used to resolve tenants to function groups
   */
  public OrgRightsService(final InstancePlacementService instancePlacementService) {
    this.instancePlacementService = instancePlacementService;
  }

  /**
   * Checks whether the authentication has read access to the given organization under the given tenant.
   *
   * @param authentication the current authentication
   * @param orgNumber the organization number
   * @param tenant the tenant identifier
   * @return true if read access is granted
   */
  public boolean canRead(final Authentication authentication, final String orgNumber, final String tenant) {
    return this.hasRight(authentication, orgNumber, tenant, OrganizationRight.READ);
  }

  /**
   * Checks whether the authentication has write access to the given organization under the given tenant.
   *
   * @param authentication the current authentication
   * @param orgNumber the organization number
   * @param tenant the tenant identifier
   * @return true if write access is granted
   */
  public boolean canWrite(final Authentication authentication, final String orgNumber, final String tenant) {
    return this.hasRight(authentication, orgNumber, tenant, OrganizationRight.WRITE);
  }

  /**
   * Checks whether the authentication has admin access to the given organization under the given tenant.
   *
   * @param authentication the current authentication
   * @param orgNumber the organization number
   * @param tenant the tenant identifier
   * @return true if admin access is granted
   */
  public boolean canAdmin(final Authentication authentication, final String orgNumber, final String tenant) {
    return this.hasRight(authentication, orgNumber, tenant, OrganizationRight.ADMIN);
  }

  private boolean hasRight(
      final Authentication authentication, final String orgNumber, final String tenant,
      final OrganizationRight required) {
    return this.instancePlacementService.resolveFunctionGroupsForTenant(tenant)
        .map(functionGroups -> hasRight(this.extractOrgRights(authentication), orgNumber, functionGroups, required))
        .orElse(false);
  }

  /**
   * Returns true if the claim grants at least the required right for the given organization on any of the given
   * function groups. Superusers always return true.
   *
   * @param claim the parsed {@code org_rights} claim
   * @param orgNumber organization number from the request path
   * @param functionGroups the function groups backing the tenant from the request path
   * @param required minimum required right level
   * @return true if access is granted
   */
  public static boolean hasRight(
      final OrgRightsClaim claim, final String orgNumber, final Collection<String> functionGroups,
      final OrganizationRight required) {
    if (claim.superuser()) {
      return true;
    }
    return findOrgEntry(claim, orgNumber)
        .flatMap(entry -> effectiveRight(entry, functionGroups))
        .map(right -> right.compareTo(required) >= 0)
        .orElse(false);
  }

  /**
   * Finds the organization entry for the given organization number.
   *
   * @param claim the parsed {@code org_rights} claim
   * @param orgNumber the organization number to look up
   * @return the matching entry, or empty if not present
   */
  public static Optional<OrgRightsClaim.OrgEntry> findOrgEntry(final OrgRightsClaim claim, final String orgNumber) {
    return claim.orgEntries().stream()
        .filter(entry -> entry.orgIdentifier().getId().equals(orgNumber))
        .findFirst();
  }

  /**
   * Computes the effective right for a set of function groups by taking the highest right among all function entries
   * whose function name is a member of the given collection. Entries carrying an unrecognized right value are skipped.
   *
   * @param entry the organization entry to evaluate
   * @param functionGroups the function groups to evaluate
   * @return the effective right, or empty if no matching entry exists
   */
  public static Optional<OrganizationRight> effectiveRight(
      final OrgRightsClaim.OrgEntry entry, final Collection<String> functionGroups) {
    return entry.functions().stream()
        .filter(f -> functionGroups.contains(f.function()))
        .map(f -> parseRight(f.right()))
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder());
  }

  private static OrganizationRight parseRight(final String right) {
    try {
      return OrganizationRight.parse(right);
    }
    catch (final IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Extracts the parsed {@code org_rights} claim from the given authentication, regardless of whether it
   * originated from a JWT bearer request or an OIDC browser login.
   *
   * @param authentication the current authentication
   * @return the parsed org rights
   * @throws AccessDeniedException if the authentication type is not supported
   */
  public OrgRightsClaim extractOrgRights(final Authentication authentication) {
    if (authentication == null) {
      throw new AccessDeniedException("There is no authentication object given");
    }
    if (authentication instanceof final RegistryClaims registryClaims) {
      return registryClaims.getOrgRights();
    }
    if (authentication instanceof final OAuth2AuthenticationToken token
        && token.getPrincipal() instanceof final RegistryOidcUser oidcUser) {
      return oidcUser.getOrgRights();
    }
    throw new AccessDeniedException(
        "Unsupported authentication type: " + authentication.getClass().getSimpleName());
  }
}
