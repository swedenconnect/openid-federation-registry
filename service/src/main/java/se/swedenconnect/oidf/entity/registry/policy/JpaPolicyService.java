/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

  /**
   * Construct a JpaPolicyService with the provided repository and object mapper.
   *
   * @param policyRepository the JPA repository
   * @param objectMapper the ObjectMapper used for JSON conversion between Policy strings and their DAO
   *     representations
   */
  public JpaPolicyService(final PolicyRepository policyRepository, final ObjectMapper objectMapper) {
    this.policyRepository = policyRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public PolicyRecord create(final PolicyRecord record) {
    try {
      return this.policyRepository.findByExternalId(record.getPolicyRecordId())
          .or(() -> Optional.of(new PolicyEntity()))
          .map(entity -> this.mergeRecordIntoEntity(record, entity))
          .map(this.policyRepository::save)
          .map(this::toRecord)
          .orElseThrow();
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
    this.policyRepository.findByExternalId(policyRecordId).ifPresent(this.policyRepository::delete);
  }

  private PolicyRecord toRecord(final PolicyEntity policyEntity) {
    return PolicyRecord.builder()
        .policyRecordId(policyEntity.getExternalId())
        .name(policyEntity.getName())
        .policy(policyEntity.getPolicy())
        .build();
  }

  private PolicyEntity mergeRecordIntoEntity(final PolicyRecord record, final PolicyEntity entity) {
    try {
      final JsonNode policyJson = this.objectMapper.readTree(record.getPolicy());
      entity.setPolicy(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyJson));
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
