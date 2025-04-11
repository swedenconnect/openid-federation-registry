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
package se.swedenconnect.oidf.registry.service;

import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;

import java.util.List;
import java.util.Map;
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
   * @param organizationRecord Current organization
   * @return the created options record
   */
  OptionsRecord create(OrganizationRecord organizationRecord, FkKeyType fkKeyType, UUID id, OptionsRecord record);

  /**
   * Updates an existing entity record.
   *
   * @param fkKeyType the type of foreign key
   * @param id the unique identifier
   * @param record the data record with updates
   * @param organizationRecord Current organization
   * @return the updated options record
   */
  OptionsRecord update(OrganizationRecord organizationRecord, FkKeyType fkKeyType, UUID id, OptionsRecord record);

  /**
   * Loads an entity record.
   *
   * @param fkKeyType the type of foreign key
   * @param id the unique identifier
   * @param organizationRecord Current organization
   * @return the loaded options record
   */
  OptionsRecord get(OrganizationRecord organizationRecord, FkKeyType fkKeyType, UUID id);

  /**
   * Retrieves the template for a specific entity type.
   *
   * @param fkKeyType the type of foreign key
   * @param organizationRecord Current organization
   * @return the template options record
   */
  OptionsRecord template(OrganizationRecord organizationRecord, FkKeyType fkKeyType);

  /**
   * Deletes an entity record identified by a foreign key type and unique identifier.
   *
   * @param fkKeyType the type of foreign key associated with the record to be deleted
   * @param id the unique identifier of the record to delete
   * @param organizationRecord Current organization
   * @return the deleted options record
   */
  OptionsRecord delete(OrganizationRecord organizationRecord, FkKeyType fkKeyType, UUID id);

  /**
   * Lists the available options records for the specified foreign key type.
   *
   * @param fkKeyType the type of foreign key for which options records are to be listed
   * @param organizationRecord Current organization
   * @return the options record containing the available options
   */
  List<Map<String, Object>> list(OrganizationRecord organizationRecord, FkKeyType fkKeyType);
}