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
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityRepository is a JPA repository interface for accessing and performing CRUD operations on
 * {@link TrustMarkSubject} entities stored in a database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods for standard data access layers.
 *
 * @author Per Fredrik Plars
 */
public interface TrustMarkSubjectRepository extends JpaRepository<TrustMarkSubject, UUID> {

  /**
   * Executes the SQL query to retrieve trust mark subjects based on the organization number.
   *
   * @param orgNumber the organization number
   * @return a list of TrustMarkSubject
   */
  @Query("SELECT ts FROM TrustMarkSubject ts "
      + "JOIN ts.trustMark tm "
      + "JOIN tm.trustmarkIssuer tmi "
      + "JOIN tmi.entity e "
      + "JOIN e.organization o "
        + "WHERE o.orgNumber = :orgNumber")
  List<TrustMarkSubject> findByOrgNumber(@Param("orgNumber") String orgNumber);

  /**
   * Retrieves a {@link TrustMark} based on the organization's unique number and the trust mark subject's unique
   * identifier.
   *
   * @param orgNumber the unique number identifying the organization
   * @param trustmarksubjectId the unique identifier for the trust mark subject
   * @return an {@link Optional} containing the {@link TrustMark} if found, otherwise empty
   */
  @Query("SELECT ts FROM TrustMarkSubject ts "
      + "JOIN ts.trustMark tm "
      + "JOIN tm.trustmarkIssuer tmi "
      + "JOIN tmi.entity e "
      + "JOIN e.organization o "
      + "WHERE o.orgNumber = :orgNumber AND ts.trustmarksubjectId = :id")
  Optional<TrustMarkSubject> findByOrgNumberAndTrustmarkId(@Param("orgNumber") String orgNumber,
      @Param("id") UUID trustmarksubjectId);

  /**
   * Finds an existing subject entry for a given trust mark and subject entity identifier.
   *
   * @param trustmarkId the trust mark ID
   * @param subject the subject entity identifier
   * @return an {@link Optional} containing the matching subject if found
   */
  Optional<TrustMarkSubject> findByTrustMarkTrustmarkIdAndSubject(UUID trustmarkId, String subject);

  /**
   * Finds all trust mark subjects added via a specific registration.
   *
   * @param registrationId the registration ID
   * @return subjects linked to that registration
   */
  List<TrustMarkSubject> findByRegistrationRegistrationId(UUID registrationId);

}
