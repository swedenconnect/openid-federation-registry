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
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityRepository is a JPA repository interface for accessing and performing CRUD operations on {@link EntityEntity}
 * entities stored in the database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods for standard data access layers.
 *
 * @author David Goldring
 * @author Per Fredrik Plars
 */
public interface EntityRepository extends JpaRepository<EntityEntity, UUID> {
  /**
   * Retrieves an {@link Optional} containing an {@link EntityEntity} if an entity with the specified ID and entity type
   * exists.
   *
   * @param id the unique identifier of the entity
   * @param entityType the type of the entity, specified as {@link EntityKeyType}
   * @return an {@link Optional} containing the entity if found, or an empty {@link Optional} if not found
   */
  Optional<EntityEntity> findByEntityIdAndEntityType(UUID id, EntityKeyType entityType);

  /**
   * Retrieves an {@link Optional} containing an {@link EntityEntity} based on the specified entity type.
   *
   * @param entityType the type of the entity, specified as {@link EntityKeyType}
   * @return an {@link Optional} containing the entity if found, or an empty {@link Optional} if no match is found
   */
  List<EntityEntity> findByEntityType(EntityKeyType entityType);
}
