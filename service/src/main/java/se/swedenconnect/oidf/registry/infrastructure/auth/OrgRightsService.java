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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.Right;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient.RegistryOidcUser;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

/**
 * Spring Security service for evaluating {@code org_rights} in {@code @PreAuthorize} expressions.
 *
 * <p>Used in controllers as {@code @PreAuthorize("@orgRightsService.canRead(...)")} etc.
 * The org-membership check (throwing 403 if org not in token) is handled earlier by
 * {@link OrganizationRecordClaimSelector}; this service only verifies right level.
 *
 * <p>A right is granted when: the tenant slug from the request path resolves to a configured
 * {@link RegistryProperties.InstanceProperties#functionGroup()}, and the {@code org_rights} claim holds an entry
 * for the given organization number with a function right on that exact function group covering the required
 * level.
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
    return this.hasRight(authentication, orgNumber, tenant, Right.READ);
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
    return this.hasRight(authentication, orgNumber, tenant, Right.WRITE);
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
    return this.hasRight(authentication, orgNumber, tenant, Right.ADMIN);
  }

  private boolean hasRight(
      final Authentication authentication, final String orgNumber, final String tenant, final Right required) {
    return this.instancePlacementService.resolveFunctionGroupForTenant(tenant)
        .map(functionGroup -> this.extractOrgRights(authentication).hasRight(orgNumber, functionGroup, required))
        .orElse(false);
  }

  /**
   * Extracts the parsed {@code org_rights} claim from the given authentication, regardless of whether it
   * originated from a JWT bearer request or an OIDC browser login.
   *
   * @param authentication the current authentication
   * @return the parsed org rights
   * @throws AccessDeniedException if the authentication type is not supported
   */
  public OrgRights extractOrgRights(final Authentication authentication) {
    if(authentication == null) {
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
