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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OptionsCRUDSelector serves as a selector for delegating the operations defined in the OptionsCRUD interface to the
 * appropriate implementation based on the provided FkKeyType. It maintains a list of OptionsCRUD implementations and
 * determines the correct one to use for each operation.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OptionsCRUDSelector implements OptionsCRUD {

  final List<OptionsCRUD> optionsCRUDS;
  final RegistryAuditService registryAuditService;

  /**
   * Constructs a new OptionsCRUDSelector instance with the provided list of OptionsCRUD implementations and the
   * registry audit service.
   *
   * @param optionsCRUDS the list of OptionsCRUD implementations to be used for delegating operations
   * @param registryAuditService the service responsible for auditing registry operations
   */
  public OptionsCRUDSelector(final List<OptionsCRUD> optionsCRUDS, final RegistryAuditService registryAuditService) {
    this.optionsCRUDS = optionsCRUDS;
    this.registryAuditService = registryAuditService;
  }

  private OptionsCRUD getOptionsCRUD(final FkKeyType fkKeyType) {
    return this.optionsCRUDS.stream()
        .filter(optionsCRUD -> optionsCRUD.supports(fkKeyType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No optionsCRUD found for::" + fkKeyType));
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return this.getOptionsCRUD(fkKeyType).supports(fkKeyType);
  }

  @Transactional
  @Override
  public OptionsRecord create(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final OptionsRecord newRecord = this.getOptionsCRUD(fkKeyType).create(organizationRecord, fkKeyType, id, record);
    this.registryAuditService.optionsCreate(id, fkKeyType, record, newRecord);
    return newRecord;
  }

  @Transactional
  @Override
  public OptionsRecord update(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    final OptionsRecord newRecord = this.getOptionsCRUD(fkKeyType).update(organizationRecord, fkKeyType, id, record);
    this.registryAuditService.optionsUpdate(id, fkKeyType, record, newRecord);
    return newRecord;
  }

  @Override
  public OptionsRecord get(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {
    return this.getOptionsCRUD(fkKeyType).get(organizationRecord, fkKeyType, id);
  }

  @Override
  public OptionsRecord template(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType) {
    return this.getOptionsCRUD(fkKeyType).template(organizationRecord, fkKeyType);
  }

  @Override
  public OptionsRecord delete(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType, final UUID id) {

    try {
      final OptionsRecord deletedRecord = this.getOptionsCRUD(fkKeyType).delete(organizationRecord, fkKeyType, id);
      this.registryAuditService.optionsDelete(id, fkKeyType, deletedRecord);
      return deletedRecord;
    }
    catch (final DataIntegrityViolationException e) {
      throw new RegistryClientException(ErrorTypes.PARENT_HAS_CHILDREN,
          "Unable to delete entity, remove children first", e);
    }
  }

  @Override
  public List<Map<String, Object>> list(final OrganizationRecord organizationRecord,
      final FkKeyType fkKeyType) {
    return this.getOptionsCRUD(fkKeyType).list(organizationRecord, fkKeyType);
  }

  /**
   * Retrieves a map containing lists of mapped objects for each key type. The method iterates through all possible
   * FkKeyType values, retrieves the corresponding data through the `list` method, and organizes the results into a
   * map.
   * @param organizationRecord Current organization
   * @param query a string value representing the query or filter criteria (currently unused in the method's logic)
   * @return a map where the keys are the names of each FkKeyType and the values are lists of maps containing related
   *     objects
   */
  public Map<String, List<Map<String, Object>>> listAll(final OrganizationRecord organizationRecord,
      final String query) {
    final Map<String, List<Map<String, Object>>> result = new HashMap<>();
    for (FkKeyType value : FkKeyType.values()) {
      final List<Map<String, Object>> values = this.list(organizationRecord, value);
      if (values.isEmpty())
        continue;
      result.put(value.name(), values);
    }
    return result;
  }
}
