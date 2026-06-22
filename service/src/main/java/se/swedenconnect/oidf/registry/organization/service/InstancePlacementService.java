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
    if (this.registryProperties.instances().isEmpty()) {
      return Optional.empty();
    }

    for (final RegistryProperties.InstanceProperties instance : this.registryProperties.instances()) {
      final RegistryProperties.InstanceMatcherProperties matcher = instance.matchers();

      final boolean orgNumberMatch = Optional.ofNullable(matcher.org_numbers())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(orgNr -> orgNr.equals(orgNumber));

      if (orgNumberMatch) {
        return Optional.of(this.entityPrefixFrom(instance, orgNumber));
      }

      final boolean functionGroupMatch = functionGroup != null
          && Optional.ofNullable(matcher.functiongroups())
              .orElse(Collections.emptyList())
              .stream()
              .anyMatch(fg -> fg.equals(functionGroup));

      if (functionGroupMatch) {
        return Optional.of(this.entityPrefixFrom(instance, orgNumber));
      }
    }

    return this.registryProperties.instances().stream()
        .filter(i -> i.matchers().useForDefaultAssignment())
        .map(i -> this.entityPrefixFrom(i, orgNumber))
        .findFirst();
  }

  private String entityPrefixFrom(final RegistryProperties.InstanceProperties instance, final String orgNumber) {
    return Optional.ofNullable(instance.orgBaseUrlOverrides())
        .map(overrides -> overrides.get(orgNumber))
        .orElse(instance.baseUrl().toString() + "/" + orgNumber);

  }

  /**
   * Resolves the base URL of the instance that this organization is placed on. Pure config lookup — no database
   * access.
   *
   * @param organizationRecord data to be used when matching
   * @return base URL of the matched instance, or empty if no instance matches
   */
  public Optional<URI> resolveBaseUrl(final OrganizationRecord organizationRecord) {
    if (this.registryProperties.instances().isEmpty()) {
      return Optional.empty();
    }
    for (final RegistryProperties.InstanceProperties instance : this.registryProperties.instances()) {
      final RegistryProperties.InstanceMatcherProperties matcher = instance.matchers();

      final boolean orgNumberMatch = Optional.ofNullable(matcher.org_numbers())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(n -> n.equals(organizationRecord.orgNumber()));
      if (orgNumberMatch) {
        return Optional.of(instance.baseUrl());
      }

      final boolean functionGroupMatch = organizationRecord.functionGroup() != null
          && Optional.ofNullable(matcher.functiongroups())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(fg -> fg.equals(organizationRecord.functionGroup()));
      if (functionGroupMatch) {
        return Optional.of(instance.baseUrl());
      }
    }
    return this.registryProperties.instances().stream()
        .filter(i -> i.matchers().useForDefaultAssignment())
        .map(RegistryProperties.InstanceProperties::baseUrl)
        .findFirst();
  }

  /**
   * Finds the instance to be used for this organization. It matches on organization_number or functiongroup
   * @param organizationRecord data to be used when matching data.
   * @return Instance object if found, else empty Optional
   */
  public Optional<Instance> resolveInstance(final OrganizationRecord organizationRecord) {
    if (this.registryProperties.instances().isEmpty()) {
      return Optional.empty();
    }


    for (final RegistryProperties.InstanceProperties instance : this.registryProperties.instances()) {

      final RegistryProperties.InstanceMatcherProperties matcher = instance.matchers();

      final boolean orgNumberMatch = Optional.ofNullable(matcher.org_numbers())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(orgNr -> orgNr.equals(organizationRecord.orgNumber()));

      if (orgNumberMatch) {
        return this.instanceRepository.findById(instance.instanceId());
      }

      final boolean functionGroupMatch = Optional.ofNullable(matcher.functiongroups())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(orgNr -> orgNr.equals(organizationRecord.functionGroup()));

      if (functionGroupMatch) {
        return this.instanceRepository.findById(instance.instanceId());
      }

    }

    return this.registryProperties.instances().stream()
        .filter(instanceProperties -> instanceProperties.matchers().useForDefaultAssignment())
        .map(RegistryProperties.InstanceProperties::instanceId)
        .map(this.instanceRepository::findById)
        .findAny()
        .orElse(Optional.empty());

  }

}
