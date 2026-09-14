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
package se.swedenconnect.oidf.registry.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link OrganizationDomain} entities.
 *
 * <p>Organization-scoped lookups back the portal API; instance-scoped lookups back the operator review API,
 * where a reviewer sees every organization's domains on its own tenant instance.
 *
 * @author Felix Hellman
 */
public interface OrganizationDomainRepository extends JpaRepository<OrganizationDomain, UUID> {

  /**
   * Finds all domains belonging to the given organization, regardless of status.
   *
   * @param organizationId the organization the domains belong to
   * @return the organization's domains
   */
  List<OrganizationDomain> findByOrganization_OrganizationId(UUID organizationId);

  /**
   * Finds all domains belonging to the given organization with one of the given statuses.
   *
   * @param organizationId the organization the domains belong to
   * @param statuses the statuses to include
   * @return the matching domains
   */
  List<OrganizationDomain> findByOrganization_OrganizationIdAndStatusIn(UUID organizationId,
      Collection<DomainStatus> statuses);

  /**
   * Finds a single domain of an organization by its domain name.
   *
   * @param organizationId the organization the domain belongs to
   * @param domain the lower-cased domain name
   * @return the matching domain, or empty if the organization has not claimed it
   */
  Optional<OrganizationDomain> findByOrganization_OrganizationIdAndDomain(UUID organizationId, String domain);

  /**
   * Finds a domain by ID, scoped to the organization that owns it. Used so that a foreign domain ID is
   * indistinguishable from a nonexistent one.
   *
   * @param domainId the domain ID
   * @param organizationId the organization expected to own the domain
   * @return the matching domain, or empty
   */
  Optional<OrganizationDomain> findByDomainIdAndOrganization_OrganizationId(UUID domainId, UUID organizationId);

  /**
   * Finds every domain of every organization placed on the given instance. This is the operator's review
   * scope: a tenant operator reviews domains across its whole tenant, not only its own organization's.
   *
   * @param instanceId the instance (tenant) the organizations are placed on
   * @return all domains on that instance
   */
  List<OrganizationDomain> findByOrganization_Instance_InstanceId(UUID instanceId);

  /**
   * Finds every domain with the given status of every organization placed on the given instance.
   *
   * @param instanceId the instance (tenant) the organizations are placed on
   * @param status the status to filter on
   * @return the matching domains
   */
  List<OrganizationDomain> findByOrganization_Instance_InstanceIdAndStatus(UUID instanceId, DomainStatus status);

  /**
   * Counts the domains with the given status across every organization placed on the given instance.
   *
   * @param instanceId the instance (tenant) the organizations are placed on
   * @param status the status to count
   * @return the number of matching domains
   */
  long countByOrganization_Instance_InstanceIdAndStatus(UUID instanceId, DomainStatus status);

  /**
   * Finds every claim on a given domain name across an instance (tenant), in the given statuses. Backs the rule
   * that only one organization on a tenant holds a domain at a time: a claim in {@link DomainStatus#PENDING} or
   * {@link DomainStatus#VALIDATED} holds the domain, a rejected one releases it. The match is on the exact
   * domain string -- a subdomain is a different domain and may be held by somebody else.
   *
   * @param instanceId the instance (tenant) to look on
   * @param domain the lower-cased domain name
   * @param statuses the statuses that count as holding the domain
   * @return the matching claims, across every organization on the instance
   */
  List<OrganizationDomain> findByInstance_InstanceIdAndDomainAndStatusIn(UUID instanceId, String domain,
      Collection<DomainStatus> statuses);

  /**
   * Finds a domain by ID, scoped to the instance the owning organization is placed on.
   *
   * @param domainId the domain ID
   * @param instanceId the instance (tenant) the owning organization must be placed on
   * @return the matching domain, or empty
   */
  Optional<OrganizationDomain> findByDomainIdAndOrganization_Instance_InstanceId(UUID domainId, UUID instanceId);
}
