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

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantDto;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantOrganizationDto;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantsResponse;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrgRightsService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.FunctionRight;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRightEntry;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves the tenants (configured instances) the current user has rights on, together with the organizations
 * already registered under each of them.
 *
 * <p>A tenant is identified by {@link RegistryProperties.InstanceProperties#name()} and is backed by exactly one
 * {@link RegistryProperties.InstanceProperties#functionGroup()}. A function right in the {@code org_rights} claim
 * grants access to exactly the tenant whose function group it names.
 *
 * <p>The right-holding organization for a matched function right is also surfaced as one of the tenant's
 * organizations, even if it has no persisted {@link Organization} row under that instance — merged with, and not
 * duplicating, the persisted ones.
 *
 * @author Per Fredrik Plars
 */
@Service
public class TenantService {

  private final RegistryProperties registryProperties;
  private final InstanceRepository instanceRepository;
  private final OrgRightsService orgRightsService;
  private final InstancePlacementService instancePlacementService;

  /**
   * Constructor.
   *
   * @param registryProperties properties holding the tenant (instance) and function-group configuration
   * @param instanceRepository used to load persisted organizations for the matched instances
   * @param orgRightsService used to extract the {@code org_rights} claim from the current authentication
   * @param instancePlacementService used to resolve entity prefixes for the tenant's organizations
   */
  public TenantService(final RegistryProperties registryProperties,
      final InstanceRepository instanceRepository,
      final OrgRightsService orgRightsService,
      final InstancePlacementService instancePlacementService) {
    this.registryProperties = registryProperties;
    this.instanceRepository = instanceRepository;
    this.orgRightsService = orgRightsService;
    this.instancePlacementService = instancePlacementService;
  }

  /**
   * Resolves the tenants the given authentication has rights on, together with the organizations already
   * persisted under each of them.
   *
   * @param authentication the current authentication
   * @return the tenants the caller has rights on, with the organizations registered under each
   */
  public TenantsResponse resolveTenants(final Authentication authentication) {
    final OrgRights orgRights = this.orgRightsService.extractOrgRights(authentication);

    final Set<String> grantedFunctionGroups = orgRights.superuser()
        ? Set.of()
        : this.resolveGrantedFunctionGroups(orgRights);

    final List<RegistryProperties.InstanceProperties> tenants = this.registryProperties.instances().stream()
        .filter(instance -> orgRights.superuser() ||
            grantedFunctionGroups.contains(instance.functionGroup()))
        .toList();

    return this.buildResponse(tenants, orgRights);
  }

  /**
   * Resolves the function groups granted by the given org_rights entries: each function right grants that
   * function group as-is.
   */
  private Set<String> resolveGrantedFunctionGroups(final OrgRights orgRights) {
    final Set<String> result = new HashSet<>();
    for (final OrgRightEntry entry : orgRights.entries()) {
      for (final FunctionRight functionRight : entry.functions()) {
        result.add(functionRight.function());
      }
    }
    return result;
  }

  private TenantsResponse buildResponse(
      final List<RegistryProperties.InstanceProperties> tenants, final OrgRights orgRights) {
    if (tenants.isEmpty()) {
      return new TenantsResponse(List.of());
    }

    final Set<UUID> instanceIds = tenants.stream()
        .map(RegistryProperties.InstanceProperties::instanceId)
        .collect(Collectors.toSet());

    final Map<UUID, List<TenantOrganizationDto>> persistedOrganizationsByInstanceId =
        this.instanceRepository.findAllById(instanceIds).stream()
            .collect(Collectors.toMap(
                Instance::getInstanceId,
                instance -> instance.getOrganizations().stream()
                    .map(organization -> new TenantOrganizationDto(
                        organization.getOrgNumber(),
                        this.resolveOrgName(organization, orgRights),
                        this.resolveEntityPrefix(organization.getOrgNumber())))
                    .toList()));

    final List<TenantDto> tenantDtos = tenants.stream()
        .map(instance -> new TenantDto(instance.name(),
            this.mergeOrganizations(
                persistedOrganizationsByInstanceId.getOrDefault(instance.instanceId(), List.of()),
                this.rightsHolderOrganizationsFor(instance, orgRights))))
        .sorted(Comparator.comparing(TenantDto::tenant))
        .toList();

    return new TenantsResponse(tenantDtos);
  }

  /**
   * Every org_rights entry with a function right matching one of the instance's function groups is itself
   * surfaced as an organization of that tenant, on top of whatever is already persisted — the right holder need
   * not already have a registered {@link Organization} row.
   */
  private List<TenantOrganizationDto> rightsHolderOrganizationsFor(
      final RegistryProperties.InstanceProperties instance, final OrgRights orgRights) {
    return orgRights.entries().stream()
        .filter(entry -> entry.functions().stream()
            .map(FunctionRight::function)
            .anyMatch(function -> function.equals(instance.functionGroup())))
        .map(entry -> new TenantOrganizationDto(
            entry.organizationIdentifier(),
            this.resolveEntryName(entry),
            this.instancePlacementService.resolveEntityPrefix(entry.organizationIdentifier(),
                instance.functionGroup()).orElse(null)))
        .toList();
  }

  /**
   * Resolves the entity prefix for the organization, e.g. {@code https://www.ppm.nu/oidf}.
   *
   * @param orgNumber organization number
   * @return the entity prefix, or {@code null} if the organization is not yet placed on any instance
   */
  private String resolveEntityPrefix(final String orgNumber) {
    return this.instancePlacementService.resolveEntityPrefixForPlacedOrg(orgNumber).orElse(null);
  }

  /**
   * Merges the persisted organizations with the right-holder organizations, keeping the persisted entry (which
   * may carry a better-resolved name) whenever the same organization number appears in both.
   */
  private List<TenantOrganizationDto> mergeOrganizations(
      final List<TenantOrganizationDto> persisted, final List<TenantOrganizationDto> rightsHolders) {
    final Map<String, TenantOrganizationDto> merged = new LinkedHashMap<>();
    persisted.forEach(dto -> merged.put(dto.orgNumber(), dto));
    rightsHolders.forEach(dto -> merged.putIfAbsent(dto.orgNumber(), dto));
    return merged.values().stream()
        .sorted(Comparator.comparing(TenantOrganizationDto::orgNumber))
        .toList();
  }

  /**
   * Resolves the display name for a persisted organization: the {@code org_rights} token claim's Swedish name
   * first, then its English name, then the organization's own persisted name, falling back to the organization
   * number itself if none of those are present (e.g. for superusers, whose token carries no per-organization
   * names).
   */
  private String resolveOrgName(final Organization organization, final OrgRights orgRights) {
    return orgRights.findOrg(organization.getOrgNumber())
        .flatMap(TenantService::nameFromEntry)
        .or(() -> this.nameFromOrganization(organization))
        .orElseGet(organization::getOrgNumber);
  }

  private Optional<String> nameFromOrganization(final Organization organization) {
    final String orgName = organization.getOrgName();
    return (orgName != null && !orgName.isBlank()) ? Optional.of(orgName) : Optional.empty();
  }

  /**
   * Resolves the display name for a right-holder organization that is not (necessarily) persisted: the
   * {@code org_rights} token claim's Swedish name first, then its English name, falling back to the
   * organization number itself.
   */
  private String resolveEntryName(final OrgRightEntry entry) {
    return nameFromEntry(entry).orElseGet(entry::organizationIdentifier);
  }

  private static Optional<String> nameFromEntry(final OrgRightEntry entry) {
    if (entry.organizationNameSv() != null && !entry.organizationNameSv().isBlank()) {
      return Optional.of(entry.organizationNameSv());
    }
    if (entry.organizationNameEn() != null && !entry.organizationNameEn().isBlank()) {
      return Optional.of(entry.organizationNameEn());
    }
    return Optional.empty();
  }
}