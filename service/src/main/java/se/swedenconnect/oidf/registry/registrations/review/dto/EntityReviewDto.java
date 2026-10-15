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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Everything reviewed for one entity, in review order: domains, then trust marks, then the entity's own steps.
 *
 * @param registrationId the entity's registration
 * @param entityId the entity identifier
 * @param organizationName the organization's legal name, or its name when no legal name is recorded
 * @param organizationNumber the organization number
 * @param submittedAt when the registration was submitted
 * @param registrationStatus the entity registration's status
 * @param hosted whether the registry hosts the entity
 * @param update whether the entity is already in the federation: the registration updates its subordinate statement,
 *     and the federation keeps the published one until the update is approved
 * @param reviewState where the review stands
 * @param review the open review session, or the latest one; {@code null} if none was started
 * @param items the items in review order
 * @author Felix Hellman
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "EntityReview")
public record EntityReviewDto(
    UUID registrationId,
    String entityId,
    String organizationName,
    String organizationNumber,
    OffsetDateTime submittedAt,
    String registrationStatus,
    boolean hosted,
    boolean update,
    EntityReviewState reviewState,
    ReviewSessionDto review,
    List<ReviewItemDto> items) {
}
