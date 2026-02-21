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

package se.swedenconnect.oidf.registry.subordinate.service;

import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;

import java.util.UUID;

/**
 * Service interface for managing Subordinate entities.
 *
 * @author Per Fredrik Plars
 */
public interface SubordinateService {

  /**
   * Gets a subordinate by ID.
   *
   * @param organizationRecord the organization record
   * @param id the subordinate ID
   * @return the subordinate
   */
  SubordinateDto getSubordinate(OrganizationRecord organizationRecord, UUID id);

  /**
   * Creates a subordinate with auto-generated ID.
   *
   * @param organizationRecord the organization record
   * @param input the subordinate data
   * @return the created subordinate
   */
  SubordinateDto createSubordinate(OrganizationRecord organizationRecord, SubordinateDto input);

  /**
   * Creates a subordinate with specified ID.
   *
   * @param organizationRecord the organization record
   * @param id the subordinate ID
   * @param input the subordinate data
   * @return the created subordinate
   */
  SubordinateDto createSubordinateWithId(OrganizationRecord organizationRecord, UUID id,
      SubordinateDto input);

  /**
   * Updates a subordinate.
   *
   * @param organizationRecord the organization record
   * @param id the subordinate ID
   * @param input the subordinate data
   * @return the updated subordinate
   */
  SubordinateDto updateSubordinate(OrganizationRecord organizationRecord, UUID id, SubordinateDto input);

  /**
   * Deletes a subordinate.
   *
   * @param organizationRecord the organization record
   * @param id the subordinate ID
   */
  void deleteSubordinate(OrganizationRecord organizationRecord, UUID id);
}
