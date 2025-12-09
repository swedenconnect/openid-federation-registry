/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.registry.service;

import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.TrustmarkSubjectDto;

import java.util.UUID;

/**
 * Service interface for managing TrustmarkSubject objects.
 *
 * @author Per Fredrik Plars
 */
public interface TrustmarkSubjectService {

  /**
   * Creates a trust mark subject.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   * @param input the trust mark subject data
   * @return the created trust mark subject
   */
  TrustmarkSubjectDto createTrustmarkSubject(OrganizationRecord organizationRecord,
      UUID id, TrustmarkSubjectDto input);

  /**
   * Updates a trust mark subject.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   * @param input the trust mark subject data
   * @return the updated trust mark subject
   */
  TrustmarkSubjectDto updateTrustmarkSubject(OrganizationRecord organizationRecord,
      UUID id, TrustmarkSubjectDto input);

  /**
   * Gets a trust mark subject by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   * @return the trust mark subject
   */
  TrustmarkSubjectDto getTrustmarkSubject(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a trust mark subject.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   */
  void deleteTrustmarkSubject(OrganizationRecord organizationRecord, UUID id);
}


