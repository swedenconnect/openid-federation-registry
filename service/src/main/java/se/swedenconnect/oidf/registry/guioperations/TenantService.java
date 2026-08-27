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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the tenants (configured instances) the current user has rights on, together with the organizations
 * registered under each of them.
 *
 * <p>A tenant is identified by {@link RegistryProperties.InstanceProperties#name()} and may be backed by one or
 * more {@link RegistryProperties.InstanceProperties#functionGroups()}.
 *
 * <p>Resolution takes one of two independent paths, depending on the caller:
 * <ul>
 *   <li><b>Superuser</b> — every configured tenant is returned, with its organizations read straight from the
 *   database ({@link InstanceRepository}).
 *   <li><b>Regular user</b> — only the {@code org_rights} claim is consulted. Each function right names a
 *   "userfunktion" (function group), which is looked up against the configured instances to find the tenant it
 *   grants access to; the tenant's organizations are the right-holding entries themselves, exactly as carried in
 *   the token. The database is not read on this path.
 * </ul>
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
   * @param instanceRepository used to load persisted organizations for a superuser's tenants
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
   * Resolves the tenants the given authentication has rights on, together with their organizations.
   *
   * @param authentication the current authentication
   * @return the tenants the caller has rights on, with the organizations registered under each
   */
  public TenantsResponse resolveTenants(final Authentication authentication) {
    final OrgRightsClaim orgRights = this.orgRightsService.extractOrgRights(authentication);
    return orgRights.superuser() ? this.resolveForSuperuser() : this.resolveFromOrgRights(orgRights);
  }

  /**
   * Superuser leg: every configured tenant, with its organizations loaded from the database.
   */
  private TenantsResponse resolveForSuperuser() {
    final List<RegistryProperties.InstanceProperties> tenants = this.registryProperties.instances();

    final Map<UUID, Instance> instancesById = this.instanceRepository.findAllById(
            tenants.stream().map(RegistryProperties.InstanceProperties::instanceId).collect(Collectors.toSet()))
        .stream()
        .collect(Collectors.toMap(Instance::getInstanceId, Function.identity()));

    final List<TenantDto> tenantDtos = tenants.stream()
        .map(instance -> new TenantDto(
            instance.name(), this.persistedOrganizations(instancesById.get(instance.instanceId()))))
        .sorted(Comparator.comparing(TenantDto::tenant))
        .toList();

    return new TenantsResponse(tenantDtos);
  }

  private List<TenantOrganizationDto> persistedOrganizations(final Instance instance) {
    if (instance == null) {
      return List.of();
    }
    return instance.getOrganizations().stream()
        .map(organization -> new TenantOrganizationDto(
            organization.getOrgNumber(),
            this.nameFromOrganization(organization).orElseGet(organization::getOrgNumber),
            this.instancePlacementService.resolveEntityPrefixForPlacedOrg(organization)
                .orElse(null)))
        .sorted(Comparator.comparing(TenantOrganizationDto::orgNumber))
        .toList();
  }

  /**
   * Regular-user leg: no database access. Each {@code org_rights} function right names a userfunktion (function group),
   * looked up against the configured instances to find the tenant it grants access to; the right-holding entry is
   * itself surfaced as one of that tenant's organizations.
   */
  private TenantsResponse resolveFromOrgRights(final OrgRightsClaim orgRights) {
    final Map<String, RegistryProperties.InstanceProperties> instanceByFunctionGroup =
        this.registryProperties.instances().stream()
            .flatMap(instance -> instance.functionGroups().stream().map(fg -> Map.entry(fg, instance)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    final Map<String, Map<String, TenantOrganizationDto>> organizationsByTenant = new LinkedHashMap<>();
    for (final OrgRightsClaim.OrgEntry entry : orgRights.orgEntries()) {
      final String orgNumber = entry.orgIdentifier().getId();
      for (final OrgRightsClaim.FunctionEntry functionRight : entry.functions()) {
        final RegistryProperties.InstanceProperties instance = instanceByFunctionGroup.get(functionRight.function());
        if (instance == null) {
          continue;
        }
        organizationsByTenant.computeIfAbsent(instance.name(), key -> new LinkedHashMap<>())
            .putIfAbsent(orgNumber, this.organizationFromEntry(entry, orgNumber, instance));
      }
    }

    final List<TenantDto> tenantDtos = organizationsByTenant.entrySet().stream()
        .map(entry -> new TenantDto(entry.getKey(), entry.getValue().values().stream()
            .sorted(Comparator.comparing(TenantOrganizationDto::orgNumber))
            .toList()))
        .sorted(Comparator.comparing(TenantDto::tenant))
        .toList();

    return new TenantsResponse(tenantDtos);
  }

  private TenantOrganizationDto organizationFromEntry(
      final OrgRightsClaim.OrgEntry entry, final String orgNumber,
      final RegistryProperties.InstanceProperties instance) {
    return new TenantOrganizationDto(
        orgNumber,
        this.resolveEntryName(entry, orgNumber),
        this.instancePlacementService.resolveEntityPrefix(orgNumber, instance.functionGroups().getFirst())
            .orElse(null));
  }

  private Optional<String> nameFromOrganization(final Organization organization) {
    final String orgName = organization.getOrgName();
    return (orgName != null && !orgName.isBlank()) ? Optional.of(orgName) : Optional.empty();
  }

  /**
   * Resolves the display name for a right-holder organization: the {@code org_rights} token claim's Swedish name
   * first, then its English name, falling back to the organization number itself.
   */
  private String resolveEntryName(final OrgRightsClaim.OrgEntry entry, final String orgNumber) {
    return nameFromEntry(entry).orElse(orgNumber);
  }

  private static Optional<String> nameFromEntry(final OrgRightsClaim.OrgEntry entry) {
    final String nameSv = entry.name().get("sv");
    if (nameSv != null && !nameSv.isBlank()) {
      return Optional.of(nameSv);
    }
    final String nameEn = entry.name().get("en");
    if (nameEn != null && !nameEn.isBlank()) {
      return Optional.of(nameEn);
    }
    return Optional.empty();
  }
}
