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
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReview;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReviewStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Review sessions.
 *
 * @author Felix Hellman
 */
public interface EntityReviewRepository extends JpaRepository<EntityReview, UUID> {

  /**
   * Finds an entity's review session in the given state.
   *
   * @param registrationId the entity's registration
   * @param status the state
   * @return the session, if any
   */
  Optional<EntityReview> findFirstByRegistration_RegistrationIdAndStatus(UUID registrationId,
      EntityReviewStatus status);

  /**
   * Finds an entity's latest review session.
   *
   * @param registrationId the entity's registration
   * @return the session, if any
   */
  Optional<EntityReview> findFirstByRegistration_RegistrationIdOrderByStartedAtDesc(UUID registrationId);

  /**
   * Finds every review session on an instance.
   *
   * @param instanceId the instance
   * @return the sessions
   */
  List<EntityReview> findByInstanceId(UUID instanceId);
}
