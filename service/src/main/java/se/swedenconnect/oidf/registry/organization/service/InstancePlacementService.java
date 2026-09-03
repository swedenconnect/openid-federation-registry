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

package se.swedenconnect.oidf.registry.organization.service;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.config.KeyEntry;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Finds the instance (tenant) to be used for this organization.
 *
 * <p>The tenant is the routing key: an instance is identified by its
 * {@link RegistryProperties.InstanceProperties#slug()}, which is what appears as the {@code {tenant\}} path
 * variable. Function groups are <em>not</em> a routing key — they only carry authorization (which rights the
 * caller holds), and the same function group value may back more than one tenant. Instance lookup therefore
 * never keys off a function group.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Service
public class InstancePlacementService {

  private final RegistryProperties registryProperties;
  private final InstanceRepository instanceRepository;

  /**
   * Constructor
   * @param registryProperties Properties where the instance configuration exists
   * @param instanceRepository Instance repository used to load a instance when found.
   */
  public InstancePlacementService(final RegistryProperties registryProperties,
      final InstanceRepository instanceRepository) {
    this.registryProperties = registryProperties;
    this.instanceRepository = instanceRepository;
  }

  /**
   * Resolves the entity prefix for an organization by finding its tenant and combining the
   * instance base URL with the organization number. Pure config lookup — no database access.
   *
   * @param orgNumber organization number
   * @param tenant the tenant slug (or, equivalently, its configured name)
   * @return entity prefix on the form {@code baseUrl/orgNumber}, or empty if no instance matches
   */
  public Optional<String> resolveEntityPrefix(final String orgNumber, final String tenant) {
    return this.findInstanceByTenant(tenant)
        .map(instance -> this.entityPrefixFrom(instance, orgNumber))
        .map(URI::toString);
  }

  /**
   * Resolves the entity prefix for an already-persisted organization, using the instance it is already placed on
   * rather than a tenant supplied by the caller. Use this when there is no tenant path variable in scope (e.g.
   * resolving the prefix for a different organization than the one making the request).
   *
   * @param organization the already-persisted organization, carrying the instance it is placed on
   * @return entity prefix on the form {@code baseUrl/orgNumber}, or empty if no instance matches
   */
  public Optional<String> resolveEntityPrefixForPlacedOrg(final Organization organization) {
    return this.findInstanceById(organization.getInstance().getInstanceId())
        .map(instance -> this.entityPrefixFrom(instance, organization.getOrgNumber()))
        .map(URI::toString);
  }

  /**
   * Resolves the base URL of the instance backing the given tenant. Pure config lookup — no database access.
   *
   * @param tenant the tenant slug (or, equivalently, its configured name)
   * @return base URL of the matched instance, or empty if no instance matches
   */
  public Optional<URI> resolveBaseUrl(final String tenant) {
    return this.findInstanceByTenant(tenant)
        .map(RegistryProperties.InstanceProperties::baseUrl);
  }

  /**
   * Resolves the base URL of the instance that this organization is placed on. Pure config lookup — no database
   * access.
   *
   * @param organizationRecord data to be used when matching
   * @return base URL of the matched instance, or empty if no instance matches
   */
  public Optional<URI> resolveBaseUrl(final OrganizationRecord organizationRecord) {
    return this.resolveBaseUrl(organizationRecord.tenant());
  }

  /**
   * Resolves the base URL of the instance that this organization is placed on. Pure config lookup — no database
   * access.
   *
   * @param instanceId instanceid for the instance
   * @return base URL of the matched instance, or empty if no instance matches
   */
  public Optional<URI> resolveBaseUrl(final UUID instanceId) {
    return this.findInstanceById(instanceId)
        .map(RegistryProperties.InstanceProperties::baseUrl);
  }

  /**
   * Finds the instance to be used for this organization, matching on the record's tenant.
   *
   * @param organizationRecord data to be used when matching data.
   * @return Instance object if found, else empty Optional
   */
  public Optional<Instance> resolveInstance(final OrganizationRecord organizationRecord) {
    return this.findInstanceByTenant(organizationRecord.tenant())
        .flatMap(instance -> this.instanceRepository.findById(instance.instanceId()));
  }

  /**
   * Resolves the public validation key configured for the instance backing the given tenant.
   * The key is used to verify JWT responses from the oidf-service node attached to that instance.
   *
   * @param tenant the tenant slug (or, equivalently, its configured name)
   * @return the parsed {@link JWK} from the instance's {@code oidf_service_api_validation_key}, or empty if
   *     the instance has no such key configured
   */
  public Optional<JWK> resolveValidationKey(final String tenant) {
    return this.findInstanceByTenant(tenant)
        .map(RegistryProperties.InstanceProperties::oidfServiceApiValidationKey)
        .map(KeyEntry::getKey);
  }

  /**
   * Resolves the canonical tenant slug for the given tenant name, e.g. the {@code {tenant\}} path variable.
   * Returns empty if no configured instance goes by that name, which is what makes an unknown tenant a 403
   * rather than a silent fallthrough.
   *
   * @param tenantName the tenant's {@link RegistryProperties.InstanceProperties#slug()} or, equivalently, its
   *     configured {@link RegistryProperties.InstanceProperties#name()}
   * @return the canonical tenant slug, or empty if no configured instance has that name
   */
  public Optional<String> resolveTenant(final String tenantName) {
    return this.findInstanceByTenant(tenantName)
        .map(RegistryProperties.InstanceProperties::slug);
  }

  /**
   * Resolves all function groups backing the tenant with the given name, e.g. the tenant slug from a request path.
   * These are the function group values a caller's {@code org_rights} claim is matched against for this tenant;
   * a given function group may equally back other tenants.
   *
   * @param tenantName the tenant's {@link RegistryProperties.InstanceProperties#slug()} or, equivalently, its
   *     configured {@link RegistryProperties.InstanceProperties#name()}
   * @return the function groups backing that tenant, or empty if no configured instance has that name
   */
  public Optional<List<String>> resolveFunctionGroupsForTenant(final String tenantName) {
    return this.findInstanceByTenant(tenantName)
        .map(RegistryProperties.InstanceProperties::functionGroups);
  }

  /**
   * Resolves the tenant slug of the instance a persisted organization is placed on. Pure config lookup — no
   * database access.
   *
   * @param organization the already-persisted organization, carrying the instance it is placed on
   * @return the tenant slug the organization is placed on, or empty if no configured instance matches the
   *     organization's instance
   */
  public Optional<String> resolveTenantForPlacedOrg(final Organization organization) {
    return this.findInstanceById(organization.getInstance().getInstanceId())
        .map(RegistryProperties.InstanceProperties::slug);
  }

  /**
   * Finds the {@link RegistryProperties.InstanceProperties} for a tenant, matching on the tenant slug. The
   * incoming value is slugged first, so a tenant may be addressed either by its slug or by its configured name.
   */
  private Optional<RegistryProperties.InstanceProperties> findInstanceByTenant(final String tenantName) {
    if (tenantName == null) {
      return Optional.empty();
    }
    final String slug = RegistryProperties.InstanceProperties.toSlug(tenantName);
    return this.registryProperties.instances().stream()
        .filter(instance -> instance.slug().equals(slug))
        .findFirst();
  }

  private Optional<RegistryProperties.InstanceProperties> findInstanceById(final UUID instanceId) {
    if (instanceId == null) {
      return Optional.empty();
    }
    return this.registryProperties.instances().stream()
        .filter(instance -> instance.instanceId().equals(instanceId))
        .findFirst();
  }

  private URI entityPrefixFrom(
      final RegistryProperties.InstanceProperties instance, final String orgNumber) {
    return Optional.ofNullable(instance.orgBaseUrlOverrides())
        .map(overrides -> overrides.get(orgNumber))
        .orElse(URI.create(instance.baseUrl().toString() + "/" + orgNumber));
  }

}
