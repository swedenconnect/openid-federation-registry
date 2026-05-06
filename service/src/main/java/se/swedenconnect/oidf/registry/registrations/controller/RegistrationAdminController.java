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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RejectRegistrationDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.time.LocalDateTime;
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
@RequestMapping("/registration-admin/v1/")
@Tag(name = "RegistrationAdmin", description = "Operator view of incoming registration requests")
public class RegistrationAdminController {

  private final RegistrationRepository registrationRepository;

  /**
   * Lists pending registrations for an intermediate.
   *
   * @param organizationRecord the calling organization
   * @param taimId the intermediate ID to filter by
   * @return list of pending registration DTOs
   */
  @GetMapping
  @Operation(summary = "List pending registrations for an intermediate")
  public ResponseEntity<List<RegistrationDto>> listPending(
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Filter by intermediate ID") @RequestParam final UUID taimId) {
    final List<RegistrationDto> result = this.registrationRepository
        .findByFlowAssignment_TaIm_TaImIdAndStatus(taimId, RegistrationStatus.PENDING)
        .stream()
        .map(RegistrationAdminController::toDto)
        .toList();
    return ResponseEntity.ok(result);
  }

  /**
   * Counts unhandled PENDING registrations for an intermediate.
   *
   * @param organizationRecord the calling organization
   * @param taimId the intermediate ID
   * @return map containing the count
   */
  @GetMapping("/count")
  @Operation(summary = "Count unhandled PENDING registrations for an intermediate (badge)")
  public ResponseEntity<Map<String, Long>> countPending(
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Intermediate ID") @RequestParam final UUID taimId) {
    final long count = this.registrationRepository
        .countByFlowAssignment_TaIm_TaImIdAndStatus(taimId, RegistrationStatus.PENDING);
    return ResponseEntity.ok(Map.of("count", count));
  }

  /**
   * Rejects a pending registration request.
   *
   * @param organizationRecord the calling organization
   * @param id the registration ID
   * @param body the rejection details
   * @return the updated registration DTO
   */
  @PostMapping("/{id}/reject")
  @Operation(summary = "Reject a pending registration request")
  public ResponseEntity<RegistrationDto> reject(
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Registration ID") @PathVariable("id") final UUID id,
      @RequestBody final RejectRegistrationDto body) {
    final Registration reg = this.registrationRepository.findById(id)
        .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Registration not found: " + id));
    if (reg.getStatus() != RegistrationStatus.PENDING) {
      return ResponseEntity.unprocessableEntity().build();
    }
    reg.setStatus(RegistrationStatus.REJECTED);
    reg.setRejectionReason(RegistrationStatus.valueOf(body.rejectionReason()));
    reg.setReviewedAt(LocalDateTime.now());
    this.registrationRepository.save(reg);
    return ResponseEntity.ok(toDto(reg));
  }

  private static RegistrationDto toDto(final Registration reg) {
    final RegistrationDto dto = new RegistrationDto();
    dto.setEntityId(reg.getEntityId());
    dto.setIntermediateEntityId(reg.getFlowAssignment().getTaIm().getEntity().getSubject());
    dto.setRejectionReason(reg.getRejectionReason().toString());
    return dto;
  }
}