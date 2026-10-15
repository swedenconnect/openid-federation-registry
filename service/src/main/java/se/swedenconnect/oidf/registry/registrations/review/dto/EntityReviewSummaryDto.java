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
package se.swedenconnect.oidf.registry.registrations.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An entity in the review list.
 *
 * @param registrationId the entity's registration, which identifies the entity in the review API
 * @param entityId the entity identifier
 * @param organizationName the organization's legal name, or its name when no legal name is recorded
 * @param organizationNumber the organization number
 * @param submittedAt when the registration was submitted
 * @param registrationStatus the entity registration's status
 * @param hosted whether the registry hosts the entity
 * @param update whether the entity is already in the federation: the registration updates its subordinate statement,
 *     and the federation keeps the published one until the update is approved
 * @param pendingDomains domains waiting for a decision
 * @param pendingTrustMarks trust mark requests waiting for a decision
 * @param pendingEntitySteps steps of the entity's registration waiting for a decision
 * @param reviewState where the review stands
 * @param startedBy who started the open review
 * @param startedAt when the open review was started
 * @param decidedItems items decided in the open review
 * @author Felix Hellman
 */
@Schema(name = "EntityReviewSummary")
public record EntityReviewSummaryDto(
    UUID registrationId,
    String entityId,
    String organizationName,
    String organizationNumber,
    OffsetDateTime submittedAt,
    String registrationStatus,
    boolean hosted,
    boolean update,
    int pendingDomains,
    int pendingTrustMarks,
    int pendingEntitySteps,
    EntityReviewState reviewState,
    String startedBy,
    OffsetDateTime startedAt,
    int decidedItems) {
}
