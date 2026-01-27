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
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.service.SubordinateService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing subordinates using typed DTOs.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/subordinates")
@Tag(name = "Subordinates", description = "CRUD for subordinates")
public class SubordinateController {

  private final SubordinateService subordinateService;

  /**
   * Gets a subordinate by ID.
   *
   * @param id the subordinate ID
   * @param organizationRecord the organization record
   * @return the subordinate
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get subordinate by id")
  public ResponseEntity<SubordinateDto> getSubordinate(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.subordinateService.getSubordinate(organizationRecord, id));
  }

  /**
   * Creates a subordinate with auto-generated ID.
   *
   * @param body the subordinate data
   * @param organizationRecord the organization record
   * @return the created subordinate
   */
  @PostMapping("/")
  @Operation(summary = "Create subordinate with auto-generated ID")
  public ResponseEntity<SubordinateDto> createSubordinate(
      @RequestBody final SubordinateDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.subordinateService.createSubordinate(organizationRecord, body));
  }

  /**
   * Creates a subordinate with specified ID.
   *
   * @param id the subordinate ID
   * @param body the subordinate data
   * @param organizationRecord the organization record
   * @return the created subordinate
   */
  @PostMapping("/{id}")
  @Operation(summary = "Create subordinate with specified ID")
  public ResponseEntity<SubordinateDto> createSubordinateWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final SubordinateDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.subordinateService.createSubordinateWithId(organizationRecord, id, body));
  }

  /**
   * Updates a subordinate.
   *
   * @param id the subordinate ID
   * @param body the subordinate data
   * @param organizationRecord the organization record
   * @return the updated subordinate
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update subordinate")
  public ResponseEntity<SubordinateDto> updateSubordinate(
      @PathVariable("id") final UUID id,
      @RequestBody final SubordinateDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.subordinateService.updateSubordinate(organizationRecord, id, body));
  }

  /**
   * Deletes a subordinate.
   *
   * @param id the subordinate ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete subordinate")
  public ResponseEntity<Void> deleteSubordinate(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.subordinateService.deleteSubordinate(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}

