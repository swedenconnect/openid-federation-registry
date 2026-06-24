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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

/**
 * Finds the instance to be used for this organization
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Slf4j
@Service
public class InstancePlacementService {

  final RegistryProperties registryProperties;
  final InstanceRepository instanceRepository;

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
    return this.findMatchingInstance(orgNumber, functionGroup)
        .map(instance -> this.entityPrefixFrom(instance, orgNumber));
  }

  /**
   * Resolves the base URL of the instance that this organization is placed on. Pure config lookup — no database
   * access.
   *
   * @param orgNumber organization number
   * @param functionGroup optional function group used for matching
   * @return base URL of the matched instance, or empty if no instance matches
   */
  public Optional<URI> resolveBaseUrl(final String orgNumber, final String functionGroup) {
    return this.findMatchingInstance(orgNumber, functionGroup)
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
    return this.resolveBaseUrl(organizationRecord.orgNumber(), organizationRecord.functionGroup());
  }

  /**
   * Finds the instance to be used for this organization. It matches on organization_number or functiongroup
   * @param organizationRecord data to be used when matching data.
   * @return Instance object if found, else empty Optional
   */
  public Optional<Instance> resolveInstance(final OrganizationRecord organizationRecord) {
    return this.findMatchingInstance(organizationRecord.orgNumber(), organizationRecord.functionGroup())
        .flatMap(instance -> this.instanceRepository.findById(instance.instanceId()));
  }

  /**
   * Finds the first {@link RegistryProperties.InstanceProperties} that matches the given org number or function group.
   * Falls back to the instance marked as default if no explicit match is found.
   */
  private Optional<RegistryProperties.InstanceProperties> findMatchingInstance(
      final String orgNumber, final String functionGroup) {

    if (this.registryProperties.instances().isEmpty()) {
      return Optional.empty();
    }

    for (final RegistryProperties.InstanceProperties instance : this.registryProperties.instances()) {
      final RegistryProperties.InstanceMatcherProperties matcher = instance.matchers();
      if (this.matchesOrgNumber(matcher, orgNumber) || this.matchesFunctionGroup(matcher, functionGroup)) {
        return Optional.of(instance);
      }
    }

    return this.registryProperties.instances().stream()
        .filter(i -> i.matchers().useForDefaultAssignment())
        .findFirst();
  }

  private boolean matchesOrgNumber(
      final RegistryProperties.InstanceMatcherProperties matcher, final String orgNumber) {
    return Optional.ofNullable(matcher.org_numbers())
        .orElse(Collections.emptyList())
        .stream()
        .anyMatch(orgNr -> orgNr.equals(orgNumber));
  }

  private boolean matchesFunctionGroup(
      final RegistryProperties.InstanceMatcherProperties matcher, final String functionGroup) {
    return functionGroup != null
        && Optional.ofNullable(matcher.functiongroups())
        .orElse(Collections.emptyList())
        .stream()
        .anyMatch(fg -> fg.equals(functionGroup));
  }

  private String entityPrefixFrom(
      final RegistryProperties.InstanceProperties instance, final String orgNumber) {
    return Optional.ofNullable(instance.orgBaseUrlOverrides())
        .map(overrides -> overrides.get(orgNumber))
        .orElse(instance.baseUrl().toString() + "/" + orgNumber);
  }

}