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

package se.swedenconnect.oidf.registry.module.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.swedenconnect.oidf.registry.module.model.Resolver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Resolver.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface ResolverRepository extends JpaRepository<Resolver, UUID> {

  /**
   * Retrieves a resolver entity based on the given organization number and resolver ID.
   *
   * @param orgNumber the organization number associated with the resolver
   * @param resolverId the unique identifier of the resolver
   * @return an Optional containing the Resolver if found, otherwise an empty Optional
   */
  @Query("SELECT r FROM Resolver r JOIN r.entity e JOIN e.organization o "
      + "WHERE o.orgNumber = :orgNumber AND r.resolverId = :resolverId")
  Optional<Resolver> findByOrgNumberAndResolverId(
      @Param("orgNumber") String orgNumber, @Param("resolverId") UUID resolverId);

  /**
   * Retrieves a list of Resolver objects associated with the specified organization number.
   *
   * @param orgNumber the organization number to filter by.
   * @return a list of Resolver objects matching the specified organization number.
   */
  @Query("SELECT r FROM Resolver r JOIN r.entity e JOIN e.organization o "
      + "WHERE o.orgNumber = :orgNumber")
  List<Resolver> findByOrgNumber(@Param("orgNumber") String orgNumber);
}
