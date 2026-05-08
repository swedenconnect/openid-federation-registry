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

import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;

import java.util.Collections;
import java.util.Optional;

/**
 * Finds the instance to be used for this organization
 *
 * @author Per Fredrik Plars
 */
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
