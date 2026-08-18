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

package se.swedenconnect.oidf.registry.module.controller;

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
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ModuleDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.module.service.ModuleConfigService;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkWithSubjectsDto;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing federation modules (TrustAnchor, Resolver, Trustmark).
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/{tenant}/{orgNumber}/modules")
@Tag(name = "Modules", description = "CRUD for federation modules")
public class ModuleConfigController {

  private final ModuleConfigService moduleConfigService;

  /**
   * List all modules.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param type optional entity type filter
   * @param organizationRecord the organization record
   * @return the list of results
   */
  @GetMapping
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List all modules", description = "Lists all modules for the organization, "
      + "optionally filtered by type")
  public ResponseEntity<ModuleDto> listModules(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestParam(name = "type", required = false) final String type,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.listModules(organizationRecord, type));
  }

  // Trust Anchor

  /**
   * Create trust anchor with auto-generated ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param body the trust anchor data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/trust-anchor")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust anchor with auto-generated ID")
  public ResponseEntity<TrustAnchorDto> createTrustAnchor(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final TrustAnchorDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createTrustAnchor(organizationRecord, id, body));
  }

  /**
   * Create trust anchor with specified ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trust anchor ID
   * @param body the trust anchor data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/trust-anchor/{trustAnchorId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust anchor with specified ID")
  public ResponseEntity<TrustAnchorDto> createTrustAnchorWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustAnchorId") final UUID id,
      @RequestBody final TrustAnchorDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createTrustAnchor(organizationRecord, id, body));
  }

  /**
   * Update trust anchor.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trust anchor ID
   * @param body the trust anchor data
   * @param organizationRecord the organization record
   * @return the updated resource
   */
  @PutMapping("/trust-anchor/{trustAnchorId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update trust anchor")
  public ResponseEntity<TrustAnchorDto> updateTrustAnchor(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustAnchorId") final UUID id,
      @RequestBody final TrustAnchorDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateTrustAnchor(organizationRecord, id, body));
  }

  /**
   * Get trust anchor.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trust anchor ID
   * @param organizationRecord the organization record
   * @return the requested resource
   */
  @GetMapping("/trust-anchor/{trustAnchorId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get trust anchor")
  public ResponseEntity<TrustAnchorDto> getTrustAnchor(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustAnchorId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getTrustAnchor(organizationRecord, id));
  }

  /**
   * Delete trust anchor.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trust anchor ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/trust-anchor/{trustAnchorId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete trust anchor")
  public ResponseEntity<Void> deleteTrustAnchor(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustAnchorId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteTrustAnchor(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Intermediate

  /**
   * Create intermediate with auto-generated ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param body the intermediate data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/intermediate")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create intermediate with auto-generated ID")
  public ResponseEntity<IntermediateDto> createIntermediate(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final IntermediateDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createIntermediate(organizationRecord, id, body));
  }

  /**
   * Create intermediate with specified ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the intermediate ID
   * @param body the intermediate data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/intermediate/{intermediateId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create intermediate with specified ID")
  public ResponseEntity<IntermediateDto> createIntermediateWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("intermediateId") final UUID id,
      @RequestBody final IntermediateDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createIntermediate(organizationRecord, id, body));
  }

  /**
   * Update intermediate.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the intermediate ID
   * @param body the intermediate data
   * @param organizationRecord the organization record
   * @return the updated resource
   */
  @PutMapping("/intermediate/{intermediateId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update intermediate")
  public ResponseEntity<IntermediateDto> updateIntermediate(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("intermediateId") final UUID id,
      @RequestBody final IntermediateDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateIntermediate(organizationRecord, id, body));
  }

  /**
   * Get intermediate.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the intermediate ID
   * @param organizationRecord the organization record
   * @return the requested resource
   */
  @GetMapping("/intermediate/{intermediateId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get intermediate")
  public ResponseEntity<IntermediateDto> getIntermediate(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("intermediateId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getIntermediate(organizationRecord, id));
  }

  /**
   * Delete intermediate.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the intermediate ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/intermediate/{intermediateId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete intermediate")
  public ResponseEntity<Void> deleteIntermediate(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("intermediateId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteIntermediate(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Resolver

  /**
   * Create resolver with auto-generated ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param body the resolver data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/resolver")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create resolver with auto-generated ID")
  public ResponseEntity<ResolverDto> createResolver(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final ResolverDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createResolver(organizationRecord, id, body));
  }

  /**
   * Create resolver with specified ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the resolver ID
   * @param body the resolver data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/resolver/{resolverId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create resolver with specified ID")
  public ResponseEntity<ResolverDto> createResolverWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("resolverId") final UUID id,
      @RequestBody final ResolverDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createResolver(organizationRecord, id, body));
  }

  /**
   * Update resolver.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the resolver ID
   * @param body the resolver data
   * @param organizationRecord the organization record
   * @return the updated resource
   */
  @PutMapping("/resolver/{resolverId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update resolver")
  public ResponseEntity<ResolverDto> updateResolver(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("resolverId") final UUID id,
      @RequestBody final ResolverDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateResolver(organizationRecord, id, body));
  }

  /**
   * Get resolver.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the resolver ID
   * @param organizationRecord the organization record
   * @return the requested resource
   */
  @GetMapping("/resolver/{resolverId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get resolver")
  public ResponseEntity<ResolverDto> getResolver(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("resolverId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getResolver(organizationRecord, id));
  }

  /**
   * Delete resolver.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the resolver ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/resolver/{resolverId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete resolver")
  public ResponseEntity<Void> deleteResolver(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("resolverId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteResolver(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  // Trustmark Issuer

  /**
   * Create trust mark issuer with auto-generated ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param body the trustmark issuer data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/trustmark-issuer")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust mark issuer with auto-generated ID")
  public ResponseEntity<TrustmarkIssuerDto> createTrustmarkIssuer(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final TrustmarkIssuerDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.moduleConfigService.createTrustmarkIssuer(organizationRecord, id, body));
  }

  /**
   * Create trust mark issuer with specified ID.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trustmark issuer ID
   * @param body the trustmark issuer data
   * @param organizationRecord the organization record
   * @return the created resource
   */
  @PostMapping("/trustmark-issuer/{trustmarkIssuerId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create trust mark issuer with specified ID")
  public ResponseEntity<TrustmarkIssuerDto> createTrustmarkIssuerWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkIssuerId") final UUID id,
      @RequestBody final TrustmarkIssuerDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.createTrustmarkIssuer(organizationRecord, id, body));
  }

  /**
   * Update trust mark issuer.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trustmark issuer ID
   * @param body the trustmark issuer data
   * @param organizationRecord the organization record
   * @return the updated resource
   */
  @PutMapping("/trustmark-issuer/{trustmarkIssuerId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update trust mark issuer")
  public ResponseEntity<TrustmarkIssuerDto> updateTrustmarkIssuer(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkIssuerId") final UUID id,
      @RequestBody final TrustmarkIssuerDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.updateTrustmarkIssuer(organizationRecord, id, body));
  }

  /**
   * Get trust mark issuer.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trustmark issuer ID
   * @param organizationRecord the organization record
   * @return the requested resource
   */
  @GetMapping("/trustmark-issuer/{trustmarkIssuerId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get trust mark issuer")
  public ResponseEntity<TrustmarkIssuerDto> getTrustmarkIssuer(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkIssuerId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.moduleConfigService.getTrustmarkIssuer(organizationRecord, id));
  }

  /**
   * List trustmarks for a trust mark issuer.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param trustmarkIssuerId the trustmark issuer ID
   * @param organizationRecord the organization record
   * @return the list of results
   */
  @GetMapping("/trustmark-issuer/{trustmarkIssuerId}/trustmarks")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List trustmarks for a trust mark issuer")
  public ResponseEntity<List<TrustmarkWithSubjectsDto>> getTrustmarks(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkIssuerId") final UUID trustmarkIssuerId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.moduleConfigService.listTrustmarks(organizationRecord, trustmarkIssuerId, false));
  }

  /**
   * Delete trust mark issuer.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param id the trustmark issuer ID
   * @param organizationRecord the organization record
   * @return empty response
   */
  @DeleteMapping("/trustmark-issuer/{trustmarkIssuerId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete trust mark issuer")
  public ResponseEntity<Void> deleteTrustmarkIssuer(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkIssuerId") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.moduleConfigService.deleteTrustmarkIssuer(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}
