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
package se.swedenconnect.oidf.registry.registrations.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.registrations.dto.FlowDto;
import se.swedenconnect.oidf.registry.registrations.dto.JoinDto;
import se.swedenconnect.oidf.registry.registrations.dto.JoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.service.RegistrationService;

import java.util.List;
import java.util.UUID;

/**
 * Handles join applications — applying to and leaving the federation.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registration")
@Tag(name = "FederationRegistration", description = "Apply to and manage federation membership")
public class RegistrationController {

  private final RegistrationService registrationService;

  /**
   * Lists all join records.
   *
   * @return list of join DTOs
   */
  @GetMapping
  @Operation(summary = "List all join records")
  public ResponseEntity<List<JoinDto>> listJoins() {
    return ResponseEntity.ok(this.registrationService.listJoins());
  }

  /**
   * Creates a join application with an auto-generated ID.
   *
   * @param body the join request
   * @return the created join DTO
   */
  @PostMapping("/")
  @Operation(summary = "Create a join application with auto-generated ID")
  public ResponseEntity<JoinDto> createJoin(
      @RequestBody final JoinRequestDto body) {
    return ResponseEntity.status(201).body(this.registrationService.createJoin(body));
  }

  /**
   * Creates a join application with a specified ID.
   *
   * @param joinId the join ID to use
   * @param body the join request
   * @return the created join DTO
   */
  @PostMapping("/{joinId}")
  @Operation(summary = "Create a join application with specified ID")
  public ResponseEntity<JoinDto> createJoinWithId(
      @PathVariable("joinId") final UUID joinId,
      @RequestBody final JoinRequestDto body) {
    return ResponseEntity.status(201).body(this.registrationService.createJoinWithId(joinId, body));
  }

  /**
   * Removes a join record.
   *
   * @param joinId the ID of the join record to remove
   * @return no-content response
   */
  @DeleteMapping("/{joinId}")
  @Operation(summary = "Remove a join record")
  public ResponseEntity<Void> deleteJoin(
      @Parameter(description = "ID of the join record to remove") @PathVariable("joinId") final UUID joinId) {
    this.registrationService.deleteJoin(joinId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists all available registration flows.
   *
   * @return list of flow DTOs
   */
  @GetMapping("/flows")
  @Operation(summary = "List all available registration flows")
  public ResponseEntity<List<FlowDto>> listFlows() {
    return ResponseEntity.ok(this.registrationService.listFlows());
  }
}
