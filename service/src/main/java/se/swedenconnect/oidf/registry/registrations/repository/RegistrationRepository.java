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
package se.swedenconnect.oidf.registry.registrations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Registration} entities.
 *
 * @author Per Fredrik Plars
 */
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

  /**
   * Finds a registration by entity ID and status.
   *
   * @param entityId the entity identifier
   * @param status the registration status
   * @return optional registration
   */
  Optional<Registration> findByEntityIdAndStatus(String entityId, RegistrationStatus status);

  /**
   * Finds all registrations for an intermediate with the given status.
   *
   * @param taimId the intermediate ID
   * @param status the registration status
   * @return list of matching registrations
   */
  List<Registration> findByTaIm_TaImIdAndStatus(UUID taimId, RegistrationStatus status);

  /**
   * Counts registrations for an intermediate with the given status.
   *
   * @param taimId the intermediate ID
   * @param status the registration status
   * @return count of matching registrations
   */
  long countByTaIm_TaImIdAndStatus(UUID taimId, RegistrationStatus status);

  /**
   * Deletes registrations with the given status created before the given date.
   *
   * @param status the registration status
   * @param before the cutoff date
   */
  void deleteByStatusAndCreatedDateBefore(RegistrationStatus status, LocalDateTime before);
}