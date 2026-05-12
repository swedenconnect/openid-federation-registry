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
package se.swedenconnect.oidf.registry.registrations.service;

import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;

import java.util.List;
import java.util.UUID;

/**
 * Admin service for reviewing and managing registration requests.
 *
 * @author Per Fredrik Plars
 */
public interface RegistrationAdminService {

  /**
   * Counts registrations with PENDING_APPROVAL status for a given intermediate.
   *
   * @param taimId the intermediate ID
   * @return count of pending registrations
   */
  long countPending(UUID taimId);

  /**
   * Rejects a pending registration.
   *
   * @param id the registration ID
   * @param rejectionReason the reason for rejection
   * @return the updated registration DTO
   */
  RegistrationDto reject(UUID id, String rejectionReason);
}
