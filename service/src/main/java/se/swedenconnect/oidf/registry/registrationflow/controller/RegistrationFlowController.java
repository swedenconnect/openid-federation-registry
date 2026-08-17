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
package se.swedenconnect.oidf.registry.registrationflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrationflow.dto.AssignFlowRequest;
import se.swedenconnect.oidf.registry.registrationflow.dto.AssignFlowResponse;
import se.swedenconnect.oidf.registry.registrationflow.dto.FlowSummaryDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.IntermediateFlowAssignmentDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.StepDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.TrustMarkFlowAssignmentDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.TrustMarkIssuerFlowAssignmentDto;

import java.util.List;
import java.util.UUID;

/**
 * Exposes the configured registration flow pipeline.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequestMapping("/registration-flow/v1/{tenant}/{orgNumber}")
@Tag(name = "Registration Flow", description = "Configured pipeline steps for entity registration")
public class RegistrationFlowController {

  private final RegistrationFlowService registrationFlowService;

  public RegistrationFlowController(final RegistrationFlowService registrationFlowService) {
    this.registrationFlowService = registrationFlowService;
  }

  @GetMapping("/flows")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List all registration flows")
  public ResponseEntity<List<FlowSummaryDto>> listFlows(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.listFlows(organizationRecord));
  }

  @GetMapping("/steps")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List all configured pipeline steps with their settings")
  public ResponseEntity<List<StepDto>> getSteps(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber) {
    return ResponseEntity.ok(this.registrationFlowService.getDefineSteps());
  }

  @GetMapping("/flow/{flowId}")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get a registration flow by ID")
  public ResponseEntity<RegistrationFlowDto> getFlow(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("flowId") final UUID flowId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getRegistrationFlow(organizationRecord, flowId));
  }

  @PostMapping("/flow")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create a new flow")
  public ResponseEntity<RegistrationFlowDto> createFlow(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestBody final RegistrationFlowDto registrationFlowDto,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.createRegistrationFlow(
            organizationRecord, registrationFlowDto, UUID.randomUUID()));
  }

  @PostMapping("/flow/{flowid}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Create a new flow with a specified id")
  public ResponseEntity<RegistrationFlowDto> createFlowWithId(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("flowid") final UUID id,
      @RequestBody final RegistrationFlowDto registrationFlowDto,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.createRegistrationFlow(organizationRecord, registrationFlowDto, id));
  }

  @PutMapping("/flow/{flowid}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Update a registration flow")
  public ResponseEntity<RegistrationFlowDto> updateFlow(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("flowid") final UUID id,
      @RequestBody final RegistrationFlowDto registrationFlowDto,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.updateRegistrationFlow(organizationRecord, id, registrationFlowDto));
  }

  @DeleteMapping("/flow/{flowid}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Delete a registration flow")
  public ResponseEntity<Void> deleteFlow(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("flowid") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.deleteRegistrationFlow(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/intermediate/{taImId}/flows")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List flows assigned to an intermediate")
  public ResponseEntity<List<RegistrationFlowDto>> getFlowsForIntermediate(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("taImId") final UUID taImId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getFlowsForIntermediate(taImId));
  }

  @GetMapping("/intermediate/{taImId}/assignments")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List flow assignments for an intermediate (includes assignId)")
  public ResponseEntity<List<IntermediateFlowAssignmentDto>> getFlowAssignments(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("taImId") final UUID taImId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getFlowAssignmentsForIntermediate(taImId));
  }

  @PostMapping("/intermediate/{taImId}/assign")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Assign a flow to an intermediate")
  public ResponseEntity<AssignFlowResponse> assignFlow(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("taImId") final UUID taImId,
      @RequestBody final AssignFlowRequest request,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.registrationFlowService.assignFlow(taImId, request.flowId()));
  }

  @DeleteMapping("/intermediate/{taImId}/assign/{assignId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Remove a flow assignment from an intermediate")
  public ResponseEntity<Void> unassignFlow(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("taImId") final UUID taImId,
      @PathVariable("assignId") final UUID assignId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.unassignFlow(taImId, assignId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/trustmark-issuer/{tmIssuerId}/assignments")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List flow assignments for a trust mark issuer (includes assignId)")
  public ResponseEntity<List<TrustMarkIssuerFlowAssignmentDto>> getTrustMarkIssuerFlowAssignments(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getFlowAssignmentsForTrustMarkIssuer(tmIssuerId));
  }

  @PostMapping("/trustmark-issuer/{tmIssuerId}/assign")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Assign a flow to a trust mark issuer")
  public ResponseEntity<AssignFlowResponse> assignFlowToTrustMarkIssuer(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @RequestBody final AssignFlowRequest request,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.registrationFlowService.assignFlowToTrustMarkIssuer(tmIssuerId, request.flowId()));
  }

  @DeleteMapping("/trustmark-issuer/{tmIssuerId}/assign/{assignId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Remove a flow assignment from a trust mark issuer")
  public ResponseEntity<Void> unassignFlowFromTrustMarkIssuer(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @PathVariable("assignId") final UUID assignId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.unassignFlowFromTrustMarkIssuer(tmIssuerId, assignId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/trustmark-issuer/{tmIssuerId}/trustmark-assignments")
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List flow assignments for all trust marks under a trust mark issuer")
  public ResponseEntity<List<TrustMarkFlowAssignmentDto>> getTrustMarkFlowAssignments(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.getFlowAssignmentsForTrustMarkIssuerTrustmarks(tmIssuerId));
  }

  @PostMapping("/trustmark/{trustmarkId}/assign")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Assign a flow to a specific trust mark")
  public ResponseEntity<AssignFlowResponse> assignFlowToTrustMark(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkId") final UUID trustmarkId,
      @RequestBody final AssignFlowRequest request,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.registrationFlowService.assignFlowToTrustMark(trustmarkId, request.flowId()));
  }

  @DeleteMapping("/trustmark/{trustmarkId}/assign/{assignId}")
  @PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Remove a flow assignment from a specific trust mark")
  public ResponseEntity<Void> unassignFlowFromTrustMark(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @PathVariable("trustmarkId") final UUID trustmarkId,
      @PathVariable("assignId") final UUID assignId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.unassignFlowFromTrustMark(trustmarkId, assignId);
    return ResponseEntity.noContent().build();
  }
}
