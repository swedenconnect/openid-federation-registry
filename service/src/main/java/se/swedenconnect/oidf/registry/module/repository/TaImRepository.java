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
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing TrustAnchorIntermediateModule.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface TaImRepository extends JpaRepository<TrustAnchorIntermediateModule, UUID> {

  /**
   * Retrieves a TrustAnchorIntermediateModule entity based on the given organization ID, TaIm ID, and module type.
   *
   * @param organizationId the organization ID associated with the module
   * @param taImId the unique identifier of the module
   * @param moduleType the type of the module
   * @return an Optional containing the TrustAnchorIntermediateModule if found, otherwise an empty Optional
   */
  @Query("SELECT m FROM TrustAnchorIntermediateModule m JOIN m.organization o "
      + "WHERE o.organizationId = :organizationId AND m.moduleType = :moduleType AND m.taImId=:taImId")
  Optional<TrustAnchorIntermediateModule> findByOrganizationIdAndTaImIdAndModuleType(
      @Param("organizationId") UUID organizationId,
      @Param("taImId") UUID taImId,
      @Param("moduleType") ModuleType moduleType);

  /**
   * Retrieves a TrustAnchorIntermediateModule entity based on the given organization ID, TaIm ID
   *
   * @param organizationId the organization ID associated with the module
   * @param taImId the unique identifier of the module
   * @return an Optional containing the TrustAnchorIntermediateModule if found, otherwise an empty Optional
   */
  @Query("SELECT m FROM TrustAnchorIntermediateModule m JOIN m.organization o "
      + "WHERE o.organizationId = :organizationId  AND m.taImId=:taImId")
  Optional<TrustAnchorIntermediateModule> findByOrganizationIdAndTaImId(
      @Param("organizationId") UUID organizationId,
      @Param("taImId") UUID taImId);

  /**
   * Retrieves a list of TrustAnchorIntermediateModule objects associated with the specified organization ID and
   * module type.
   *
   * @param organizationId the organization ID to filter by.
   * @param moduleType the type of module to filter by.
   * @return a list of TrustAnchorIntermediateModule objects matching the specified organization ID and module type.
   */
  @Query("SELECT m FROM TrustAnchorIntermediateModule m JOIN m.organization o "
      + "WHERE o.organizationId = :organizationId AND m.moduleType = :moduleType")
  List<TrustAnchorIntermediateModule> findByOrganizationIdAndModuleType(
      @Param("organizationId") UUID organizationId, @Param("moduleType") ModuleType moduleType);

  /**
   * Retrieves a list of TrustAnchorIntermediateModule objects associated with the specified organization ID,
   * optionally filtered by module type.
   *
   * @param organizationId the organization ID to filter by.
   * @param moduleType the type of module to filter by (optional, null means all types).
   * @return a list of TrustAnchorIntermediateModule objects matching the specified organization ID and optional
   *     module type.
   */
  @Query("SELECT m FROM TrustAnchorIntermediateModule m JOIN m.organization o "
      + "WHERE o.organizationId = :organizationId "
      + "AND (:moduleType IS NULL OR m.moduleType = :moduleType)")
  List<TrustAnchorIntermediateModule> findByOrganizationIdAndOptionalModuleType(
      @Param("organizationId") UUID organizationId, @Param("moduleType") ModuleType moduleType);
}
