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
@RequestMapping("/registration-flow/v1")
@Tag(name = "Registration Flow", description = "Configured pipeline steps for entity registration")
public class RegistrationFlowController {

  private final RegistrationFlowService registrationFlowService;

  /**
   * Constructs the controller.
   *
   * @param registrationFlowService the service handling flow operations
   */
  public RegistrationFlowController(final RegistrationFlowService registrationFlowService) {
    this.registrationFlowService = registrationFlowService;
  }

  /**
   * Lists all registration flows owned by the calling organization.
   *
   * @param organizationRecord the calling organization
   * @return list of flow summaries
   */
  @GetMapping("/flows")
  @Operation(summary = "List all registration flows")
  public ResponseEntity<List<FlowSummaryDto>> listFlows(
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.listFlows(organizationRecord));
  }

  /**
   * Lists all configured pipeline steps with their settings.
   *
   * @return list of step DTOs
   */
  @GetMapping("/steps")
  @Operation(summary = "List all configured pipeline steps with their settings")
  public ResponseEntity<List<StepDto>> getSteps() {
    return ResponseEntity.ok(this.registrationFlowService.getDefineSteps());
  }

  /**
   * Returns a single registration flow by ID. The flow must belong to the calling organization.
   *
   * @param flowId the flow ID
   * @param organizationRecord the calling organization
   * @return the flow DTO
   */
  @GetMapping("/flow/{flowId}")
  @Operation(summary = "Get a registration flow by ID")
  public ResponseEntity<RegistrationFlowDto> getFlow(
      @PathVariable("flowId") final UUID flowId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getRegistrationFlow(organizationRecord, flowId));
  }

  /**
   * Creates a new registration flow with an auto-generated ID.
   *
   * @param registrationFlowDto the flow definition
   * @param organizationRecord the calling organization
   * @return the created flow DTO
   */
  @PostMapping("/flow")
  @Operation(summary = "Create a new flow")
  public ResponseEntity<RegistrationFlowDto> createFlow(
      @RequestBody final RegistrationFlowDto registrationFlowDto,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.createRegistrationFlow(
            organizationRecord, registrationFlowDto, UUID.randomUUID()));
  }

  /**
   * Creates a new registration flow with a specified ID.
   *
   * @param id the flow ID to use
   * @param registrationFlowDto the flow definition
   * @param organizationRecord the calling organization
   * @return the created flow DTO
   */
  @PostMapping("/flow/{flowid}")
  @Operation(summary = "Create a new flow with a specified id")
  public ResponseEntity<RegistrationFlowDto> createFlowWithId(
      @PathVariable("flowid") final UUID id,
      @RequestBody final RegistrationFlowDto registrationFlowDto,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.createRegistrationFlow(organizationRecord, registrationFlowDto, id));
  }

  /**
   * Updates a registration flow. The flow must belong to the calling organization.
   *
   * @param id the flow ID
   * @param registrationFlowDto the updated flow definition
   * @param organizationRecord the calling organization
   * @return the updated flow DTO
   */
  @PutMapping("/flow/{flowid}")
  @Operation(summary = "Update a registration flow")
  public ResponseEntity<RegistrationFlowDto> updateFlow(
      @PathVariable("flowid") final UUID id,
      @RequestBody final RegistrationFlowDto registrationFlowDto,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.updateRegistrationFlow(organizationRecord, id, registrationFlowDto));
  }

  /**
   * Deletes a registration flow. The flow must belong to the calling organization.
   *
   * @param id the flow ID to delete
   * @param organizationRecord the calling organization
   * @return no-content response
   */
  @DeleteMapping("/flow/{flowid}")
  @Operation(summary = "Delete a registration flow")
  public ResponseEntity<Void> deleteFlow(
      @PathVariable("flowid") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.deleteRegistrationFlow(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists flows assigned to an intermediate.
   *
   * @param taImId the intermediate ID
   * @param organizationRecord the calling organization
   * @return list of flow DTOs
   */
  @GetMapping("/intermediate/{taImId}/flows")
  @Operation(summary = "List flows assigned to an intermediate")
  public ResponseEntity<List<RegistrationFlowDto>> getFlowsForIntermediate(
      @PathVariable("taImId") final UUID taImId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getFlowsForIntermediate(taImId));
  }

  /**
   * Returns all flow assignments for an intermediate, including assign IDs for unassign calls.
   *
   * @param taImId the intermediate ID
   * @param organizationRecord the calling organization
   * @return list of assignment summaries
   */
  @GetMapping("/intermediate/{taImId}/assignments")
  @Operation(summary = "List flow assignments for an intermediate (includes assignId)")
  public ResponseEntity<List<IntermediateFlowAssignmentDto>> getFlowAssignments(
      @PathVariable("taImId") final UUID taImId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getFlowAssignmentsForIntermediate(taImId));
  }

  /**
   * Assigns a flow to an intermediate. Idempotent: if the flow is already assigned the existing
   * assignment ID is returned.
   *
   * @param taImId the intermediate ID
   * @param request body containing the flow ID to assign
   * @param organizationRecord the calling organization
   * @return 201 Created with the assignment ID
   */
  @PostMapping("/intermediate/{taImId}/assign")
  @Operation(summary = "Assign a flow to an intermediate")
  public ResponseEntity<AssignFlowResponse> assignFlow(
      @PathVariable("taImId") final UUID taImId,
      @RequestBody final AssignFlowRequest request,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.registrationFlowService.assignFlow(taImId, request.flowId()));
  }

  /**
   * Removes a flow assignment from an intermediate.
   *
   * @param taImId the intermediate ID
   * @param assignId the assignment ID to remove
   * @param organizationRecord the calling organization
   * @return no-content response
   */
  @DeleteMapping("/intermediate/{taImId}/assign/{assignId}")
  @Operation(summary = "Remove a flow assignment from an intermediate")
  public ResponseEntity<Void> unassignFlow(
      @PathVariable("taImId") final UUID taImId,
      @PathVariable("assignId") final UUID assignId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.unassignFlow(taImId, assignId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns all flow assignments for a trust mark issuer, including assign IDs for unassign calls.
   *
   * @param tmIssuerId the trust mark issuer ID
   * @param organizationRecord the calling organization
   * @return list of assignment summaries
   */
  @GetMapping("/trustmark-issuer/{tmIssuerId}/assignments")
  @Operation(summary = "List flow assignments for a trust mark issuer (includes assignId)")
  public ResponseEntity<List<TrustMarkIssuerFlowAssignmentDto>> getTrustMarkIssuerFlowAssignments(
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.registrationFlowService.getFlowAssignmentsForTrustMarkIssuer(tmIssuerId));
  }

  /**
   * Assigns a flow to a trust mark issuer. Idempotent: if the flow is already assigned the existing
   * assignment ID is returned.
   *
   * @param tmIssuerId the trust mark issuer ID
   * @param request body containing the flow ID to assign
   * @param organizationRecord the calling organization
   * @return 201 Created with the assignment ID
   */
  @PostMapping("/trustmark-issuer/{tmIssuerId}/assign")
  @Operation(summary = "Assign a flow to a trust mark issuer")
  public ResponseEntity<AssignFlowResponse> assignFlowToTrustMarkIssuer(
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @RequestBody final AssignFlowRequest request,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.registrationFlowService.assignFlowToTrustMarkIssuer(tmIssuerId, request.flowId()));
  }

  /**
   * Removes a flow assignment from a trust mark issuer.
   *
   * @param tmIssuerId the trust mark issuer ID
   * @param assignId the assignment ID to remove
   * @param organizationRecord the calling organization
   * @return no-content response
   */
  @DeleteMapping("/trustmark-issuer/{tmIssuerId}/assign/{assignId}")
  @Operation(summary = "Remove a flow assignment from a trust mark issuer")
  public ResponseEntity<Void> unassignFlowFromTrustMarkIssuer(
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @PathVariable("assignId") final UUID assignId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.unassignFlowFromTrustMarkIssuer(tmIssuerId, assignId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists flow assignments for all trust marks under a trust mark issuer.
   *
   * @param tmIssuerId trust mark issuer ID
   * @param organizationRecord resolved organization
   * @return list of trust mark flow assignments
   */
  @GetMapping("/trustmark-issuer/{tmIssuerId}/trustmark-assignments")
  @Operation(summary = "List flow assignments for all trust marks under a trust mark issuer")
  public ResponseEntity<List<TrustMarkFlowAssignmentDto>> getTrustMarkFlowAssignments(
      @PathVariable("tmIssuerId") final UUID tmIssuerId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(
        this.registrationFlowService.getFlowAssignmentsForTrustMarkIssuerTrustmarks(tmIssuerId));
  }

  /**
   * Assigns a flow to a specific trust mark.
   *
   * @param trustmarkId trust mark ID
   * @param request assign flow request
   * @param organizationRecord resolved organization
   * @return created assignment response
   */
  @PostMapping("/trustmark/{trustmarkId}/assign")
  @Operation(summary = "Assign a flow to a specific trust mark")
  public ResponseEntity<AssignFlowResponse> assignFlowToTrustMark(
      @PathVariable("trustmarkId") final UUID trustmarkId,
      @RequestBody final AssignFlowRequest request,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.registrationFlowService.assignFlowToTrustMark(trustmarkId, request.flowId()));
  }

  /**
   * Removes a flow assignment from a specific trust mark.
   *
   * @param trustmarkId trust mark ID
   * @param assignId assignment ID
   * @param organizationRecord resolved organization
   * @return no content response
   */
  @DeleteMapping("/trustmark/{trustmarkId}/assign/{assignId}")
  @Operation(summary = "Remove a flow assignment from a specific trust mark")
  public ResponseEntity<Void> unassignFlowFromTrustMark(
      @PathVariable("trustmarkId") final UUID trustmarkId,
      @PathVariable("assignId") final UUID assignId,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.registrationFlowService.unassignFlowFromTrustMark(trustmarkId, assignId);
    return ResponseEntity.noContent().build();
  }

}