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
   * @param trustmarkIssuerId optional trustmarkIssuerId
   * @return a list of TrustMark
   */
  @Query("""
      SELECT t FROM TrustMark t 
            JOIN t.trustmarkIssuer tmi 
            JOIN tmi.entity e 
            JOIN e.organization o 
            WHERE o.orgNumber = :orgNumber
            AND (:trustmarkIssuerId IS NULL OR tmi.trustmarkIssuerId = :trustmarkIssuerId)
      """)
  List<TrustMark> findByOrgNumber(@Param("orgNumber") String orgNumber,
      @Param("trustmarkIssuerId") UUID trustmarkIssuerId);

  /**
   * Executes the SQL query to retrieve trust marks with subjects based on the organization number. Uses LEFT JOIN FETCH
   * to eagerly load trustmark subjects, including trustmarks without subjects.
   *
   * @param orgNumber the organization number
   * @param trustmarkIssuerId trustmarkIssuerId
   * @return a list of TrustMark with subjects loaded
   */
  @Query("""
          SELECT DISTINCT t FROM TrustMark t
          LEFT JOIN FETCH t.trustmarksubjects
          JOIN t.trustmarkIssuer tmi
          JOIN tmi.entity e
          JOIN e.organization o
          WHERE o.orgNumber = :orgNumber
          AND (:trustmarkIssuerId IS NULL OR tmi.trustmarkIssuerId = :trustmarkIssuerId)
      """)
  List<TrustMark> findByOrgNumberWithSubjects(@Param("orgNumber") String orgNumber,
      @Param("trustmarkIssuerId") UUID trustmarkIssuerId);

  /**
   * Finds a {@link TrustMark} based on the organization's number and the trustmark's ID.
   *
   * @param orgNumber the unique number of the organization
   * @param trustmarkId the unique identifier of the trustmark
   * @return an {@link Optional} containing the matching {@link TrustMark} if found, otherwise an empty
   */
  @Query("""
      SELECT t FROM TrustMark t 
      JOIN t.trustmarkIssuer tmi
      JOIN tmi.entity e 
      JOIN e.organization o 
      WHERE o.orgNumber = :orgNumber 
      AND t.trustmarkId = :trustmarkId
      """)
  Optional<TrustMark> findByOrgNumberAndTrustmarkId(@Param("orgNumber") String orgNumber,
      @Param("trustmarkId") UUID trustmarkId);

  /**
   * Finds a {@link TrustMark} by the issuer's entity identifier and trust mark type.
   *
   * @param issuerEntityId the entity identifier (subject) of the trust mark issuer
   * @param trustmarkType the trust mark type URI
   * @return an {@link Optional} containing the matching {@link TrustMark} if found, otherwise empty
   */
  @Query("""
      SELECT t FROM TrustMark t
      JOIN t.trustmarkIssuer tmi
      JOIN tmi.entity e
      WHERE e.subject = :issuerEntityId
      AND t.trustmarkType = :trustmarkType
      """)
  Optional<TrustMark> findByIssuerEntityIdAndTrustmarkType(
      @Param("issuerEntityId") String issuerEntityId,
      @Param("trustmarkType") String trustmarkType);

  /**
   * Finds {@link TrustMark} records by trust mark type only.
   * Used as a fallback when the issuer entity identifier is unknown or does not match.
   * Trust mark type URIs are expected to be globally unique; multiple results indicate
   * a misconfigured registry.
   *
   * @param trustmarkType the trust mark type URI
   * @return matching trust marks (normally at most one)
   */
  @Query("""
      SELECT t FROM TrustMark t
      WHERE t.trustmarkType = :trustmarkType
      """)
  List<TrustMark> findAllByTrustmarkType(@Param("trustmarkType") String trustmarkType);
}
