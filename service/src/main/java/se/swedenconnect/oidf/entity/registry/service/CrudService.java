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
package se.swedenconnect.oidf.entity.registry.service;

import java.util.List;

/**
 * A service interface for managing entities of type T.
 *
 * @param <T> the type of the entity
 * @param <ID> the type of the entity's identifier
 * @author David Goldring
 */
public interface CrudService<T, ID> {

  /**
   * Creates a new entity in the system.
   *
   * @param entity the entity to be created
   * @return the created entity
   */
  T create(T entity);

  /**
   * Retrieves an entity by its identifier.
   *
   * @param id the identifier of the entity to be retrieved
   * @return the entity with the specified identifier or null if not found
   */
  T get(ID id);

  /**
   * Retrieves all entities.
   *
   * @return a list of all entities
   */
  List<T> getAll();

  /**
   * Updates an existing entity identified by its ID.
   *
   * @param id the identifier of the entity to be updated
   * @param entity the entity with updated information
   * @return the updated entity
   */
  T update(ID id, T entity);

  /**
   * Deletes the entity with the specified identifier.
   *
   * @param id the identifier of the entity to be deleted
   */
  void delete(ID id);
}
