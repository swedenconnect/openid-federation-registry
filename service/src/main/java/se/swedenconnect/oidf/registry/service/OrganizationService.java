/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.registry.service;

import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.repository.OrganizationRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * The OrganizationService class provides methods and functionality for handling operations related to organizations in
 * the registry service.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OrganizationService {
  final OrganizationRepository organizationRepository;
  final InstanceRepository instanceRepository;

  /**
   * Constructs an instance of OrganizationService, initializing the organization and instance repositories required for
   * operations related to organizations and their instances in the registry service.
   *
   * @param organizationRepository the repository responsible for managing organization entities
   * @param instanceRepository the repository responsible for managing instance entities
   */
  public OrganizationService(final OrganizationRepository organizationRepository,
      final InstanceRepository instanceRepository) {
    this.organizationRepository = organizationRepository;
    this.instanceRepository = instanceRepository;
  }

  /**
   * Finds an existing organization entity by its organization number, or creates a new one if it does not exist. If a
   * new organization is created, it is assigned to the default assignment instance and saved to the repository.
   *
   * @param orgNumber the organization number used to search for an existing organization
   * @param orgName the name of the organization to be created if no existing organization is found
   * @return the existing or newly created {@link OrganizationEntity}
   * @throws RuntimeException if no default assignment instance is configured
   */

  public OrganizationEntity findCreate(final String orgNumber, final String orgName) {

    return this.organizationRepository.findByOrgNumber(orgNumber).or(() -> {

      final InstanceEntity instanceEntity = this.instanceRepository.findByUseForDefaultAssignment(true)
          .orElseThrow(() ->
              new IllegalArgumentException("There is no default assignment instance configured. "
                  + "Review the configuration"));

      final OrganizationEntity org = new OrganizationEntity();
      org.setOrganizationId(UUID.randomUUID());
      org.setOrgNumber(orgNumber);
      org.setOrgName(orgName);
      org.setCreatedBy("Registry Service");
      org.setLastModifiedBy(org.getCreatedBy());
      instanceEntity.addOrganization(org);
      this.organizationRepository.saveAndFlush(org);
      return Optional.of(org);

    }).orElseThrow();

  }

  ;

}
