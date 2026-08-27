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
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * The OrganizationService class provides methods and functionality for handling operations related to organizations in
 * the registry service.
 *
 * @author Per Fredrik Plars
 */
@Service
@Slf4j
public class OrganizationService {
  final OrganizationRepository organizationRepository;
  final InstancePlacementService instancePlacementService;

  /**
   * Constructs an instance of OrganizationService, initializing the organization and instance repositories required
   * for operations related to organizations and their instances in the registry service.
   *
   * @param organizationRepository the repository responsible for managing organization entities
   * @param instancePlacementService the repository responsible for managing instance entities
   */
  public OrganizationService(final OrganizationRepository organizationRepository,
      final InstancePlacementService instancePlacementService) {
    this.organizationRepository = organizationRepository;
    this.instancePlacementService = instancePlacementService;
  }

  /**
   * Finds an existing organization entity by its organization number on the instance resolved for the given record,
   * or creates a new one if it does not exist. The same organization number may be registered on more than one
   * instance, so lookup and creation are always scoped to the resolved instance.
   *
   * @param organizationRecord the organization record used to search for or create an organization
   * @return the existing or newly created {@link Organization}
   * @throws IllegalArgumentException if no instance was found for the given matcher config
   */
  public Organization findCreate(final OrganizationRecord organizationRecord) {

    final Instance instanceEntity = this.instancePlacementService.resolveInstance(organizationRecord)
        .orElseThrow(() ->
            new IllegalArgumentException("No instance was found for the given matcher config"));

    return this.find(organizationRecord)
        .orElseGet(() -> {
          final Organization org = new Organization();
          org.setOrganizationId(UUID.randomUUID());
          org.setOrgNumber(organizationRecord.orgNumber());
          org.setOrgName(organizationRecord.orgName());
          org.setInstance(instanceEntity);
          final Organization saved = this.organizationRepository.save(org);
          //TODO: This is a significand event that should trigger a audit event.
          log.info("Creating a new organization. {}-{}-{} assigning to instanceid:{}",
              saved.getOrganizationId(), saved.getOrgName(), saved.getOrgNumber(),
              saved.getInstance().getInstanceId());
          return saved;
        });
  }

  /**
   * Finds an existing organization entity by its organization number on the instance resolved for the given record,
   * without creating one if absent. Read-only counterpart to {@link #findCreate} — use this for authorization/ownership
   * checks, where creating an organization as a side effect of a lookup would be incorrect.
   *
   * @param organizationRecord the organization record used to search for an organization
   * @return the existing {@link Organization}, or empty if none exists for this record
   * @throws IllegalArgumentException if no instance was found for the given matcher config
   */
  public Optional<Organization> find(final OrganizationRecord organizationRecord) {
    final Instance instanceEntity = this.instancePlacementService.resolveInstance(organizationRecord)
        .orElseThrow(() ->
            new IllegalArgumentException("No instance was found for the given matcher config"));
    return this.organizationRepository
        .findByInstance_InstanceIdAndOrgNumber(instanceEntity.getInstanceId(), organizationRecord.orgNumber());
  }

}
