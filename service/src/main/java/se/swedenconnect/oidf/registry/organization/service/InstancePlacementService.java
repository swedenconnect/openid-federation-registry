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
 * Finds the instance to be used for this organization
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
   * Resolves the entity prefix for an organization by finding its instance and combining the
   * instance base URL with the organization number. Pure config lookup — no database access.
   *
   * @param orgNumber organization number
   * @param functionGroup optional function group used for matching
   * @return entity prefix on the form {@code baseUrl/orgNumber}, or empty if no instance matches
   */
  public Optional<String> resolveEntityPrefix(final String orgNumber, final String functionGroup) {
    return this.findMatchingInstance( functionGroup)
        .map(instance -> this.entityPrefixFrom(instance, orgNumber))
        .map(URI::toString);
  }

  /**
   * Resolves the entity prefix for an already-persisted organization, using the function group its instance is
   * already attached to rather than one supplied by the caller. Use this when there is no tenant path variable
   * in scope (e.g. resolving the prefix for a different organization than the one making the request).
   *
   * @param organization the already-persisted organization, carrying the instance it is placed on
   * @return entity prefix on the form {@code baseUrl/orgNumber}, or empty if no instance matches
   */
  public Optional<String> resolveEntityPrefixForPlacedOrg(final Organization organization) {
    return this.resolveAttachedFunctionGroup(organization)
        .flatMap(functionGroup -> this.resolveEntityPrefix(organization.getOrgNumber(), functionGroup));
  }

  /**
   * Resolves the base URL of the instance that this organization is placed on. Pure config lookup — no database
   * access.
   *
   * @param functionGroup optional function group used for matching
   * @return base URL of the matched instance, or empty if no instance matches
   */
  public Optional<URI> resolveBaseUrl(final String functionGroup) {
    return this.findMatchingInstance(functionGroup)
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
    return this.registryProperties.instances().stream()
        .filter(instanceProperties -> instanceProperties.instanceId().equals(instanceId))
        .map(RegistryProperties.InstanceProperties::baseUrl)
        .findFirst();
  }

  /**
   * Finds the instance to be used for this organization. It matches on organization_number or functiongroup
   * @param organizationRecord data to be used when matching data.
   * @return Instance object if found, else empty Optional
   */
  public Optional<Instance> resolveInstance(final OrganizationRecord organizationRecord) {
    return this.findMatchingInstance(organizationRecord.tenant())
        .flatMap(instance -> this.instanceRepository.findById(instance.instanceId()));
  }

  /**
   * Resolves the public validation key configured for the instance that serves the given organisation.
   * The key is used to verify JWT responses from the oidf-service node attached to that instance.
   *
   * @param functionGroup optional function group used for matching
   * @return the parsed {@link JWK} from the instance's {@code oidf_service_api_validation_key}, or empty if
   *     the instance has no such key configured
   */
  public Optional<JWK> resolveValidationKey(final String functionGroup) {
    return this.findMatchingInstance(functionGroup)
        .map(RegistryProperties.InstanceProperties::oidfServiceApiValidationKey)
        .map(KeyEntry::getKey);
  }

  /**
   * Resolves a representative function group backing the tenant with the given name, e.g. the tenant slug from a
   * request path. A tenant may be backed by more than one function group; this returns the first configured one,
   * which is sufficient for pure config/instance lookups (any member of the tenant's function groups resolves to
   * the same instance). Use {@link #resolveFunctionGroupsForTenant(String)} when the full set is needed, e.g. for
   * rights evaluation.
   *
   * @param tenantName the tenant's {@link RegistryProperties.InstanceProperties#slug()} or, equivalently, its
   *     configured {@link RegistryProperties.InstanceProperties#name()}
   * @return a function group backing that tenant, or empty if no configured instance has that name
   */
  public Optional<String> resolveFunctionGroupForTenant(final String tenantName) {
    if (tenantName == null) {
      return Optional.empty();
    }
    final String slug = RegistryProperties.InstanceProperties.toSlug(tenantName);
    return this.registryProperties.instances().stream()
        .filter(instance -> instance.slug().equals(slug))
        .map(instance -> instance.functionGroups().getFirst())
        .findFirst();
  }

  /**
   * Resolves all function groups backing the tenant with the given name, e.g. the tenant slug from a request path.
   *
   * @param tenantName the tenant's {@link RegistryProperties.InstanceProperties#slug()} or, equivalently, its
   *     configured {@link RegistryProperties.InstanceProperties#name()}
   * @return the function groups backing that tenant, or empty if no configured instance has that name
   */
  public Optional<List<String>> resolveFunctionGroupsForTenant(final String tenantName) {
    if (tenantName == null) {
      return Optional.empty();
    }
    final String slug = RegistryProperties.InstanceProperties.toSlug(tenantName);
    return this.registryProperties.instances().stream()
        .filter(instance -> instance.slug().equals(slug))
        .map(RegistryProperties.InstanceProperties::functionGroups)
        .findFirst();
  }

  /**
   * Finds the {@link RegistryProperties.InstanceProperties} backed by the given function group. A tenant
   * (instance) may be backed by more than one function group, and each function group value must map
   * unambiguously to exactly one instance, so this is a membership match.
   */
  private Optional<RegistryProperties.InstanceProperties> findMatchingInstance(
       final String functionGroup) {

    if (functionGroup == null) {
      return Optional.empty();
    }

    return this.registryProperties.instances().stream()
        .filter(instance -> instance.functionGroups().contains(functionGroup))
        .findFirst();
  }

  /**
   * Resolves a function group already attached to a persisted organization, i.e. one of the function groups
   * backing the instance the organization was placed on. Pure config lookup — no database access.
   *
   * @param organization the already-persisted organization, carrying the instance it is placed on
   * @return a function group the organization is currently attached to, or empty if no configured instance
   *     matches the organization's instance
   */
  public Optional<String> resolveAttachedFunctionGroup(final Organization organization) {
    final UUID instanceId = organization.getInstance().getInstanceId();
    return this.registryProperties.instances().stream()
        .filter(instance -> instance.instanceId().equals(instanceId))
        .findFirst()
        .map(instance -> instance.functionGroups().getFirst());
  }

  private URI entityPrefixFrom(
      final RegistryProperties.InstanceProperties instance, final String orgNumber) {
    return Optional.ofNullable(instance.orgBaseUrlOverrides())
        .map(overrides -> overrides.get(orgNumber))
        .orElse(URI.create(instance.baseUrl().toString() + "/" + orgNumber));
  }

}