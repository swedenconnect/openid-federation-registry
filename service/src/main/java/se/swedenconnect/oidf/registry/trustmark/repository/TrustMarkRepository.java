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

package se.swedenconnect.oidf.registry.trustmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing TrustMark.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface TrustMarkRepository extends JpaRepository<TrustMark, UUID> {
  /**
   * Executes the SQL query to retrieve trust marks based on the organization number.
   *
   * @param orgNumber the organization number
   * @return a list of TrustMark
   */
  @Query("SELECT t FROM TrustMark t JOIN t.trustmarkIssuer tmi "
      + "JOIN tmi.entity e JOIN e.organization o WHERE o.orgNumber = :orgNumber")
  List<TrustMark> findByOrgNumber(@Param("orgNumber") String orgNumber);

  /**
   * Executes the SQL query to retrieve trust marks with subjects based on the organization number. Uses LEFT JOIN FETCH
   * to eagerly load trustmark subjects, including trustmarks without subjects.
   *
   * @param orgNumber the organization number
   * @return a list of TrustMark with subjects loaded
   */
  @Query("SELECT DISTINCT t FROM TrustMark t "
      + "LEFT JOIN FETCH t.trustmarksubjects "
      + "JOIN t.trustmarkIssuer tmi JOIN tmi.entity e JOIN e.organization o "
      + "WHERE o.orgNumber = :orgNumber")
  List<TrustMark> findByOrgNumberWithSubjects(@Param("orgNumber") String orgNumber);

  /**
   * Finds a {@link TrustMark} based on the organization's number and the trustmark's ID.
   *
   * @param orgNumber the unique number of the organization
   * @param trustmarkId the unique identifier of the trustmark
   * @return an {@link Optional} containing the matching {@link TrustMark} if found, otherwise an empty
   */
  @Query("SELECT t FROM TrustMark t JOIN t.trustmarkIssuer tmi "
      + "JOIN tmi.entity e JOIN e.organization o WHERE o.orgNumber = :orgNumber "
      + "AND t.trustmarkId = :trustmarkId")
  Optional<TrustMark> findByOrgNumberAndTrustmarkId(@Param("orgNumber") String orgNumber,
      @Param("trustmarkId") UUID trustmarkId);
}
