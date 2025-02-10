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

import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.UUID;

/**
 * Interface defining operations for managing entity records based on FkKeyType.
 *
 * @author Per Fredrik Plars
 */
public interface OptionsCRUD {

  /**
   * Determines if the specified foreign key type is supported.
   *
   * @param fkKeyType the type of foreign key to check
   * @return true if the foreign key type is supported, false otherwise
   */
  boolean supports(FkKeyType fkKeyType);

  /**
   * Creates a new entity record.
   *
   * @param fkKeyType the type of foreign key
   * @param id the unique identifier
   * @param record the data record to create
   * @return the created options record
   */
  OptionsRecord create(FkKeyType fkKeyType, UUID id, OptionsRecord record);

  /**
   * Updates an existing entity record.
   *
   * @param fkKeyType the type of foreign key
   * @param id the unique identifier
   * @param record the data record with updates
   * @return the updated options record
   */
  OptionsRecord update(FkKeyType fkKeyType, UUID id, OptionsRecord record);

  /**
   * Loads an entity record.
   *
   * @param fkKeyType the type of foreign key
   * @param id the unique identifier
   * @return the loaded options record
   */
  OptionsRecord get(FkKeyType fkKeyType, UUID id);

  /**
   * Retrieves the template for a specific entity type.
   *
   * @param fkKeyType the type of foreign key
   * @return the template options record
   */
  OptionsRecord template(FkKeyType fkKeyType);

  /**
   * Deletes an entity record identified by a foreign key type and unique identifier.
   *
   * @param fkKeyType the type of foreign key associated with the record to be deleted
   * @param id the unique identifier of the record to delete
   * @return the deleted options record
   */
  OptionsRecord delete(FkKeyType fkKeyType, UUID id);
}