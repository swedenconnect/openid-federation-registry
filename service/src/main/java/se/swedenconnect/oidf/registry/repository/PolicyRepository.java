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
package se.swedenconnect.oidf.registry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;

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
   * Finds and retrieves a list of {@link PolicyEntity} instances associated with the specified organization number.
   * The query performs a join fetch to retrieve the related organization entity.
   *
   * @param orgNumber the organization number used to filter the policies
   * @return a list of {@link PolicyEntity} associated with the given organization number
   */
  @Query("SELECT p FROM PolicyEntity p JOIN fetch p.organization o "
      + "WHERE o.orgNumber = :orgNumber")
  List<PolicyEntity> findByOrgNumber(@Param("orgNumber") String orgNumber);

  /**
   * Retrieves a {@link PolicyEntity} that belongs to the organization with the specified organization number and has
   * the specified policy ID.
   *
   * @param orgNumber the unique number identifying the organization
   * @param policyid the unique identifier of the policy
   * @return an {@link Optional} containing the matching {@link PolicyEntity} if one exists, otherwise an empty
   *     {@link Optional}
   */
  @Query("SELECT p FROM PolicyEntity p JOIN fetch p.organization o "
      + "WHERE o.orgNumber = :orgNumber AND p.policyId = :policyid")
  Optional<PolicyEntity> findByOrgNumberAndPolicyId(@Param("orgNumber") String orgNumber,
      @Param("policyid") UUID policyid);

}
