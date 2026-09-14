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
package se.swedenconnect.oidf.registry.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.dto.CreateOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRequestDto;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.service.OrganizationApiService;

import java.util.List;
import java.util.UUID;

/**
 * Portal-facing organization API: bootstrapping an organization's registry record and managing the domains it
 * registers entities under.
 *
 * <p>Unlike the registration flow, nothing here creates an organization as a side effect — a {@code GET} on an
 * organization that has not bootstrapped is a 404.
 *
 * @author Felix Hellman
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/organization/v1/{tenant}/{orgNumber}")
@Tag(name = "Organization", description = "Organization bootstrap and domain registration")
public class OrganizationController {

  private final OrganizationApiService organizationApiService;

  /**
   * Returns the calling organization's registry record.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @return the organization
   */
  @GetMapping
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get the organization's registry record")
  public ResponseEntity<OrganizationDto> getOrganization(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.organizationApiService.getOrganization(organizationRecord));
  }

  /**
   * Bootstraps the calling organization's registry record on this tenant.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @param body the legal name to register
   * @return the created organization
   */
  @PostMapping
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Bootstrap the organization's registry record")
  public ResponseEntity<OrganizationDto> createOrganization(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @RequestBody final CreateOrganizationDto body) {
    return ResponseEntity.status(201)
        .body(this.organizationApiService.createOrganization(organizationRecord, body));
  }

  /**
   * Updates the calling organization's legal name.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @param body the legal name to set
   * @return the updated organization
   */
  @PutMapping
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update the organization's legal name")
  public ResponseEntity<OrganizationDto> updateOrganization(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @RequestBody final CreateOrganizationDto body) {
    return ResponseEntity.ok(this.organizationApiService.updateOrganization(organizationRecord, body));
  }

  /**
   * Lists every domain the calling organization has claimed, in every status.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @return the organization's domains
   */
  @GetMapping("/domains")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List the organization's domains")
  public ResponseEntity<List<DomainDto>> listDomains(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.organizationApiService.listDomains(organizationRecord));
  }

  /**
   * Claims a domain for the calling organization, for the tenant operator to review.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @param body the domain to claim
   * @return the created, or re-opened, domain
   */
  @PostMapping("/domains")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Claim a domain for the organization")
  public ResponseEntity<DomainDto> requestDomain(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @RequestBody final DomainRequestDto body) {
    return ResponseEntity.status(201)
        .body(this.organizationApiService.requestDomain(organizationRecord, body));
  }

  /**
   * Withdraws one of the calling organization's domains, whatever its status.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @param domainId the domain to withdraw
   * @return no-content response
   */
  @DeleteMapping("/domains/{domainId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Withdraw one of the organization's domains")
  public ResponseEntity<Void> deleteDomain(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Domain ID") @PathVariable("domainId") final UUID domainId) {
    this.organizationApiService.deleteDomain(organizationRecord, domainId);
    return ResponseEntity.noContent().build();
  }
}
