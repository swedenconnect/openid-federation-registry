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
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewDecision;
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewItemType;

import java.util.UUID;

/**
 * An operator's decision on one review item.
 *
 * @param itemType the kind of item
 * @param itemId the item, as listed in the review
 * @param stepIndex for a registration, the step index as listed; a stale index is refused
 * @param decision approve, reject or defer
 * @param reason why the item is rejected; required for a rejection
 * @author Felix Hellman
 */
@Schema(name = "ReviewDecisionRequest")
public record ReviewDecisionRequestDto(
    ReviewItemType itemType,
    UUID itemId,
    Integer stepIndex,
    ReviewDecision decision,
    String reason) {
}
