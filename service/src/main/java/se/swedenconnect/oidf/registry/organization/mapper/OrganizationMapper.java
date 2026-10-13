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
package se.swedenconnect.oidf.registry.organization.mapper;

import se.swedenconnect.oidf.registry.organization.dto.AdminDomainDto;
import se.swedenconnect.oidf.registry.organization.dto.AdminOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRejectionResultDto;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;

import se.swedenconnect.oidf.registry.infrastructure.persistence.Timestamps;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Maps organization and domain entities to their API representations.
 *
 * @author Felix Hellman
 */
public final class OrganizationMapper {

  private OrganizationMapper() {
  }

  /**
   * Maps an organization together with its domains and pre-validated trust marks.
   *
   * @param organization the organization
   * @param tenant the tenant slug the organization is placed on
   * @param domains the organization's domains, in every status
   * @param preValidatedTrustMarks the trust mark types pre-approved for the organization
   * @return the organization representation
   */
  public static OrganizationDto toOrganizationDto(final Organization organization, final String tenant,
      final List<OrganizationDomain> domains, final List<String> preValidatedTrustMarks) {
    return new OrganizationDto(
        organization.getOrgNumber(),
        organization.getOrgName(),
        organization.getLegalName(),
        tenant,
        domains.stream().map(OrganizationMapper::toDomainDto).toList(),
        preValidatedTrustMarks);
  }

  /**
   * Maps an organization as seen by the tenant operator administering it: identity, domain counts and
   * pre-validated trust mark types, without the domains themselves.
   *
   * @param organization the organization
   * @param domainCount the number of domains the organization has claimed, in every status
   * @param pendingDomainCount how many of those domains are still awaiting review
   * @param preValidatedTrustMarks the trust mark types pre-approved for the organization
   * @return the operator's organization representation
   */
  public static AdminOrganizationDto toAdminOrganizationDto(final Organization organization,
      final long domainCount, final long pendingDomainCount, final List<String> preValidatedTrustMarks) {
    return new AdminOrganizationDto(
        organization.getOrgNumber(),
        organization.getOrgName(),
        organization.getLegalName(),
        domainCount,
        pendingDomainCount,
        preValidatedTrustMarks);
  }

  /**
   * Maps a domain as seen by the organization that owns it.
   *
   * @param domain the domain
   * @return the domain representation
   */
  public static DomainDto toDomainDto(final OrganizationDomain domain) {
    return new DomainDto(
        domain.getDomainId(),
        domain.getDomain(),
        domain.getStatus(),
        domain.getRejectionReason(),
        toOffsetDateTime(domain.getCreatedDate()),
        toOffsetDateTime(domain.getReviewedAt()));
  }

  /**
   * Maps a domain as seen by the tenant operator reviewing it, carrying the owning organization's identity.
   *
   * @param domain the domain
   * @return the domain representation
   */
  public static AdminDomainDto toAdminDomainDto(final OrganizationDomain domain) {
    final Organization organization = domain.getOrganization();
    return new AdminDomainDto(
        domain.getDomainId(),
        domain.getDomain(),
        domain.getStatus(),
        domain.getRejectionReason(),
        toOffsetDateTime(domain.getCreatedDate()),
        toOffsetDateTime(domain.getReviewedAt()),
        organization.getOrgNumber(),
        organization.getOrgName(),
        organization.getLegalName());
  }

  /**
   * Maps a rejected domain together with the registrations the rejection cascaded to.
   *
   * @param domain the rejected domain
   * @param cascadedRegistrationIds identifiers of the registrations rejected as a consequence
   * @return the rejection result representation
   */
  public static DomainRejectionResultDto toDomainRejectionResultDto(final OrganizationDomain domain,
      final List<UUID> cascadedRegistrationIds) {
    final Organization organization = domain.getOrganization();
    return new DomainRejectionResultDto(
        domain.getDomainId(),
        domain.getDomain(),
        domain.getStatus(),
        domain.getRejectionReason(),
        toOffsetDateTime(domain.getCreatedDate()),
        toOffsetDateTime(domain.getReviewedAt()),
        organization.getOrgNumber(),
        organization.getOrgName(),
        organization.getLegalName(),
        cascadedRegistrationIds);
  }

  private static OffsetDateTime toOffsetDateTime(final LocalDateTime timestamp) {
    return Timestamps.toOffsetDateTime(timestamp);
  }
}
