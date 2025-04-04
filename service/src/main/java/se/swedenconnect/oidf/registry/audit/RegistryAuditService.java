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
package se.swedenconnect.oidf.registry.audit;

import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;

import java.util.UUID;

/**
 * RegistryAuditService defines an interface for auditing specific read operations within a federation API.
 * Implementations of this interface are responsible for logging or handling details related to these read actions,
 * often for monitoring or compliance purposes. The interface provides methods to audit the following types of
 * operations: - Reading information about a federation entity. - Reading details about a trust mark associated with a
 * federation entity. - Reading a federation policy by its unique identifier.
 *
 * @author Per Fredrik Plars
 */
public interface RegistryAuditService {

  /**
   * Audits the creation of an options record. This method is invoked when a new options record is created and is used
   * for purposes such as monitoring, compliance, or debugging. It logs or handles the necessary details to track the
   * creation of the options record.
   *
   * @param optionsRecordId the unique identifier of the options record being created.
   * @param fkKeyType the type of foreign key associated with the options record.
   * @param oldRecord the previous state of the options record. Typically null during creation.
   * @param newRecord the new state of the options record after it has been created.
   */
  void optionsCreate(UUID optionsRecordId, FkKeyType fkKeyType, OptionsRecord oldRecord, OptionsRecord newRecord);

  /**
   * Audits the update operation performed on an options record. This method is invoked when an existing options record
   * is modified within the system and is used for purposes such as monitoring, compliance, or debugging. It logs or
   * handles the necessary details of the options record's state before and after the update.
   *
   * @param optionsRecordId the unique identifier of the options record being updated.
   * @param fkKeyType the type of foreign key associated with the options record.
   * @param oldRecord the previous state of the options record before the update. Can be null if not applicable.
   * @param newRecord the new state of the options record after the update.
   */
  void optionsUpdate(UUID optionsRecordId, FkKeyType fkKeyType, OptionsRecord oldRecord, OptionsRecord newRecord);

  /**
   * Logs or handles the deletion of an options record. This method is used to audit the removal of an options record
   * from the system for purposes such as monitoring, compliance, or debugging.
   *
   * @param optionsRecordId the unique identifier of the options record being deleted.
   * @param fkKeyType the type of foreign key associated with the options record.
   * @param deletedRecord the record containing the details of the deleted options record.
   */
  void optionsDelete(UUID optionsRecordId, FkKeyType fkKeyType, OptionsRecord deletedRecord);
}
