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
package se.swedenconnect.oidf.registry.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityRepository is a JPA repository interface for accessing and performing CRUD operations on
 * {@link FederationEntity}
 * entities stored in the database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods for standard data access layers.
 *
 * @author Per Fredrik Plars
 */
public interface EntityRepository extends JpaRepository<FederationEntity, UUID> {

  /**
   * Queries the database to retrieve an optional {@link FederationEntity} based on the provided
   * organization number, entity ID, and entity type.
   *
   * @param orgNumber the organization number to filter the results
   * @param entityId the unique identifier of the entity to filter the results
   * @param entityType the type of the entity to filter the results
   * @return an {@link Optional} containing the matching {@link FederationEntity} if found, or an empty {@link Optional}
   * if no match is found
   */
  @Query("SELECT e FROM FederationEntity e JOIN fetch e.organization o "
      + "WHERE o.orgNumber = :orgNumber "
      + "AND e.entityId = :entityId "
      + "AND e.entityType = :entityType")
  Optional<FederationEntity> findByOrgNumberAndEntityIdAndEntityKeyType(
      @Param("orgNumber") String orgNumber,
      @Param("entityId") UUID entityId,
      @Param("entityType") EntityType entityType);

  /**
   * Finds a list of {@link FederationEntity} objects based on the given organization number and entity type.
   *
   * @param orgNumber the organization number used to filter the query
   * @param entityType the type of entity used to filter the query
   * @param issuer issuer
   * @return a list of {@link FederationEntity} matching the specified organization number and entity type
   */
  @Query("SELECT e FROM FederationEntity e JOIN fetch e.organization o "
      + "WHERE o.orgNumber = :orgNumber AND e.issuer = :issuer "
      + "AND e.entityType = :entityType")
  Optional<FederationEntity> findByOrgNumberAndEntityKeyTypeAndIssuer(
      @Param("orgNumber") String orgNumber,
      @Param("entityType") EntityType entityType,
      @Param("issuer") String issuer
  );

  /**
   * Finds a list of {@link FederationEntity} objects based on the given organization number and entity type.
   *
   * @param orgNumber the organization number used to filter the query
   * @param entityType the type of entity used to filter the query
   * @param subject subject
   * @return a list of {@link FederationEntity} matching the specified organization number and entity type
   */
  @Query("SELECT e FROM FederationEntity e JOIN fetch e.organization o "
      + "WHERE o.orgNumber = :orgNumber AND e.subject = :subject "
      + "AND e.entityType = :entityType")
  Optional<FederationEntity> findByOrgNumberAndEntityKeyTypeAndSubject(
      @Param("orgNumber") String orgNumber,
      @Param("entityType") EntityType entityType,
      @Param("subject") String subject
  );

  /**
   * Finds all {@link FederationEntity} objects for the given organization number, optionally filtered by entity type.
   *
   * @param orgNumber the organization number used to filter the query
   * @param entityType the type of entity used to filter the query (optional, null means all types)
   * @return a list of {@link FederationEntity} matching the specified organization number and optional entity type
   */
  @Query("SELECT DISTINCT e FROM FederationEntity e JOIN fetch e.organization o "
      + "LEFT JOIN fetch e.trustanchorIntermediate m "
      + "LEFT JOIN fetch e.resolver r "
      + "LEFT JOIN fetch e.trustmarkIssuer tmi "
      + "WHERE o.orgNumber = :orgNumber "
      + "AND (:entityType IS NULL OR e.entityType = :entityType)")
  List<FederationEntity> findByOrgNumberAndOptionalEntityKeyType(
      @Param("orgNumber") String orgNumber,
      @Param("entityType") EntityType entityType);

  /**
   * Finds hosted entities by entity type and optional issuer (entityIdentifier), across all organizations.
   *
   * @param entityType the entity type to filter by
   * @param issuer the issuer to filter by, or {@code null} to return all of the given type
   * @return matching entities
   */
  @Query("SELECT e FROM FederationEntity e JOIN fetch e.organization o "
      + "WHERE e.entityType = :entityType "
      + "AND (:issuer IS NULL OR e.issuer = :issuer)")
  List<FederationEntity> findByEntityTypeAndOptionalIssuer(
      @Param("entityType") EntityType entityType,
      @Param("issuer") String issuer);
}
