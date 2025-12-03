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
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityRepository is a JPA repository interface for accessing and performing CRUD operations on
 * {@link TrustMarkSubjectEntity} entities stored in a database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods for standard data access layers.
 *
 * @author Per Fredrik Plars
 */
public interface TrustMarkSubjectRepository extends JpaRepository<TrustMarkSubjectEntity, UUID> {

  /**
   * Executes the SQL query to retrieve trust mark subjects based on the organization number.
   *
   * @param orgNumber the organization number
   * @return a list of TrustMarkSubjectEntity
   */
  @Query("SELECT t FROM TrustMarkSubjectEntity t "
        + "JOIN t.trustMark tm "
        + "JOIN tm.module m "
        + "JOIN m.organization o "
        + "WHERE o.orgNumber = :orgNumber")
  List<TrustMarkSubjectEntity> findByOrgNumber(@Param("orgNumber") String orgNumber);

  /**
   * Retrieves a {@link TrustMarkEntity} based on the organization's unique number and the trust mark subject's unique
   * identifier.
   *
   * @param orgNumber the unique number identifying the organization
   * @param trustmarksubjectId the unique identifier for the trust mark subject
   * @return an {@link Optional} containing the {@link TrustMarkEntity} if found, otherwise empty
   */
  @Query("SELECT ts FROM TrustMarkSubjectEntity ts "
      + "JOIN ts.trustMark t "
      + "JOIN t.module m "
      + "JOIN m.organization o "
      + "WHERE o.orgNumber = :orgNumber AND ts.trustmarksubjectId = :id")
  Optional<TrustMarkSubjectEntity> findByOrgNumberAndTrustmarkId(@Param("orgNumber") String orgNumber,
      @Param("id") UUID trustmarksubjectId);

}
