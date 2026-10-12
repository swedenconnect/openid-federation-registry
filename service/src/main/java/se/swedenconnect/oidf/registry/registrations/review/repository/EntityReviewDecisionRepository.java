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
package se.swedenconnect.oidf.registry.registrations.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReviewDecision;
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewDecision;
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewItemType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Decisions taken in review sessions.
 *
 * @author Felix Hellman
 */
public interface EntityReviewDecisionRepository extends JpaRepository<EntityReviewDecision, UUID> {

  /**
   * Lists a session's decisions, oldest first.
   *
   * @param reviewId the session
   * @return the decisions
   */
  List<EntityReviewDecision> findByReview_ReviewIdOrderByDecidedAtAsc(UUID reviewId);

  /**
   * Finds the latest decision of the given kind on an item, in any session.
   *
   * @param itemType the kind of item
   * @param itemId the item
   * @param decisions the decisions to look for
   * @return the decision, if any
   */
  Optional<EntityReviewDecision> findFirstByItemTypeAndItemIdAndDecisionInOrderByDecidedAtDesc(
      ReviewItemType itemType, UUID itemId, List<ReviewDecision> decisions);
}
