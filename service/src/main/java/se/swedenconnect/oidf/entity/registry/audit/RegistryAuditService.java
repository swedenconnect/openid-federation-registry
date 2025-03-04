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
package se.swedenconnect.oidf.entity.registry.audit;

import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

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
   * Audits a write operation performed on a federation policy. This method is invoked when a policy is created,
   * updated, or modified. It logs or handles the event to ensure monitoring, compliance, or debugging purposes are
   * met.
   *
   * @param policyId the unique identifier of the policy being written or updated.
   * @param oldRecord the previous state of the policy. Can be null if the policy is being created.
   * @param newRecord the new state of the policy after the write operation.
   */
  void policyWrite(String policyId, PolicyRecord oldRecord, PolicyRecord newRecord);

  /**
   * Logs or handles the deletion of a federation policy. This method is used to audit the removal of a federation
   * policy from the system, often for purposes such as monitoring, compliance, or debugging.
   *
   * @param policyId the unique identifier of the federation policy being deleted.
   * @param deletedRecord the record containing the details of the deleted federation policy.
   */
  void policyDelete(String policyId, PolicyRecord deletedRecord);

  /**
   * Audits a write operation performed on an entity. This method handles or logs the event when an entity is created,
   * updated, or modified within the system for purposes such as monitoring, compliance, or debugging.
   *
   * @param entityId the unique identifier of the entity being written or updated.
   * @param oldRecord the previous state of the entity. Can be null if the entity is being created.
   * @param newRecord the new state of the entity after the write operation.
   */
  void entityWrite(String entityId, EntityRecord oldRecord, EntityRecord newRecord);

  /**
   * Logs or handles the deletion of an entity. This method is used to audit the removal of an entity within the system
   * for purposes such as monitoring, compliance, or debugging.
   *
   * @param entityId the unique identifier of the entity being deleted.
   * @param deletedRecord the record containing the details of the deleted entity.
   */
  void entityDelete(String entityId, EntityRecord deletedRecord);

  /**
   * Audits a write operation performed on a trustmark subject record. This method is used to log or handle the event
   * when a trustmark subject record is created, updated, or modified for purposes such as monitoring, compliance, or
   * debugging.
   *
   * @param trustMarkSubjectRecordId the unique identifier of the trustmark subject record being written or
   *     updated.
   * @param oldRecord the previous state of the trustmark subject record. Can be null if the record is being
   *     created.
   * @param newRecord the new state of the trustmark subject record after the write operation.
   */
  void trustmarkSubjectWrite(String trustMarkSubjectRecordId,
      TrustMarkSubjectRecord oldRecord,
      TrustMarkSubjectRecord newRecord);

  /**
   * This method is used to audit the removal of a trustmark subject record for purposes such as monitoring, compliance,
   * or debugging.
   *
   * @param trustMarkSubjectRecordId the unique identifier of the trustmark subject record being deleted.
   * @param deletedRecord the record containing the details of the deleted trustmark subject.
   */
  void trustmarkSubjectDelete(String trustMarkSubjectRecordId, TrustMarkSubjectRecord deletedRecord);

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
