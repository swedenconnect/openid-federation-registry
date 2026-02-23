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

package se.swedenconnect.oidf.registry.entity.controller;

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
import se.swedenconnect.oidf.registry.entity.dto.EntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;

import java.util.List;
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

  /**
   * Lists all entities for the organization.
   *
   * @param type optional entity type filter (federation, hosted, subordinate)
   * @param includeModules whether to include modules (trustanchor, intermediate, resolver, trustmarkissuer)
   * @param organizationRecord the organization record
   * @return list of entities with optional modules
   */
  @GetMapping
  @Operation(summary = "List all entities",
      description = "Lists all entities for the organization, optionally filtered by type and with modules included")
  public ResponseEntity<EntityWithModulesDto> listEntities(
      @RequestParam(name = "type", required = false) final String type,
      @RequestParam(name = "includemodules", defaultValue = "false") final boolean includeModules,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.listEntities(organizationRecord, type, includeModules));
  }

  /**
   * Creates a federation entity with auto-generated ID.
   *
   * @param body the federation entity data
   * @param organizationRecord the organization record
   * @return the created federation entity
   */
  @PostMapping("/federation")
  @Operation(summary = "Create federation entity with auto-generated ID")
  public ResponseEntity<FederationEntityDto> createFederationEntity(
      @RequestBody final FederationEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.entityConfigService.createFederationEntity(organizationRecord, id, body));
  }

  /**
   * Creates a federation entity with specified ID.
   *
   * @param id the federation entity ID
   * @param body the federation entity data
   * @param organizationRecord the organization record
   * @return the created federation entity
   */
  @PostMapping("/federation/{entityId}")
  @Operation(summary = "Create federation entity with specified ID")
  public ResponseEntity<FederationEntityDto> createFederationEntityWithId(
      @PathVariable("entityId") final UUID id,
      @RequestBody final FederationEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.createFederationEntity(organizationRecord, id, body));
  }

  /**
   * Updates a federation entity.
   *
   * @param id the federation entity ID
   * @param body the federation entity data
   * @param organizationRecord the organization record
   * @return the updated federation entity
   */
  @PutMapping("/federation/{entityId}")
  @Operation(summary = "Update federation entity")
  public ResponseEntity<FederationEntityDto> updateFederationEntity(
      @PathVariable("entityId") final UUID id,
      @RequestBody final FederationEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.updateFederationEntity(organizationRecord, id, body));
  }

  /**
   * Retrieves a federation entity by its unique identifier. Optionally includes associated modules
   * based on the provided parameter.
   *
   * @param id the unique identifier of the federation entity
   * @param includeModules a flag indicating whether to include associated modules in the response
   * @param organizationRecord the organization context for the request, typically populated internally
   * @return a ResponseEntity containing the federation entity along with its modules if requested
   */
  @GetMapping("/federation/{entityId}")
  @Operation(summary = "Get federation entity")
  public ResponseEntity<FederationEntityWithModulesDto> getFederationEntity(
      @PathVariable("entityId") final UUID id,
      @RequestParam(name = "includemodules", defaultValue = "false") final boolean includeModules,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.getFederationEntity(organizationRecord, id, includeModules));
  }

  /**
   * Deletes a federation entity.
   *
   * @param id the federation entity ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/federation/{entityId}")
  @Operation(summary = "Delete federation entity")
  public ResponseEntity<Void> deleteFederationEntity(
      @PathVariable("entityId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.entityConfigService.deleteFederationEntity(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Creates a hosted entity with auto-generated ID.
   *
   * @param body the hosted entity data
   * @param organizationRecord the organization record
   * @return the created hosted entity
   */
  @PostMapping("/hosted")
  @Operation(summary = "Create hosted entity with auto-generated ID")
  public ResponseEntity<HostedEntityDto> createHostedEntity(
      @RequestBody final HostedEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.entityConfigService.createHostedEntity(organizationRecord, id, body));
  }

  /**
   * Creates a hosted entity with specified ID.
   *
   * @param id the hosted entity ID
   * @param body the hosted entity data
   * @param organizationRecord the organization record
   * @return the created hosted entity
   */
  @PostMapping("/hosted/{entityId}")
  @Operation(summary = "Create hosted entity with specified ID")
  public ResponseEntity<HostedEntityDto> createHostedEntityWithId(
      @PathVariable("entityId") final UUID id,
      @RequestBody final HostedEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.createHostedEntity(organizationRecord, id, body));
  }

  /**
   * Updates a hosted entity.
   *
   * @param id the hosted entity ID
   * @param body the hosted entity data
   * @param organizationRecord the organization record
   * @return the updated hosted entity
   */
  @PutMapping("/hosted/{entityId}")
  @Operation(summary = "Update hosted entity")
  public ResponseEntity<HostedEntityDto> updateHostedEntity(
      @PathVariable("entityId") final UUID id,
      @RequestBody final HostedEntityDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.updateHostedEntity(organizationRecord, id, body));
  }

  /**
   * Gets a hosted entity by ID.
   *
   * @param id the hosted entity ID
   * @param organizationRecord the organization record
   * @return the hosted entity
   */
  @GetMapping("/hosted/{entityId}")
  @Operation(summary = "Get hosted entity")
  public ResponseEntity<HostedEntityDto> getHostedEntity(
      @PathVariable("entityId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.getHostedEntity(organizationRecord, id));
  }

  /**
   * Gets a hosted entity by ID.
   *
   * @param entityIdentifier the hosted entity ID
   * @param organizationRecord the organization record
   * @return the hosted entity
   */
  @GetMapping("/hosted")
  @Operation(summary = "List hosted entity")
  public ResponseEntity<List<HostedEntityDto>> listHostedEntity(
      @RequestParam(name = "entityIdentifier", required = false) final String entityIdentifier,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.entityConfigService.listHostedEntity(organizationRecord, entityIdentifier));
  }

  /**
   * Deletes a hosted entity.
   *
   * @param id the hosted entity ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/hosted/{entityId}")
  @Operation(summary = "Delete hosted entity")
  public ResponseEntity<Void> deleteHostedEntity(
      @PathVariable("entityId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.entityConfigService.deleteHostedEntity(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

}
