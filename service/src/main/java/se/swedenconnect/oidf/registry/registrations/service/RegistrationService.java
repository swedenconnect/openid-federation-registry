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

import se.swedenconnect.oidf.registry.registrations.dto.FlowDto;
import se.swedenconnect.oidf.registry.registrations.dto.HostedRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.HostedUpdateDto;
import se.swedenconnect.oidf.registry.registrations.dto.JoinDto;
import se.swedenconnect.oidf.registry.registrations.dto.JoinRequestDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing federation registrations (join, hosted entities, flows).
 *
 * @author Per Fredrik Plars
 */
public interface RegistrationService {

  /**
   * Creates a join application with an auto-generated ID.
   *
   * @param request the join request
   * @return the created join record
   */
  JoinDto createJoin(JoinRequestDto request);

  /**
   * Creates a join application with a specified ID.
   *
   * @param joinId  the join ID to use
   * @param request the join request
   * @return the created join record
   */
  JoinDto createJoinWithId(UUID joinId, JoinRequestDto request);

  /**
   * Removes a join record by ID.
   *
   * @param joinId the ID of the join record to remove
   */
  void deleteJoin(UUID joinId);

  /**
   * Returns all join records visible to the caller.
   *
   * @return list of join records
   */
  List<JoinDto> listJoins();

  /**
   * Returns all available registration flows.
   *
   * @return list of flows
   */
  List<FlowDto> listFlows();
}
