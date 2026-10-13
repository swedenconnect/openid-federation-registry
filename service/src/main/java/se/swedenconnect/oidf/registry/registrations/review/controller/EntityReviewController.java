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
package se.swedenconnect.oidf.registry.registrations.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.registrations.review.dto.EntityReviewDto;
import se.swedenconnect.oidf.registry.registrations.review.dto.EntityReviewSummaryDto;
import se.swedenconnect.oidf.registry.registrations.review.dto.ReviewDecisionRequestDto;
import se.swedenconnect.oidf.registry.registrations.review.service.EntityReviewService;

import java.util.List;
import java.util.UUID;

/**
 * Operator review of registrations, one entity at a time: its domain, then its trust mark requests, then its own
 * registration. An entity is identified by the ID of its own registration.
 *
 * @author Felix Hellman
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registration-admin/v1/{tenant}/{orgNumber}/entity-reviews")
@Tag(name = "EntityReview", description = "Operator review of everything an entity waits for")
public class EntityReviewController {

  private final EntityReviewService entityReviewService;

  /**
   * Lists the entities registered on the tenant.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param all {@code true} to include entities with nothing left to review
   * @return the entities
   */
  @GetMapping
  @PreAuthorize("@orgRightsService.canReadAsOperator(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List entities with what each waits for")
  public ResponseEntity<List<EntityReviewSummaryDto>> list(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @Parameter(description = "Include entities with nothing left to review")
      @RequestParam(name = "all", defaultValue = "false") final boolean all) {
    return ResponseEntity.ok(this.entityReviewService.list(organizationRecord, !all));
  }

  /**
   * Returns everything reviewed for one entity, in review order.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param registrationId the entity's registration
   * @return the entity's review
   */
  @GetMapping("/{registrationId}")
  @PreAuthorize("@orgRightsService.canReadAsOperator(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Get an entity's review items")
  public ResponseEntity<EntityReviewDto> get(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @PathVariable("registrationId") final UUID registrationId) {
    return ResponseEntity.ok(this.entityReviewService.get(organizationRecord, registrationId));
  }

  /**
   * Starts the review of an entity, or continues the open one.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param registrationId the entity's registration
   * @return the entity's review
   */
  @PostMapping("/{registrationId}/start")
  @PreAuthorize("@orgRightsService.canWriteAsOperator(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Start or continue the review of an entity")
  public ResponseEntity<EntityReviewDto> start(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @PathVariable("registrationId") final UUID registrationId) {
    return ResponseEntity.ok(this.entityReviewService.start(organizationRecord, registrationId));
  }

  /**
   * Approves, rejects or defers one item of an open review.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param organizationRecord the calling organization
   * @param registrationId the entity's registration
   * @param request the decision
   * @return the entity's review after the decision
   */
  @PostMapping("/{registrationId}/decisions")
  @PreAuthorize("@orgRightsService.canWriteAsOperator(authentication, #orgNumber, #tenant)")
  @Operation(summary = "Approve, reject or defer one review item")
  public ResponseEntity<EntityReviewDto> decide(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord,
      @PathVariable("registrationId") final UUID registrationId,
      @RequestBody final ReviewDecisionRequestDto request) {
    return ResponseEntity.ok(this.entityReviewService.decide(organizationRecord, registrationId, request));
  }
}
