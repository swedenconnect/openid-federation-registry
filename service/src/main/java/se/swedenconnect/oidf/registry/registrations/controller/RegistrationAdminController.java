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
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
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
@RequestMapping("/registration/admin")
@Tag(name = "Pending registrations", description = "Operator view of incoming registration requests")
public class RegistrationAdminController {

  private final RegistrationRepository registrationRepository;

  @GetMapping
  @Operation(summary = "List pending registrations for an intermediate")
  public ResponseEntity<List<RegistrationDto>> listPending(
      @Parameter(description = "Filter by intermediate ID") @RequestParam final UUID taimId) {
    final List<RegistrationDto> result = this.registrationRepository
        .findByTaIm_TaImIdAndStatus(taimId, RegistrationStatus.PENDING)
        .stream()
        .map(RegistrationAdminController::toDto)
        .toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/count")
  @Operation(summary = "Count unhandled PENDING registrations for an intermediate (badge)")
  public ResponseEntity<Map<String, Long>> countPending(
      @Parameter(description = "Intermediate ID") @RequestParam final UUID taimId) {
    final long count = this.registrationRepository.countByTaIm_TaImIdAndStatus(taimId, RegistrationStatus.PENDING);
    return ResponseEntity.ok(Map.of("count", count));
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Reject a pending registration request")
  public ResponseEntity<RegistrationDto> reject(
      @Parameter(description = "Registration ID") @PathVariable final UUID id,
      @RequestBody final Map<String, String> body) {
    final Registration reg = this.registrationRepository.findById(id)
        .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Registration not found: " + id));
    if (reg.getStatus() != RegistrationStatus.PENDING) {
      return ResponseEntity.unprocessableEntity().build();
    }
    reg.setStatus(RegistrationStatus.REJECTED);
    reg.setRejectionReason(body.get("rejectionReason"));
    reg.setReviewedAt(LocalDateTime.now());
    this.registrationRepository.save(reg);
    return ResponseEntity.ok(toDto(reg));
  }

  private static RegistrationDto toDto(final Registration reg) {
    final RegistrationDto dto = new RegistrationDto();
    dto.setId(reg.getId());
    dto.setTaimId(reg.getTaIm().getTaImId());
    dto.setRegistrationFlowId(reg.getRegistrationFlow().getFlowId());
    dto.setEntityId(reg.getEntityId());
    dto.setJwks(reg.getJwks());
    dto.setMetadata(reg.getMetadata());
    dto.setMetadataPolicy(reg.getMetadataPolicy());
    dto.setTrustmarksRequested(reg.getTrustmarksRequested());
    dto.setStatus(reg.getStatus());
    dto.setReviewedAt(reg.getReviewedAt());
    dto.setReviewedBy(reg.getReviewedBy());
    dto.setRejectionReason(reg.getRejectionReason());
    dto.setCreatedDate(reg.getCreatedDate());
    return dto;
  }
}