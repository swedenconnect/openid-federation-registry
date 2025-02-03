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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;
import java.util.Optional;


/**
 * JpaPolicyService is an implementation of the {@link PolicyService} interface that uses JPA for managing JSON Policy
 * objects in the database.
 *
 * <ul>
 *   <li>Provide implementation for CRUD operations on policies stored as JSON objects.</li>
 *   <li>Validate policy content before performing operations.</li>
 * </ul>
 *
 * This service utilizes the ObjectMapper from the Jackson library to handle JSON conversion
 * between Entity objects and their DAO ({@link PolicyEntity}) representations.
 *
 * @author David Goldring
 */
@Slf4j
public class JpaPolicyService implements PolicyService {

  @Getter
  private final PolicyRepository policyRepository;
  private final ObjectMapper objectMapper;
  private final RegistryAuditService auditService;
  /**
   * Constructs a new instance of {@code JpaPolicyService} which handles policy-related operations
   * using a JPA repository. This class is responsible for interacting with the data layer and
   * performing policy CRUD (Create, Read, Update, Delete) operations.
   *
   * @param policyRepository the {@link PolicyRepository} used for accessing and modifying policy data
   * @param objectMapper the {@link ObjectMapper} used for JSON serialization and deserialization of policy objects
   * @param auditService the {@link RegistryAuditService} used for logging and auditing operations
   */
  public JpaPolicyService(final PolicyRepository policyRepository,
      final ObjectMapper objectMapper,
      final RegistryAuditService auditService) {
    this.policyRepository = policyRepository;
    this.objectMapper = objectMapper;
    this.auditService = auditService;
  }

  @Override
  public PolicyRecord create(final PolicyRecord record) {
    try {

      final PolicyRecord result = this.policyRepository.findByExternalId(record.getPolicyRecordId())
          .or(() -> Optional.of(new PolicyEntity()))
          .map(entity -> this.mergeRecordIntoEntity(record, entity))
          .map(this.policyRepository::save)
          .map(this::toRecord)
          .orElseThrow();
      this.auditService.policyWrite(result.getPolicyRecordId(),record,result);
      return result;
    }
    catch (final DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "TrustMarkSubjectRecord already exists");
    }
  }

  @Override
  public PolicyRecord get(final String policyRecordId) {
    return this.policyRepository.findByExternalId(policyRecordId)
        .map(this::toRecord)
        .orElse(null);
  }

  @Override
  public List<PolicyRecord> getAll() {
    return this.policyRepository.findAll()
        .stream()
        .map(this::toRecord)
        .toList();
  }

  @Override
  public PolicyRecord update(final String policyRecordId, final PolicyRecord record) {
    if (!policyRecordId.equals(record.getPolicyRecordId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PolicyRecordId has to be the same in path and object");
    }
    return this.create(record);
  }

  @Override
  public void delete(final String policyRecordId) {
    this.policyRepository.findByExternalId(policyRecordId)
        .map(policyEntity -> {
          this.auditService.policyDelete(policyRecordId, this.toRecord(policyEntity));
          return policyEntity;
        })
        .ifPresent(this.policyRepository::delete);
  }

  private PolicyRecord toRecord(final PolicyEntity policyEntity) {
    try {
      return this.objectMapper.readValue(policyEntity.getPolicy(), PolicyRecord.class);
    }
    catch (final JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to map record to entity", e);
    }
  }

  private PolicyEntity mergeRecordIntoEntity(final PolicyRecord record, final PolicyEntity entity) {
    try {
      entity.setPolicy(this.objectMapper.writeValueAsString(record));
      entity.setName(record.getName());
      entity.setExternalId(record.getPolicyRecordId());
      if (entity.getExternalId() == null) {
        entity.setExternalId(record.getPolicyRecordId());
      }
      return entity;
    }
    catch (final JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to parse json, in policy record ", e);
    }
  }

}
