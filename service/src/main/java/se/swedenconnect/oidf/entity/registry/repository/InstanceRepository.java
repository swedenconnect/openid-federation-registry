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
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing InstanceEntity objects. Extends JpaRepository to provide standard CRUD operations
 * for the InstanceEntity.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface InstanceRepository extends JpaRepository<InstanceEntity, UUID> {
  /**
   * Finds an {@link InstanceEntity} based on the useForDefaultAssignment flag.
   *
   * @param useForDefaultAssignment a boolean indicating whether the instance is used for default assignment
   * @return an {@link Optional} containing the {@link InstanceEntity} if found, or empty if no matching entity exists
   */
  Optional<InstanceEntity> findByUseForDefaultAssignment(boolean useForDefaultAssignment);

}