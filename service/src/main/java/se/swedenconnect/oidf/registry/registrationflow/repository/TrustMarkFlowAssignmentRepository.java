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
import se.swedenconnect.oidf.registry.registrationflow.model.TrustMarkFlowAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for flow-to-trust-mark assignments.
 *
 * @author Felix Hellman
 */
public interface TrustMarkFlowAssignmentRepository extends JpaRepository<TrustMarkFlowAssignment, UUID> {

  /**
   * Finds assignment by trust mark ID.
   *
   * @param trustmarkId trust mark ID
   * @return matching assignment
   */
  Optional<TrustMarkFlowAssignment> findByTrustMarkTrustmarkId(UUID trustmarkId);

  /**
   * Finds assignments by trust mark issuer ID.
   *
   * @param trustmarkIssuerId trust mark issuer ID
   * @return matching assignments
   */
  List<TrustMarkFlowAssignment> findByTrustMarkTrustmarkIssuerTrustmarkIssuerId(UUID trustmarkIssuerId);

  /**
   * Finds assignment by trust mark ID and flow ID.
   *
   * @param trustmarkId trust mark ID
   * @param flowId flow ID
   * @return matching assignment
   */
  Optional<TrustMarkFlowAssignment> findByTrustMarkTrustmarkIdAndRegistrationFlowFlowId(
      UUID trustmarkId, UUID flowId);
}
