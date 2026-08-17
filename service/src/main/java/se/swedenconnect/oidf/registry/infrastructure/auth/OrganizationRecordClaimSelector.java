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

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRightEntry;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient.RegistryOidcUser;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves {@link OrganizationRecord} from the {@code {orgNumber}} and {@code {tenant}} path variables.
 *
 * <p>The {@code {tenant}} path variable is the tenant's configured
 * {@link se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties.InstanceProperties#name()};
 * it is resolved to the tenant's function group via {@link InstancePlacementService#resolveFunctionGroupForTenant}
 * before being used any further. Throws {@link AccessDeniedException} (HTTP 403) if the tenant does not match
 * any configured instance.
 *
 * <p>Verifies that the authenticated user's {@code org_rights} claim contains an entry for the
 * requested {@code orgNumber}. Throws {@link AccessDeniedException} (HTTP 403) if not found.
 * Rights-level validation (READ/WRITE) is handled separately by {@code @PreAuthorize} on each endpoint.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class OrganizationRecordClaimSelector implements HandlerMethodArgumentResolver {

  private final InstancePlacementService instancePlacementService;

  public OrganizationRecordClaimSelector(final InstancePlacementService instancePlacementService) {
    this.instancePlacementService = instancePlacementService;
  }

  @Override
  public boolean supportsParameter(final MethodParameter parameter) {
    return parameter.getParameterType().equals(OrganizationRecord.class);
  }

  @Override
  public Object resolveArgument(
      @Nonnull final MethodParameter parameter,
      final ModelAndViewContainer mavContainer,
      final NativeWebRequest webRequest,
      final WebDataBinderFactory binderFactory) {

    final HttpServletRequest request =
        Objects.requireNonNull(webRequest.getNativeRequest(HttpServletRequest.class));

    @SuppressWarnings("unchecked")
    final Map<String, String> pathVars =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    final String orgNumber = pathVars != null ? pathVars.get("orgNumber") : null;
    final String tenant = pathVars != null ? pathVars.get("tenant") : null;

    if (orgNumber == null) {
      throw new IllegalArgumentException("Path variable 'orgNumber' is required");
    }

    log.debug("Resolving OrganizationRecord for orgNumber='{}' tenant='{}'", orgNumber, tenant);

    final String functionGroup = this.instancePlacementService.resolveFunctionGroupForTenant(tenant)
        .orElseThrow(() -> new AccessDeniedException("Unknown tenant '" + tenant + "'"));

    final OrgRights orgRights = extractOrgRights(SecurityContextHolder.getContext().getAuthentication());

    if (orgRights.superuser()) {
      return buildSuperuserRecord(orgNumber, functionGroup);
    }

    final OrgRightEntry entry = orgRights.findOrg(orgNumber)
        .orElseThrow(() -> new AccessDeniedException(
            "Organization '" + orgNumber + "' not found in token claims"));

    return buildOrganizationRecord(entry, functionGroup);
  }

  private OrgRights extractOrgRights(final Authentication authentication) {
    if (authentication instanceof RegistryClaims registryClaims) {
      return registryClaims.getOrgRights();
    }
    if (authentication instanceof OAuth2AuthenticationToken token
        && token.getPrincipal() instanceof RegistryOidcUser oidcUser) {
      return oidcUser.getOrgRights();
    }
    throw new IllegalArgumentException(
        "Unsupported authentication type: " + authentication.getClass().getSimpleName());
  }

  private OrganizationRecord buildSuperuserRecord(final String orgNumber, final String functionGroup) {
    final String entityPrefix = this.instancePlacementService
        .resolveEntityPrefix(orgNumber, functionGroup)
        .orElse(null);
    return new OrganizationRecord(orgNumber, orgNumber, entityPrefix, functionGroup);
  }

  private OrganizationRecord buildOrganizationRecord(final OrgRightEntry entry, final String functionGroup) {
    final String entityPrefix = this.instancePlacementService
        .resolveEntityPrefix(entry.organizationIdentifier(), functionGroup)
        .orElse(null);
    return new OrganizationRecord(
        entry.organizationIdentifier(),
        entry.organizationNameSv(),
        entityPrefix,
        functionGroup);
  }
}
