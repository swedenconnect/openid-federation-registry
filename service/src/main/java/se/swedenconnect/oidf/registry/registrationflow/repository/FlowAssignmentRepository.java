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
package se.swedenconnect.oidf.registry.registrationflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for flow-to-intermediate assignments.
 *
 * @author Per Fredrik Plars
 */
public interface FlowAssignmentRepository extends JpaRepository<FlowAssignment, UUID> {

  /**
   * Find all assignments for a given intermediate module.
   *
   * @param taImId the intermediate module ID
   * @return list of assignments
   */
  List<FlowAssignment> findByTaImTaImId(UUID taImId);

  /**
   * Find an assignment by its assign ID and intermediate module ID.
   *
   * @param assignId the assignment ID
   * @param taImId the intermediate module ID
   * @return the assignment if found
   */
  Optional<FlowAssignment> findByAssignIdAndTaImTaImId(UUID assignId, UUID taImId);

  /**
   * Find an existing assignment for a given intermediate and flow combination.
   *
   * @param taImId the intermediate module ID
   * @param flowId the registration flow ID
   * @return the assignment if found
   */
  Optional<FlowAssignment> findByTaImTaImIdAndRegistrationFlowFlowId(UUID taImId, UUID flowId);
}