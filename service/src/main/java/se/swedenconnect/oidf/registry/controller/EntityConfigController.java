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
import se.swedenconnect.oidf.registry.api.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.api.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.api.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.service.EntityConfigService;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

import java.util.UUID;

/**
 * REST controller for managing Federation, Hosted and Subordinate entities using typed DTOs.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/entities")
@Tag(name = "Entities", description = "CRUD for federation, hosted and subordinate entities")
public class EntityConfigController {

  private final EntityConfigService entityConfigService;

  @PostMapping("/federation")
  @Operation(summary = "Create federation entity with auto-generated ID")
  public ResponseEntity<FederationEntityDto> createFederationEntity(
      @RequestBody final FederationEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.entityConfigService.createFederationEntity(organizationRecord, id, body));
  }

  @PostMapping("/federation/{id}")
  @Operation(summary = "Create federation entity with specified ID")
  public ResponseEntity<FederationEntityDto> createFederationEntityWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final FederationEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.entityConfigService.createFederationEntity(organizationRecord, id, body));
  }

  @PutMapping("/federation/{id}")
  @Operation(summary = "Update federation entity")
  public ResponseEntity<FederationEntityDto> updateFederationEntity(
      @PathVariable("id") final UUID id,
      @RequestBody final FederationEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.entityConfigService.updateFederationEntity(organizationRecord, id, body));
  }

  @GetMapping("/federation/{id}")
  @Operation(summary = "Get federation entity")
  public ResponseEntity<FederationEntityDto> getFederationEntity(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.getFederationEntity(organizationRecord, id));
  }

  @DeleteMapping("/federation/{id}")
  @Operation(summary = "Delete federation entity")
  public ResponseEntity<Void> deleteFederationEntity(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.entityConfigService.deleteFederationEntity(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/hosted")
  @Operation(summary = "Create hosted entity with auto-generated ID")
  public ResponseEntity<HostedEntityDto> createHostedEntity(
      @RequestBody final HostedEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.entityConfigService.createHostedEntity(organizationRecord, id, body));
  }

  @PostMapping("/hosted/{id}")
  @Operation(summary = "Create hosted entity with specified ID")
  public ResponseEntity<HostedEntityDto> createHostedEntityWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final HostedEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.entityConfigService.createHostedEntity(organizationRecord, id, body));
  }

  @PutMapping("/hosted/{id}")
  @Operation(summary = "Update hosted entity")
  public ResponseEntity<HostedEntityDto> updateHostedEntity(
      @PathVariable("id") final UUID id,
      @RequestBody final HostedEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.entityConfigService.updateHostedEntity(organizationRecord, id, body));
  }

  @GetMapping("/hosted/{id}")
  @Operation(summary = "Get hosted entity")
  public ResponseEntity<HostedEntityDto> getHostedEntity(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.getHostedEntity(organizationRecord, id));
  }

  @DeleteMapping("/hosted/{id}")
  @Operation(summary = "Delete hosted entity")
  public ResponseEntity<Void> deleteHostedEntity(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.entityConfigService.deleteHostedEntity(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/subordinate")
  @Operation(summary = "Create subordinate entity with auto-generated ID")
  public ResponseEntity<SubordinateEntityDto> createSubordinateEntity(
      @RequestBody final SubordinateEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.entityConfigService.createSubordinateEntity(organizationRecord, id, body));
  }

  @PostMapping("/subordinate/{id}")
  @Operation(summary = "Create subordinate entity with specified ID")
  public ResponseEntity<SubordinateEntityDto> createSubordinateEntityWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final SubordinateEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.entityConfigService.createSubordinateEntity(organizationRecord, id, body));
  }

  @PutMapping("/subordinate/{id}")
  @Operation(summary = "Update subordinate entity")
  public ResponseEntity<SubordinateEntityDto> updateSubordinateEntity(
      @PathVariable("id") final UUID id,
      @RequestBody final SubordinateEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.entityConfigService.updateSubordinateEntity(organizationRecord, id, body));
  }

  @GetMapping("/subordinate/{id}")
  @Operation(summary = "Get subordinate entity")
  public ResponseEntity<SubordinateEntityDto> getSubordinateEntity(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.getSubordinateEntity(organizationRecord, id));
  }

  @DeleteMapping("/subordinate/{id}")
  @Operation(summary = "Delete subordinate entity")
  public ResponseEntity<Void> deleteSubordinateEntity(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.entityConfigService.deleteSubordinateEntity(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}


