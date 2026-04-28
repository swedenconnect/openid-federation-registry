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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrationflow.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.StepDto;

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

  public RegistrationFlowController(
      final RegistrationFlowService registrationFlowService) {
    this.registrationFlowService = registrationFlowService;
  }

  @GetMapping("/steps")
  @Operation(summary = "List all configured pipeline steps with their settings")
  public ResponseEntity<List<StepDto>> getSteps() {
    return ResponseEntity.ok(registrationFlowService.getDefineSteps());
  }

  @PostMapping("/flow")
  @Operation(summary = "Create a new flow")
  public ResponseEntity<RegistrationFlowDto> createFlow(final RegistrationFlowDto registrationFlowDto) {
    return ResponseEntity.ok(
        this.registrationFlowService.createRegistrationFlow(registrationFlowDto, UUID.randomUUID()));
  }

  @PostMapping("/flow/{flowid}")
  @Operation(summary = "Create a new flow with a specified id")
  public ResponseEntity<RegistrationFlowDto> createFlowWithId(@PathVariable("flowid") final UUID id,
      final RegistrationFlowDto registrationFlowDto) {

    return ResponseEntity.ok().build();
  }

  @PutMapping("/flow/{flowid}")
  @Operation(summary = "Update a registration flow")
  public ResponseEntity<RegistrationFlowDto> updateFlow(@PathVariable("flowid") final UUID id,
      final RegistrationFlowDto registrationFlowDto) {

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/flow/{flowid}")
  @Operation(summary = "Update a registration flow")
  public ResponseEntity<RegistrationFlowDto> deleteFlow(@PathVariable("flowid") final UUID id) {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/intermediate/{taImId}/flows")
  @Operation(summary = "List flows assigned to an intermediate")
  public ResponseEntity<List<RegistrationFlowDto>> getFlowsForIntermediate(
      @PathVariable("taImId") final UUID taImId) {
    return ResponseEntity.ok(registrationFlowService.getFlowsForIntermediate(taImId));
  }

  @PutMapping("/intermediate/{taImId}/flows")
  @Operation(summary = "Set (replace) the flows assigned to an intermediate")
  public ResponseEntity<Void> setFlowsForIntermediate(
      @PathVariable("taImId") final UUID taImId,
      @RequestBody final List<UUID> flowIds) {
    registrationFlowService.setFlowsForIntermediate(taImId, flowIds);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/intermediate/{taImId}/flows/{flowId}")
  @Operation(summary = "Remove a specific flow from an intermediate")
  public ResponseEntity<Void> removeFlowFromIntermediate(
      @PathVariable("taImId") final UUID taImId,
      @PathVariable("flowId") final UUID flowId) {
    registrationFlowService.removeFlowFromIntermediate(taImId, flowId);
    return ResponseEntity.noContent().build();
  }

}
