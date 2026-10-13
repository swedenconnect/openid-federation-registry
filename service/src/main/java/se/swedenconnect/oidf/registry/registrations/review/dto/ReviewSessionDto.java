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
import se.swedenconnect.oidf.registry.infrastructure.persistence.Timestamps;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReview;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A review session.
 *
 * @param reviewId the session
 * @param status whether the session is open
 * @param startedBy who started it
 * @param startedAt when it was started
 * @param completedAt when nothing was left to decide, {@code null} while open
 * @author Felix Hellman
 */
@Schema(name = "ReviewSession")
public record ReviewSessionDto(UUID reviewId, EntityReviewStatus status, String startedBy, OffsetDateTime startedAt,
    OffsetDateTime completedAt) {

  /**
   * Maps a session.
   *
   * @param review the session
   * @return the DTO
   */
  public static ReviewSessionDto of(final EntityReview review) {
    return new ReviewSessionDto(review.getReviewId(), review.getStatus(), review.getStartedBy(),
        Timestamps.toOffsetDateTime(review.getStartedAt()), Timestamps.toOffsetDateTime(review.getCompletedAt()));
  }
}
