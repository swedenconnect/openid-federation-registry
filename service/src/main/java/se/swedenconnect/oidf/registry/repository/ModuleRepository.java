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
import org.springframework.stereotype.Repository;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing ModuleRepository.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID> {

  /**
   * Retrieves a module entity based on the given organization number, module ID, and module type.
   *
   * @param orgNumber the organization number associated with the module
   * @param moduleId the unique identifier of the module
   * @param moduleType the type of the module
   * @return an Optional containing the ModuleEntity if found, otherwise an empty Optional
   */
  @Query("SELECT m FROM ModuleEntity m JOIN m.organization o "
      + "WHERE o.orgNumber = :orgNumber AND m.moduleType = :moduleType AND m.moduleId=:moduleId")
  Optional<ModuleEntity> findByOrgNumberAndModuleIdAndModuleType(
      @Param("orgNumber") String orgNumber, @Param("moduleId") UUID moduleId, @Param("moduleType") String moduleType);

  /**
   * Retrieves a list of ModuleEntity objects associated with the specified organization number
   * and module type.
   *
   * @param orgNumber the organization number to filter by.
   * @param moduleType the type of module to filter by.
   * @return a list of ModuleEntity objects matching the specified organization number and module type.
   */
  @Query("SELECT m FROM ModuleEntity m JOIN m.organization o "
      + "WHERE o.orgNumber = :orgNumber AND m.moduleType = :moduleType")
  List<ModuleEntity> findByOrgNumberAndModuleType(
      @Param("orgNumber") String orgNumber, @Param("moduleType") String moduleType);
}