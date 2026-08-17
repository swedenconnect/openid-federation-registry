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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationFlowInformationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.service.RegistrationService;

import java.util.List;
import java.util.UUID;

/**
 * Handles join applications — applying to and leaving the federation.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registration/v1")
@Tag(name = "FederationRegistration", description = "Apply to and manage federation membership")
public class FederationRegistrationController {

  private final RegistrationService registrationService;

  /**
   * Lists all registration records.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @return list of registration DTOs
   */
  @GetMapping("/{tenant}/{orgNumber}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List all registration records for current organization")
  public ResponseEntity<List<RegistrationDto>> listRegistrations(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationService.listRegistrationsForThisOrg(organizationRecord));
  }

  /**
   * Returns a single registration by ID.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @return the registration DTO
   */
  @GetMapping("/{tenant}/{orgNumber}/{registrationId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get a single registration by ID")
  public ResponseEntity<RegistrationDto> getById(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Registration ID") @PathVariable("registrationId") final UUID registrationId) {
    return ResponseEntity.ok(this.registrationService.getRegistrationById(organizationRecord, registrationId));
  }

  /**
   * Creates a join application with a specified ID.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param joinId the join ID to use
   * @param organizationRecord the calling organization
   * @param body the join request
   * @return the created join DTO
   */
  @PostMapping("/{tenant}/{orgNumber}/{joinId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create a registration request on this id")
  public ResponseEntity<RegistrationDto> createJoinWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("joinId") final UUID joinId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @RequestBody final RegistrationJoinRequestDto body) {
    return ResponseEntity.status(201)
        .body(this.registrationService.createRegistrationRequest(organizationRecord, joinId, body));
  }

  /**
   * Re-runs the registration flow for an existing entity.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param registrationId the existing registration ID
   * @param organizationRecord the calling organization
   * @param body the updated registration request
   * @return the updated registration DTO
   */
  @PutMapping("/{tenant}/{orgNumber}/{registrationId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Re-run the registration flow for an existing entity")
  public ResponseEntity<RegistrationDto> updateRegistration(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("registrationId") final UUID registrationId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @RequestBody final RegistrationJoinRequestDto body) {
    return ResponseEntity.ok(
        this.registrationService.updateRegistrationRequest(organizationRecord, registrationId, body));
  }

  /**
   * Removes a join record.
   *
   * @param tenant the tenant slug
   * @param orgNumber the calling organization's number
   * @param organizationRecord the calling organization
   * @param registrationId the ID of the registration to remove
   * @return no-content response
   */
  @DeleteMapping("/{tenant}/{orgNumber}/{registrationId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Remove a join record")
  public ResponseEntity<Void> deleteJoin(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "ID of the registration to remove")
      @PathVariable("registrationId") final UUID registrationId) {
    this.registrationService.deleteRegistrationRequest(organizationRecord, registrationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists all available registration flows. Open to any authenticated user — not scoped to a tenant or
   * organization, since an applicant browsing available flows may not yet belong to one.
   *
   * @return list of flow DTOs
   */
  @GetMapping("/flows")
  @Operation(summary = "List all available registration flows")
  public ResponseEntity<List<RegistrationFlowInformationDto>> listFlows() {
    return ResponseEntity.ok(this.registrationService.listRegistrationFlows());
  }
}
