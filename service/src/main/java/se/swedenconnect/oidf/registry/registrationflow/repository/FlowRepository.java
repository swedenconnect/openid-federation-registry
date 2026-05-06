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
import org.springframework.stereotype.Repository;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing registration flows.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface FlowRepository extends JpaRepository<RegistrationFlow, UUID> {

  /**
   * Returns all flows belonging to the given organization.
   *
   * @param orgNumber the organization number
   * @return list of flows
   */
  List<RegistrationFlow> findByOrganizationOrgNumber(String orgNumber);

  /**
   * Returns a flow by org number and flow ID, or empty if not found or owned by another org.
   *
   * @param orgNumber the organization number
   * @param flowId the flow ID
   * @return the flow if it exists and belongs to the org
   */
  Optional<RegistrationFlow> findByOrganizationOrgNumberAndFlowId(String orgNumber, UUID flowId);

}
