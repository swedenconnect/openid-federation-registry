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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkWithSubjectsDto;
import se.swedenconnect.oidf.registry.service.ModuleConfigService;
import se.swedenconnect.oidf.registry.service.TrustmarkSubjectService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing trust marks and trust mark subjects.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/trustmarks")
@Tag(name = "Trustmarks", description = "CRUD for trustmarks and trust mark subjects")
public class TrustmarkController {

  private final ModuleConfigService moduleConfigService;
  private final TrustmarkSubjectService trustmarkSubjectService;

  /**
   * Lists all trustmarks for the organization.
   *
   * @param includeSubjects if true, includes trustmark subjects in the response
   * @param organizationRecord the organization record
   * @return list of trustmarks with optionally included trustmark subjects
   */
  @GetMapping
  @Operation(summary = "List all trustmarks", description = "Lists all trustmarks for the organization, "
      + "optionally including trustmark subjects")
  public ResponseEntity<List<TrustmarkWithSubjectsDto>> listTrustmarks(
      @RequestParam(name = "includeSubjects", required = false, defaultValue = "false") final boolean includeSubjects,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.listTrustmarks(organizationRecord, includeSubjects));
  }

  // Trustmark

  /**
   * Creates a trust mark with auto-generated ID.
   *
   * @param body the trust mark data
   * @param organizationRecord the organization record
   * @return the created trust mark
   */
  @PostMapping
  @Operation(summary = "Create trust mark with auto-generated ID")
  public ResponseEntity<TrustmarkDto> createTrustmark(
      @RequestBody final TrustmarkDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createTrustmark(organizationRecord, id, body));
  }

  /**
   * Creates a trust mark with specified ID.
   *
   * @param id the trust mark ID
   * @param body the trust mark data
   * @param organizationRecord the organization record
   * @return the created trust mark
   */
  @PostMapping("/{id}")
  @Operation(summary = "Create trust mark with specified ID")
  public ResponseEntity<TrustmarkDto> createTrustmarkWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustmarkDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createTrustmark(organizationRecord, id, body));
  }

  /**
   * Updates a trust mark.
   *
   * @param id the trust mark ID
   * @param body the trust mark data
   * @param organizationRecord the organization record
   * @return the updated trust mark
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update trust mark")
  public ResponseEntity<TrustmarkDto> updateTrustmark(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustmarkDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateTrustmark(organizationRecord, id, body));
  }

  /**
   * Gets a trust mark by ID.
   *
   * @param id the trust mark ID
   * @param organizationRecord the organization record
   * @return the trust mark
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get trust mark")
  public ResponseEntity<TrustmarkDto> getTrustmark(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getTrustmark(organizationRecord, id));
  }

  /**
   * Deletes a trust mark.
   *
   * @param id the trust mark ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete trust mark")
  public ResponseEntity<Void> deleteTrustmark(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteTrustmark(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Trustmark Subject

  /**
   * Creates a trust mark subject with auto-generated ID.
   *
   * @param body the trust mark subject data
   * @param organizationRecord the organization record
   * @return the created trust mark subject
   */
  @PostMapping("/subjects")
  @Operation(summary = "Create trust mark subject with auto-generated ID")
  public ResponseEntity<TrustmarkSubjectDto> createTrustmarkSubject(
      @RequestBody final TrustmarkSubjectDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.trustmarkSubjectService.createTrustmarkSubject(organizationRecord, id, body));
  }

  /**
   * Creates a trust mark subject with specified ID.
   *
   * @param id the trust mark subject ID
   * @param body the trust mark subject data
   * @param organizationRecord the organization record
   * @return the created trust mark subject
   */
  @PostMapping("/subjects/{id}")
  @Operation(summary = "Create trust mark subject with specified ID")
  public ResponseEntity<TrustmarkSubjectDto> createTrustmarkSubjectWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustmarkSubjectDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.createTrustmarkSubject(organizationRecord, id, body));
  }

  /**
   * Updates a trust mark subject.
   *
   * @param id the trust mark subject ID
   * @param body the trust mark subject data
   * @param organizationRecord the organization record
   * @return the updated trust mark subject
   */
  @PutMapping("/subjects/{id}")
  @Operation(summary = "Update trust mark subject")
  public ResponseEntity<TrustmarkSubjectDto> updateTrustmarkSubject(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustmarkSubjectDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.updateTrustmarkSubject(organizationRecord, id, body));
  }

  /**
   * Gets a trust mark subject by ID.
   *
   * @param id the trust mark subject ID
   * @param organizationRecord the organization record
   * @return the trust mark subject
   */
  @GetMapping("/subjects/{id}")
  @Operation(summary = "Get trust mark subject")
  public ResponseEntity<TrustmarkSubjectDto> getTrustmarkSubject(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.trustmarkSubjectService.getTrustmarkSubject(organizationRecord, id));
  }

  /**
   * Deletes a trust mark subject.
   *
   * @param id the trust mark subject ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/subjects/{id}")
  @Operation(summary = "Delete trust mark subject")
  public ResponseEntity<Void> deleteTrustmarkSubject(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.trustmarkSubjectService.deleteTrustmarkSubject(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}
