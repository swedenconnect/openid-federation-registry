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
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.ResolverDto;
import se.swedenconnect.oidf.registry.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.service.ModuleConfigService;

import java.util.UUID;

/**
 * REST controller for managing federation modules (TrustAnchor, Resolver, Trustmark).
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/modules")
@Tag(name = "Modules", description = "CRUD for federation modules")
public class ModuleConfigController {

  private final ModuleConfigService moduleConfigService;

  // TrustAnchor

  /**
   * Creates a trust anchor with auto-generated ID.
   *
   * @param body the trust anchor data
   * @param organizationRecord the organization record
   * @return the created trust anchor
   */
  @PostMapping("/trust-anchor")
  @Operation(summary = "Create trust anchor with auto-generated ID")
  public ResponseEntity<TrustAnchorDto> createTrustAnchor(
      @RequestBody final TrustAnchorDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createTrustAnchor(organizationRecord, id, body));
  }

  /**
   * Creates a trust anchor with specified ID.
   *
   * @param id the trust anchor ID
   * @param body the trust anchor data
   * @param organizationRecord the organization record
   * @return the created trust anchor
   */
  @PostMapping("/trust-anchor/{id}")
  @Operation(summary = "Create trust anchor with specified ID")
  public ResponseEntity<TrustAnchorDto> createTrustAnchorWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustAnchorDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createTrustAnchor(organizationRecord, id, body));
  }

  /**
   * Updates a trust anchor.
   *
   * @param id the trust anchor ID
   * @param body the trust anchor data
   * @param organizationRecord the organization record
   * @return the updated trust anchor
   */
  @PutMapping("/trust-anchor/{id}")
  @Operation(summary = "Update trust anchor")
  public ResponseEntity<TrustAnchorDto> updateTrustAnchor(
      @PathVariable("id") final UUID id,
      @RequestBody final TrustAnchorDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateTrustAnchor(organizationRecord, id, body));
  }

  /**
   * Gets a trust anchor by ID.
   *
   * @param id the trust anchor ID
   * @param organizationRecord the organization record
   * @return the trust anchor
   */
  @GetMapping("/trust-anchor/{id}")
  @Operation(summary = "Get trust anchor")
  public ResponseEntity<TrustAnchorDto> getTrustAnchor(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getTrustAnchor(organizationRecord, id));
  }

  /**
   * Deletes a trust anchor.
   *
   * @param id the trust anchor ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/trust-anchor/{id}")
  @Operation(summary = "Delete trust anchor")
  public ResponseEntity<Void> deleteTrustAnchor(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteTrustAnchor(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Resolver

  /**
   * Creates a resolver with auto-generated ID.
   *
   * @param body the resolver data
   * @param organizationRecord the organization record
   * @return the created resolver
   */
  @PostMapping("/resolver")
  @Operation(summary = "Create resolver with auto-generated ID")
  public ResponseEntity<ResolverDto> createResolver(
      @RequestBody final ResolverDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createResolver(organizationRecord, id, body));
  }

  /**
   * Creates a resolver with specified ID.
   *
   * @param id the resolver ID
   * @param body the resolver data
   * @param organizationRecord the organization record
   * @return the created resolver
   */
  @PostMapping("/resolver/{id}")
  @Operation(summary = "Create resolver with specified ID")
  public ResponseEntity<ResolverDto> createResolverWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final ResolverDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createResolver(organizationRecord, id, body));
  }

  /**
   * Updates a resolver.
   *
   * @param id the resolver ID
   * @param body the resolver data
   * @param organizationRecord the organization record
   * @return the updated resolver
   */
  @PutMapping("/resolver/{id}")
  @Operation(summary = "Update resolver")
  public ResponseEntity<ResolverDto> updateResolver(
      @PathVariable("id") final UUID id,
      @RequestBody final ResolverDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateResolver(organizationRecord, id, body));
  }

  /**
   * Gets a resolver by ID.
   *
   * @param id the resolver ID
   * @param organizationRecord the organization record
   * @return the resolver
   */
  @GetMapping("/resolver/{id}")
  @Operation(summary = "Get resolver")
  public ResponseEntity<ResolverDto> getResolver(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getResolver(organizationRecord, id));
  }

  /**
   * Deletes a resolver.
   *
   * @param id the resolver ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/resolver/{id}")
  @Operation(summary = "Delete resolver")
  public ResponseEntity<Void> deleteResolver(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteResolver(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Trustmark

  /**
   * Creates a trust mark with auto-generated ID.
   *
   * @param body the trust mark data
   * @param organizationRecord the organization record
   * @return the created trust mark
   */
  @PostMapping("/trustmark")
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
  @PostMapping("/trustmark/{id}")
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
  @PutMapping("/trustmark/{id}")
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
  @GetMapping("/trustmark/{id}")
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
  @DeleteMapping("/trustmark/{id}")
  @Operation(summary = "Delete trust mark")
  public ResponseEntity<Void> deleteTrustmark(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteTrustmark(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}


