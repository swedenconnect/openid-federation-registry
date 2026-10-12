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
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewItemType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One thing reviewed for an entity: a domain, a trust mark request, or a step of the entity's registration.
 *
 * @param type the kind of item
 * @param itemId the domain, or the registration (the trust mark's own, or the entity's)
 * @param stepIndex for a registration, the index of the step; send it back when deciding
 * @param title what the item is, such as the domain or the step name
 * @param subtitle what the item belongs to
 * @param state where the item stands
 * @param waitingFor why the item cannot be decided yet, for {@link ReviewItemState#WAITING}
 * @param decidedBy who decided the item
 * @param decidedAt when the item was decided
 * @param rejectionReason why the item was rejected
 * @param decidedInReviewOf the entity in whose review a shared domain was decided, if not this one
 * @param deferred whether the latest decision in this review was to defer
 * @param preview what the operator reviews; {@code null} for an upcoming step
 * @author Felix Hellman
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ReviewItem")
public record ReviewItemDto(
    ReviewItemType type,
    UUID itemId,
    Integer stepIndex,
    String title,
    String subtitle,
    ReviewItemState state,
    String waitingFor,
    String decidedBy,
    OffsetDateTime decidedAt,
    String rejectionReason,
    String decidedInReviewOf,
    boolean deferred,
    StepPreview preview) {
}
