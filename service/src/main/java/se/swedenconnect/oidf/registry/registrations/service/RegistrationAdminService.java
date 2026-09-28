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
package se.swedenconnect.oidf.registry.registrations.service;

import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.dto.AdminDomainDto;
import se.swedenconnect.oidf.registry.organization.dto.AdminOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRejectionResultDto;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.PreValidatedTrustMarksDto;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;

import java.util.List;
import java.util.UUID;

/**
 * Admin service for reviewing and managing registration requests.
 *
 * @author Per Fredrik Plars
 */
public interface RegistrationAdminService {

  /**
   * Counts registrations with PENDING_APPROVAL status for a given intermediate. Scoped to
   * intermediates owned by the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param taimId the intermediate ID
   * @return count of pending registrations
   */
  long countPending(OrganizationRecord organizationRecord, UUID taimId);

  /**
   * Rejects a pending registration. The registration must be connected to an intermediate owned
   * by the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param id the registration ID
   * @param rejectionReason the reason for rejection
   * @return the updated registration DTO
   */
  RegistrationDto reject(OrganizationRecord organizationRecord, UUID id, String rejectionReason);

  /**
   * Return all registrations that is done towards this organization intermidiate:s
   *
   * @param organizationRecord the calling organization
   * @return list of registration records
   */
  List<RegistrationDto> listRegistrationsConnectedToThisOrgIM(OrganizationRecord organizationRecord);

  /**
   * Getting a registration by ID. The registration must be connected to an intermediate owned by
   * the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @return the registration DTO
   */
  RegistrationDto getRegistrationById(OrganizationRecord organizationRecord, UUID registrationId);

  /**
   * Approves a specific pending step, resumes pipeline execution from that step. The registration
   * must be connected to an intermediate owned by the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @param stepIndex the index of the step to approve
   * @return the updated registration DTO after resumption
   */
  RegistrationDto approveStep(OrganizationRecord organizationRecord, UUID registrationId, int stepIndex);

  /**
   * Lists the domains claimed by every organization on the calling tenant operator's instance. Domain review is
   * a tenant-wide duty, so the listing is not limited to the reviewing organization's own domains.
   *
   * @param organizationRecord the calling tenant operator
   * @param status optional status to filter on, {@code null} for every status
   * @return the matching domains
   */
  List<AdminDomainDto> listDomains(OrganizationRecord organizationRecord, DomainStatus status);

  /**
   * Counts the domains still awaiting review on the calling tenant operator's instance.
   *
   * @param organizationRecord the calling tenant operator
   * @return the number of pending domains
   */
  long countPendingDomains(OrganizationRecord organizationRecord);

  /**
   * Approves a pending domain.
   *
   * @param organizationRecord the calling tenant operator
   * @param domainId the domain to approve
   * @return the approved domain
   */
  AdminDomainDto approveDomain(OrganizationRecord organizationRecord, UUID domainId);

  /**
   * Rejects a pending domain and cascades the rejection to the registrations that depended on it.
   *
   * @param organizationRecord the calling tenant operator
   * @param domainId the domain to reject
   * @param rejectionReason the reason shown to the organization
   * @return the rejected domain together with the registrations the rejection cascaded to
   */
  DomainRejectionResultDto rejectDomain(OrganizationRecord organizationRecord, UUID domainId,
      String rejectionReason);

  /**
   * Replaces the pre-validated trust mark types of an organization on the calling tenant operator's instance.
   *
   * @param organizationRecord the calling tenant operator
   * @param targetOrgNumber the organization number whose trust mark types are replaced
   * @param request the trust mark types to pre-approve
   * @return the updated organization
   */
  OrganizationDto replacePreValidatedTrustMarks(OrganizationRecord organizationRecord, String targetOrgNumber,
      PreValidatedTrustMarksDto request);

  /**
   * Lists every organization placed on the calling tenant operator's instance, ordered by the name the operator
   * reads them under. Administration is a tenant-wide duty, so the listing is not limited to the operator's own
   * organization.
   *
   * @param organizationRecord the calling tenant operator
   * @return the organizations on the operator's instance
   */
  List<AdminOrganizationDto> listOrganizations(OrganizationRecord organizationRecord);

  /**
   * Returns a single organization on the calling tenant operator's instance, with its domains.
   *
   * @param organizationRecord the calling tenant operator
   * @param targetOrgNumber the organization number to look up
   * @return the organization
   */
  OrganizationDto getOrganizationOnTenant(OrganizationRecord organizationRecord, String targetOrgNumber);

  /**
   * Lists the trust mark types issued on the calling tenant operator's instance — the types the operator can
   * pre-approve for an organization without inventing one.
   *
   * @param organizationRecord the calling tenant operator
   * @return the distinct trust mark types on the operator's instance, sorted
   */
  List<String> listTrustMarkTypes(OrganizationRecord organizationRecord);
}
