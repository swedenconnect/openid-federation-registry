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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaEntityService is an implementation of the EntityService interface that uses a JPA repository to perform CRUD
 * operations on Entity objects.
 * <p>
 * This service utilizes the ObjectMapper from the Jackson library to handle JSON conversion between Entity objects and
 * their DAO ({@link EntityEntity}) representations.
 *
 * @author David Goldring
 */
public class JpaEntityService implements EntityService {

  @Getter
  private final EntityRepository repository;
  private final PolicyRepository policyRepository;
  private final ObjectMapper objectMapper;
  private final RegistryAuditService registryAuditService;

  /**
   * Constructs a new instance of {@code JpaEntityService} with the specified dependencies.
   *
   * @param repository the {@link EntityRepository} used for CRUD operations on entities.
   * @param policyRepository the {@link PolicyRepository} used for accessing and managing policy entities.
   * @param objectMapper the {@link ObjectMapper} responsible for handling JSON serialization and deserialization.
   * @param registryAuditService the {@link RegistryAuditService} used for auditing operations related to entities and
   * policies.
   */
  public JpaEntityService(final EntityRepository repository, final PolicyRepository policyRepository,
      final ObjectMapper objectMapper,
      final RegistryAuditService registryAuditService) {
    this.repository = repository;
    this.policyRepository = policyRepository;
    this.objectMapper = objectMapper;
    this.registryAuditService = registryAuditService;

  }

  @Override
  public EntityRecord create(final EntityRecord record) {
    this.verifyPolicyRecordId(record);
    try {
      final EntityRecord result = this.repository.findByExternalId(record.getEntityRecordId())
          .or(() -> Optional.of(new EntityEntity()))
          .map(entity -> this.mergeRecordIntoEntity(record, entity))
          .map(this.repository::save)
          .map(this::toRecord)
          .orElseThrow();
      this.registryAuditService.entityWrite(result.getEntityRecordId(),record,result);
      return result;
    }
    catch (final DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Issuer and Subject already exist");
    }
  }

  @Override
  public EntityRecord get(final String entityRecordId) {
    return this.repository.findByExternalId(entityRecordId)
        .map(this::toRecord)
        .orElse(null);
  }

  @Override
  public List<EntityRecord> getAll() {
    return this.repository.findAll()
        .stream()
        .map(this::toRecord)
        .toList();
  }

  @Override
  public EntityRecord update(final String entityRecordId, final EntityRecord entityRecord) {
    if (!entityRecordId.equals(entityRecord.getEntityRecordId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EntityRecordId has to match in json payload.");
    }
    return this.create(entityRecord);

  }

  @Override
  public void delete(final String entityRecordId) {
    this.repository.findByExternalId(entityRecordId)
        .map(entityEntity -> {
          this.registryAuditService.entityDelete(entityRecordId,this.toRecord(entityEntity));
          return entityEntity;
        }).ifPresent(this.repository::delete);
  }

  private void verifyPolicyRecordId(final EntityRecord record) {
    Assert.notNull(record,"Record can not be null");
    Assert.hasText(record.getPolicyRecordId(),"PolicyRecordId has to be present");
    if (this.policyRepository.findById(UUID.fromString(record.getPolicyRecordId())).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PolicyRecordId is not found.");
    }
  }

  private EntityRecord toRecord(final EntityEntity entity) {
    try {
      return this.objectMapper.readValue(entity.getEntity(), EntityRecord.class);
    }
    catch (final JsonProcessingException e) {
      throw new RuntimeException("Unable to map json entity to record", e);
    }
  }

  private EntityEntity mergeRecordIntoEntity(final EntityRecord record, final EntityEntity entity) {
    try {
      entity.setIssuer(record.getIssuer());
      entity.setSubject(record.getSubject());
      entity.setEntity(this.objectMapper.writeValueAsString(record));
      if (entity.getExternalId() == null) {
        entity.setExternalId(record.getEntityRecordId());
      }
      return entity;
    }
    catch (final JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to map record to entity", e);
    }
  }
}
