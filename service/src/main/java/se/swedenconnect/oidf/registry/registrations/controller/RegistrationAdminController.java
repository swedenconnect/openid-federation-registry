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
package se.swedenconnect.oidf.registry.registrations.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.dto.AdminDomainDto;
import se.swedenconnect.oidf.registry.organization.dto.AdminOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRejectionResultDto;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.PreValidatedTrustMarksDto;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RejectRegistrationDto;
import se.swedenconnect.oidf.registry.registrations.service.RegistrationAdminService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Operator API for reviewing pending registration requests.
 * <p>
 * Approval is done via the subordinate dialog — the operator opens a PENDING record,
 * which pre-fills the subordinate form. Rejection is handled here.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registration-admin/v1/{tenant}/{orgNumber}")
@Tag(name = "RegistrationAdmin", description = "Operator view of incoming registration requests")
public class RegistrationAdminController {

  private final RegistrationAdminService registrationAdminService;

  /**
   * Counts unhandled PENDING registrations for an intermediate.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param taimId the intermediate ID
   * @return map containing the count
   */
  @GetMapping("/count")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Count unhandled PENDING registrations for an intermediate")
  public ResponseEntity<Map<String, Long>> countPending(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Intermediate ID") @RequestParam("taimId") final UUID taimId) {
    return ResponseEntity.ok(
        Map.of("count", this.registrationAdminService.countPending(organizationRecord, taimId)));
  }

  /**
   * List all registration records for this organizations intermidiates
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @return list of registration DTOs
   */
  @GetMapping
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List all registration records for current organization")
  public ResponseEntity<List<RegistrationDto>> listRegistrations(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationAdminService.listRegistrationsConnectedToThisOrgIM(organizationRecord));
  }

  /**
   * Returns a single registration by ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @return the registration DTO
   */
  @GetMapping("/{registrationId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get a single registration by ID")
  public ResponseEntity<RegistrationDto> getById(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Registration ID") @PathVariable("registrationId") final UUID registrationId) {
    return ResponseEntity.ok(this.registrationAdminService.getRegistrationById(organizationRecord, registrationId));
  }

  /**
   * Rejects a pending registration request.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @param body the rejection details
   * @return the updated registration DTO
   */
  @PostMapping("/{registrationId}/reject")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Reject a pending registration request")
  public ResponseEntity<RegistrationDto> reject(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Registration ID") @PathVariable("registrationId") final UUID registrationId,
      @RequestBody final RejectRegistrationDto body) {
    return ResponseEntity.ok(
        this.registrationAdminService.reject(organizationRecord, registrationId, body.rejectionReason()));
  }

  /**
   * Approves a specific pending step and resumes pipeline execution from that step.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @param stepIndex the zero-based index of the step to approve
   * @return the updated registration DTO after resumption
   */
  @PostMapping("/{registrationId}/steps/{stepIndex}/approve")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Approve a specific pending pipeline step and resume execution")
  public ResponseEntity<RegistrationDto> approveStep(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Registration ID") @PathVariable("registrationId") final UUID registrationId,
      @Parameter(description = "Step index") @PathVariable("stepIndex") final int stepIndex) {
    return ResponseEntity.ok(
        this.registrationAdminService.approveStep(organizationRecord, registrationId, stepIndex));
  }

  /**
   * Lists the domains claimed by every organization on this tenant, for the tenant operator to review.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param status optional status to filter on
   * @return list of domains awaiting or having passed review
   */
  @GetMapping("/domains")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List domains claimed by organizations on this tenant")
  public ResponseEntity<List<AdminDomainDto>> listDomains(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Domain status to filter on")
      @RequestParam(value = "status", required = false) final DomainStatus status) {
    return ResponseEntity.ok(this.registrationAdminService.listDomains(organizationRecord, status));
  }

  /**
   * Counts the domains still awaiting review on this tenant.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @return map containing the count
   */
  @GetMapping("/domains/count")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Count domains awaiting review on this tenant")
  public ResponseEntity<Map<String, Long>> countPendingDomains(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        Map.of("count", this.registrationAdminService.countPendingDomains(organizationRecord)));
  }

  /**
   * Approves a pending domain.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param domainId the domain ID
   * @return the approved domain
   */
  @PostMapping("/domains/{domainId}/approve")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Approve a pending domain")
  public ResponseEntity<AdminDomainDto> approveDomain(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Domain ID") @PathVariable("domainId") final UUID domainId) {
    return ResponseEntity.ok(this.registrationAdminService.approveDomain(organizationRecord, domainId));
  }

  /**
   * Rejects a pending domain. Registrations that depended on the domain, and on no other registered domain of
   * the same organization, are rejected along with it.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param domainId the domain ID
   * @param body the rejection details
   * @return the rejected domain together with the registrations the rejection cascaded to
   */
  @PostMapping("/domains/{domainId}/reject")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Reject a pending domain and cascade to dependent registrations")
  public ResponseEntity<DomainRejectionResultDto> rejectDomain(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Domain ID") @PathVariable("domainId") final UUID domainId,
      @RequestBody final RejectRegistrationDto body) {
    return ResponseEntity.ok(
        this.registrationAdminService.rejectDomain(organizationRecord, domainId, body.rejectionReason()));
  }

  /**
   * Lists every organization on this tenant, for the tenant operator to administer.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @return the organizations on the tenant's instance, ordered by name
   */
  @GetMapping("/organizations")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List the organizations on this tenant")
  public ResponseEntity<List<AdminOrganizationDto>> listOrganizations(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationAdminService.listOrganizations(organizationRecord));
  }

  /**
   * Returns a single organization on this tenant, with its domains.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param targetOrgNumber the organization number to look up
   * @return the organization
   */
  @GetMapping("/organizations/{targetOrgNumber}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get a single organization on this tenant")
  public ResponseEntity<OrganizationDto> getOrganizationOnTenant(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Target organization number")
      @PathVariable("targetOrgNumber") final String targetOrgNumber) {
    return ResponseEntity.ok(
        this.registrationAdminService.getOrganizationOnTenant(organizationRecord, targetOrgNumber));
  }

  /**
   * Lists the trust mark types issued on this tenant — what the operator can pre-approve for an organization.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @return the distinct trust mark types on the tenant's instance, sorted
   */
  @GetMapping("/trustmark-types")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List the trust mark types issued on this tenant")
  public ResponseEntity<List<String>> listTrustMarkTypes(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationAdminService.listTrustMarkTypes(organizationRecord));
  }

  /**
   * Replaces the pre-validated trust mark types of an organization on this tenant.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param targetOrgNumber the organization number whose trust mark types are replaced
   * @param body the trust mark types to pre-approve
   * @return the updated organization
   */
  @PutMapping("/organizations/{targetOrgNumber}/trustmarks")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Replace an organization's pre-validated trust mark types")
  public ResponseEntity<OrganizationDto> replacePreValidatedTrustMarks(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Target organization number")
      @PathVariable("targetOrgNumber") final String targetOrgNumber,
      @RequestBody final PreValidatedTrustMarksDto body) {
    return ResponseEntity.ok(this.registrationAdminService
        .replacePreValidatedTrustMarks(organizationRecord, targetOrgNumber, body));
  }
}
