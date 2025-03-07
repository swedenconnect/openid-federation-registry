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
package se.swedenconnect.oidf.entity.registry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PolicyRepository is a JPA repository interface for accessing and performing CRUD operations on {@link PolicyEntity}
 * entities stored in the database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods for standard data access layers.
 *
 * @author David Goldring
 * @author Per Fredrik Plars
 */
public interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {
  /**
   * Retrieves a {@link PolicyEntity} associated with the specified organization ID.
   *
   * @param organizationId the unique identifier of the organization whose policy is to be retrieved
   * @return an {@link Optional} containing the {@link PolicyEntity} if found, or an empty {@link Optional}
   * if no policy exists for the given organization ID
   */
  List<PolicyEntity> findByOrganizationId(UUID organizationId);

  /**
   * Retrieves a {@link PolicyEntity} associated with the specified policy ID and organization ID.
   *
   * @param policyId the unique identifier of the policy to be retrieved
   * @param organizationId the unique identifier of the organization associated with the policy
   * @return an {@link Optional} containing the {@link PolicyEntity} if found, or an empty {@link Optional} if no
   *     matching policy exists for the given policy ID and organization ID
   */
  Optional<PolicyEntity> findByPolicyIdAndOrganizationId(UUID policyId, UUID organizationId);

}
