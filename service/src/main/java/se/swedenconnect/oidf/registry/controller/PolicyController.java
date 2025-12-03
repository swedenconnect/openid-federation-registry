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
import se.swedenconnect.oidf.registry.api.dto.PolicyDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.service.PolicyService;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing policies using typed DTOs.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/policies")
@Tag(name = "Policies", description = "CRUD for metadata policies")
public class PolicyController {

  private final PolicyService policyService;

  @GetMapping
  @Operation(summary = "List all policies")
  public ResponseEntity<List<PolicyDto>> listPolicies(
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.policyService.listPolicies(organizationRecord));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get policy by id")
  public ResponseEntity<PolicyDto> getPolicy(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return ResponseEntity.ok(this.policyService.getPolicy(organizationRecord, id));
  }

  @PostMapping
  @Operation(summary = "Create policy with auto-generated ID")
  public ResponseEntity<PolicyDto> createPolicy(
      @RequestBody final PolicyDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    final UUID id = UUID.randomUUID();
    return ResponseEntity.ok(this.policyService.createPolicy(organizationRecord, id, body));
  }

  @PostMapping("/{id}")
  @Operation(summary = "Create policy with specified ID")
  public ResponseEntity<PolicyDto> createPolicyWithId(
      @PathVariable("id") final UUID id,
      @RequestBody final PolicyDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.policyService.createPolicy(organizationRecord, id, body));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update policy")
  public ResponseEntity<PolicyDto> updatePolicy(
      @PathVariable("id") final UUID id,
      @RequestBody final PolicyDto body,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    new ValidateDto(organizationRecord).validate(body);
    return ResponseEntity.ok(this.policyService.updatePolicy(organizationRecord, id, body));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete policy")
  public ResponseEntity<Void> deletePolicy(
      @PathVariable("id") final UUID id,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    this.policyService.deletePolicy(organizationRecord, id);
    return ResponseEntity.noContent().build();
  }
}


