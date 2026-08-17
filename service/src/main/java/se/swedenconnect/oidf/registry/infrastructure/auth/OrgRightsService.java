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

  public OrgRightsService(final InstancePlacementService instancePlacementService) {
    this.instancePlacementService = instancePlacementService;
  }

  public boolean canRead(final Authentication authentication, final String orgNumber, final String tenant) {
    return this.hasRight(authentication, orgNumber, tenant, Right.READ);
  }

  public boolean canWrite(final Authentication authentication, final String orgNumber, final String tenant) {
    return this.hasRight(authentication, orgNumber, tenant, Right.WRITE);
  }

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
