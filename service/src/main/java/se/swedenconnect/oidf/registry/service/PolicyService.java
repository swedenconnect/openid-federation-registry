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

import se.swedenconnect.oidf.registry.api.dto.PolicyDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.domain.Policies;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing policies.
 *
 * @author Per Fredrik Plars
 */
public interface PolicyService {

  /**
   * Lists all policies.
   *
   * @param organizationRecord the organization record
   * @return list of policies
   */
  List<PolicyDto> listPolicies(OrganizationRecord organizationRecord);

  /**
   * Gets a policy by ID.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @return the policy
   */
  PolicyDto getPolicy(OrganizationRecord organizationRecord, UUID id);

  /**
   * Creates a policy.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @param input the policy data
   * @return the created policy
   */
  PolicyDto createPolicy(OrganizationRecord organizationRecord, UUID id, PolicyDto input);

  /**
   * Updates a policy.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @param input the policy data
   * @return the updated policy
   */
  PolicyDto updatePolicy(OrganizationRecord organizationRecord, UUID id, PolicyDto input);

  /**
   * Deletes a policy.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   */
  void deletePolicy(OrganizationRecord organizationRecord, UUID id);

  /**
   * Gets a policy domain object.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @return the policy domain object
   */
  Policies getPolicyDomain(OrganizationRecord organizationRecord, UUID id);
}


