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

import se.swedenconnect.oidf.registry.api.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.api.dto.input.TrustmarkSubjectInputDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;

import java.util.UUID;

/**
 * Service interface for managing TrustmarkSubject objects.
 *
 * @author Per Fredrik Plars
 */
public interface TrustmarkSubjectService {

  TrustmarkSubjectDto createTrustmarkSubject(OrganizationRecord organizationRecord,
      UUID id, TrustmarkSubjectInputDto input);

  TrustmarkSubjectDto updateTrustmarkSubject(OrganizationRecord organizationRecord,
      UUID id, TrustmarkSubjectInputDto input);

  TrustmarkSubjectDto getTrustmarkSubject(OrganizationRecord organizationRecord, UUID id);

  void deleteTrustmarkSubject(OrganizationRecord organizationRecord, UUID id);
}


