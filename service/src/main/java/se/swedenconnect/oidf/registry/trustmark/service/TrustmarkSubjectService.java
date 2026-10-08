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

package se.swedenconnect.oidf.registry.trustmark.service;

import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSourceDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;

import java.util.List;
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

  /**
   * Deletes the entries where {@code subject} is the subject of the trust marks pointed out by the trust mark
   * sources. Only trust marks issued by a trust mark issuer of the calling organization are affected, entries on trust
   * marks of other organizations are left alone.
   *
   * @param organizationRecord the organization record
   * @param subject the subject entity identifier
   * @param sources the trust mark sources, an issuer entity identifier and a trust mark type each
   * @return the number of deleted subject entries
   */
  int deleteSubjectsForTrustmarkSources(OrganizationRecord organizationRecord, String subject,
      List<TrustmarkSourceDto> sources);
}
