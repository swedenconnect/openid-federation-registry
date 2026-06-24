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
import se.swedenconnect.oidf.registry.registrationflow.model.TrustMarkIssuerFlowAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for flow-to-trust-mark-issuer assignments.
 *
 * @author Felix Hellman
 */
public interface TrustMarkIssuerFlowAssignmentRepository
    extends JpaRepository<TrustMarkIssuerFlowAssignment, UUID> {

  /**
   * Find all assignments for a given trust mark issuer.
   *
   * @param trustmarkIssuerId the trust mark issuer ID
   * @return list of assignments
   */
  List<TrustMarkIssuerFlowAssignment> findByTrustMarkIssuerTrustmarkIssuerId(UUID trustmarkIssuerId);

  /**
   * Find an assignment by its assign ID and trust mark issuer ID.
   *
   * @param assignId the assignment ID
   * @param trustmarkIssuerId the trust mark issuer ID
   * @return the assignment if found
   */
  Optional<TrustMarkIssuerFlowAssignment> findByAssignIdAndTrustMarkIssuerTrustmarkIssuerId(
      UUID assignId, UUID trustmarkIssuerId);

  /**
   * Find an existing assignment for a given trust mark issuer and flow combination.
   *
   * @param trustmarkIssuerId the trust mark issuer ID
   * @param flowId the registration flow ID
   * @return the assignment if found
   */
  Optional<TrustMarkIssuerFlowAssignment> findByTrustMarkIssuerTrustmarkIssuerIdAndRegistrationFlowFlowId(
      UUID trustmarkIssuerId, UUID flowId);
}
