/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.registry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.api.dto.input.TrustmarkSubjectInputDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.service.TrustmarkSubjectService;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

import java.util.UUID;

/**
 * REST controller for managing trust mark subjects.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/trustmark-subjects")
@Tag(name = "TrustmarkSubjects", description = "CRUD for trust mark subjects")
public class TrustmarkSubjectController {

  private final TrustmarkSubjectService trustmarkSubjectService;

  @PostMapping
  @Operation(summary = "Create trust mark subject with auto-generated ID")
  public ResponseEntity<TrustmarkSubjectDto> createTrustmarkSubject(
      @RequestBody final TrustmarkSubjectInputDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.trustmarkSubjectService.createTrustmarkSubject(organizationRecord, id, body));
  }

  @PostMapping("/{id}")
  @Operation(summary = "Create trust mark subject with specified ID")
  public ResponseEntity<TrustmarkSubjectDto> createTrustmarkSubjectWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustmarkSubjectInputDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.trustmarkSubjectService.createTrustmarkSubject(organizationRecord, id, body));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update trust mark subject")
  public ResponseEntity<TrustmarkSubjectDto> updateTrustmarkSubject(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustmarkSubjectInputDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.trustmarkSubjectService.updateTrustmarkSubject(organizationRecord, id, body));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get trust mark subject")
  public ResponseEntity<TrustmarkSubjectDto> getTrustmarkSubject(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.getTrustmarkSubject(organizationRecord, id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete trust mark subject")
  public ResponseEntity<Void> deleteTrustmarkSubject(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.trustmarkSubjectService.deleteTrustmarkSubject(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}


