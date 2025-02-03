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
import org.springframework.stereotype.Repository;
import se.swedenconnect.oidf.entity.registry.entity.ModuleEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing SettingsEntity.
 * @author Per Fredrik Plars
 */
@Repository
public interface ModuleRepository extends JpaRepository<ModuleEntity, Long> {

  /**
   * Retrieves a {@link ModuleEntity} based on the provided external ID.
   *
   * @param externalid the unique external identifier of the module to search for
   * @return an {@link Optional} containing the found {@link ModuleEntity}, or an empty {@link Optional} if no entity
   *     was found
   */
  Optional<ModuleEntity> findByExternalId(String externalid);

  /**
   * Retrieves a {@link ModuleEntity} based on the provided external ID and module type.
   *
   * @param externalid the unique external identifier of the module to search for
   * @param moduleType the type of the module to search for
   * @return an {@link Optional}*/
  Optional<ModuleEntity> findByExternalIdAndModuleType(String externalid, String moduleType);

  /**
   * Retrieves a list of {@link ModuleEntity} objects that match the specified module type.
   *
   * @param moduleType the type of modules to search for
   * @return a list of {@link ModuleEntity} objects matching the specified module type
   */
  List<ModuleEntity> findByModuleType(String moduleType);

}