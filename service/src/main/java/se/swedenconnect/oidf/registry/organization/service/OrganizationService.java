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
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationDomainRepository;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;

import java.util.List;
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

  /** Statuses that count as a registered domain when enforcing registration requests. */
  public static final List<DomainStatus> REGISTERED_DOMAIN_STATUSES =
      List.of(DomainStatus.PENDING, DomainStatus.VALIDATED);

  final OrganizationRepository organizationRepository;
  final InstancePlacementService instancePlacementService;
  final OrganizationDomainRepository organizationDomainRepository;
  final RegistryProperties registryProperties;
  final RegistryAuditService auditService;

  /**
   * Constructs an instance of OrganizationService, initializing the organization and instance repositories required
   * for operations related to organizations and their instances in the registry service.
   *
   * @param organizationRepository the repository responsible for managing organization entities
   * @param instancePlacementService the repository responsible for managing instance entities
   * @param organizationDomainRepository the repository holding the domains an organization has claimed
   * @param registryProperties the registry configuration, carrying the domain-enforcement switch
   * @param auditService the audit service organization lifecycle events are emitted through
   */
  public OrganizationService(final OrganizationRepository organizationRepository,
      final InstancePlacementService instancePlacementService,
      final OrganizationDomainRepository organizationDomainRepository,
      final RegistryProperties registryProperties,
      final RegistryAuditService auditService) {
    this.organizationRepository = organizationRepository;
    this.instancePlacementService = instancePlacementService;
    this.organizationDomainRepository = organizationDomainRepository;
    this.registryProperties = registryProperties;
    this.auditService = auditService;
  }

  /**
   * Finds an existing organization entity by its organization number on the instance resolved for the given record,
   * or creates a new one if it does not exist. The same organization number may be registered on more than one
   * instance, so lookup and creation are always scoped to the resolved instance.
   *
   * <p>An organization created this way carries no legal name — that is only set when the organization bootstraps
   * its own record through the organization API.
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
          log.info("Creating a new organization. {}-{}-{} assigning to instanceid:{}",
              saved.getOrganizationId(), saved.getOrgName(), saved.getOrgNumber(),
              saved.getInstance().getInstanceId());
          this.auditService.organizationCreated(saved.getOrganizationId(),
              saved.getInstance().getInstanceId(), saved.getOrganizationId(), null,
              new OrganizationDto(saved.getOrgNumber(), saved.getOrgName(), saved.getLegalName(),
                  organizationRecord.tenant(), List.of(), List.of()));
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

  /**
   * Requires that the host of the given entity identifier is covered by one of the calling organization's
   * registered ({@link DomainStatus#PENDING} or {@link DomainStatus#VALIDATED}) domains.
   *
   * <p>The check is skipped entirely when
   * {@code openid.federation.registry.registration.require-registered-domain} is set to {@code false}. It is never
   * skipped for a superuser: owning the registry does not make an entity identifier the organization's to claim.
   *
   * @param organizationRecord the calling organization
   * @param entityIdentifier the entity identifier the registration request is for
   * @throws RegistryServerException with {@link ErrorTypes#INVALID_PARAMETER} if the host is not registered
   */
  public void requireRegisteredDomain(final OrganizationRecord organizationRecord, final String entityIdentifier) {
    if (!this.registryProperties.requireRegisteredDomain()) {
      return;
    }
    final String host = DomainMatcher.hostOf(entityIdentifier)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
            "Entity identifier '%s' has no resolvable host".formatted(entityIdentifier)));

    final List<String> registeredDomains = this.find(organizationRecord)
        .map(organization -> this.organizationDomainRepository
            .findByOrganization_OrganizationIdAndStatusIn(organization.getOrganizationId(),
                REGISTERED_DOMAIN_STATUSES))
        .orElse(List.of())
        .stream()
        .map(OrganizationDomain::getDomain)
        .toList();

    if (!DomainMatcher.matchesAny(host, registeredDomains)) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "Entity identifier host '%s' is not a registered domain of organization %s"
              .formatted(host, organizationRecord.orgNumber()));
    }
  }
}
