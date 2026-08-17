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

package se.swedenconnect.oidf.registry.trustmark.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.module.service.ModuleConfigService;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkWithSubjectsDto;
import se.swedenconnect.oidf.registry.trustmark.service.TrustmarkSubjectService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing trust marks and trust mark subjects.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/{tenant}/{orgNumber}/trustmarks")
@Tag(name = "Trustmarks", description = "CRUD for trustmarks and trust mark subjects")
public class TrustmarkController {

  private final ModuleConfigService moduleConfigService;
  private final TrustmarkSubjectService trustmarkSubjectService;

  @GetMapping
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List all trustmarks", description = "Lists all trustmarks for the organization, "
      + "optionally including trustmark subjects")
  public ResponseEntity<List<TrustmarkWithSubjectsDto>> listTrustmarks(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestParam(name = "includeSubjects", required = false, defaultValue = "false") final boolean includeSubjects,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.moduleConfigService.listTrustmarks(organizationRecord, null, includeSubjects));
  }

  // Trustmark

  @PostMapping
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust mark with auto-generated ID")
  public ResponseEntity<TrustmarkDto> createTrustmark(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final TrustmarkDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createTrustmark(organizationRecord, id, body));
  }

  @PostMapping("/{trustMarkId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust mark with specified ID")
  public ResponseEntity<TrustmarkDto> createTrustmarkWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustMarkId") final UUID id,
      @RequestBody final TrustmarkDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createTrustmark(organizationRecord, id, body));
  }

  @PutMapping("/{trustMarkId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update trust mark")
  public ResponseEntity<TrustmarkDto> updateTrustmark(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustMarkId") final UUID id,
      @RequestBody final TrustmarkDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateTrustmark(organizationRecord, id, body));
  }

  @GetMapping("/{trustMarkId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get trust mark")
  public ResponseEntity<TrustmarkDto> getTrustmark(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustMarkId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getTrustmark(organizationRecord, id));
  }

  @GetMapping("/{trustMarkId}/subjects")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get trust mark with their subjects")
  public ResponseEntity<TrustmarkWithSubjectsDto> getTrustmarkSubjects(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustMarkId") final UUID trustmarkId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getTrustmarkWithSubjects(organizationRecord, trustmarkId));
  }

  @DeleteMapping("/{trustMarkId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete trust mark")
  public ResponseEntity<Void> deleteTrustmark(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustMarkId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteTrustmark(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Trustmark Subject

  @PostMapping("/subjects")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust mark subject with auto-generated ID")
  public ResponseEntity<TrustmarkSubjectDto> createTrustmarkSubject(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final TrustmarkSubjectDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.trustmarkSubjectService.createTrustmarkSubject(organizationRecord, id, body));
  }

  @PostMapping("/subjects/{subjectId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust mark subject with specified ID")
  public ResponseEntity<TrustmarkSubjectDto> createTrustmarkSubjectWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("subjectId") final UUID id,
      @RequestBody final TrustmarkSubjectDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.createTrustmarkSubject(organizationRecord, id, body));
  }

  @PutMapping("/subjects/{subjectId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update trust mark subject")
  public ResponseEntity<TrustmarkSubjectDto> updateTrustmarkSubject(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("subjectId") final UUID id,
      @RequestBody final TrustmarkSubjectDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.updateTrustmarkSubject(organizationRecord, id, body));
  }

  @GetMapping("/subjects/{subjectId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get trust mark subject")
  public ResponseEntity<TrustmarkSubjectDto> getTrustmarkSubject(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("subjectId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.getTrustmarkSubject(organizationRecord, id));
  }

  @DeleteMapping("/subjects/{subjectId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete trust mark subject")
  public ResponseEntity<Void> deleteTrustmarkSubject(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("subjectId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.trustmarkSubjectService.deleteTrustmarkSubject(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}
