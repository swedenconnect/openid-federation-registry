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
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationFlowInformationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestStatusDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing federation registrations (join, hosted entities, flows).
 *
 * @author Per Fredrik Plars
 */
public interface RegistrationService {



  /**
   * Creates a join application with a specified ID.
   *
   * @param organizationRecord the calling organization
   * @param joinId the join ID to use
   * @param request the join request
   * @return the created join record
   */
  RegistrationRequestStatusDto createRegistrationRequest(OrganizationRecord organizationRecord, UUID joinId,
      RegistrationJoinRequestDto request);

  /**
   * Returns a single registration by ID.
   *
   * @param registrationId the registration ID
   * @return the registration DTO
   */
  RegistrationDto getRegistrationById(UUID registrationId);


  /**
   * Removes a join record by ID.
   *
   * @param organizationRecord the calling organization
   * @param joinId the ID of the join record to remove
   */
  void deleteRegistrationRequest(OrganizationRecord organizationRecord, UUID joinId);

  /**
   * Returns all join records visible to the caller.
   *
   * @param organizationRecord the calling organization
   * @return list of join records
   */
  List<RegistrationDto> listRegistrations(OrganizationRecord organizationRecord);

  /**
   * Returns all available registration flows.
   *
   * @param organizationRecord the calling organization
   * @return list of flows
   */
  List<RegistrationFlowInformationDto> listRegistrationFlows(OrganizationRecord organizationRecord);
}