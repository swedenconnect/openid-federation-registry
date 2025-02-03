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
package se.swedenconnect.oidf.entity.registry.trustmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.jpaentity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

import java.util.List;
import java.util.Optional;

/**
 * Handling trustmark CRUD operations
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class JpaTrustMarkSubjectService implements TrustMarkSubjectService {
  private final TrustMarkSubjectRepository repository;
  private final ObjectMapper objectMapper;
  private final RegistryAuditService registryAuditService;

  /**
   * JpaTrustMarkSubjectService is an implementation of the TrustMarkSubjectService interface,
   * responsible for managing TrustMarkSubjectRecord entities in the database.
   * This service handles CRUD operations and additional queries for TrustMarkSubject entities.
   *
   * @param repository the repository used for database operations on TrustMarkSubject entities
   * @param objectMapper the object mapper for handling JSON serialization and deserialization
   * @param registryAuditService the service used for auditing and logging registry actions
   */
  public JpaTrustMarkSubjectService(final TrustMarkSubjectRepository repository,
      final ObjectMapper objectMapper,
      final RegistryAuditService registryAuditService) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.registryAuditService = registryAuditService;
  }

  @Override
  public TrustMarkSubjectRecord create(final TrustMarkSubjectRecord record) {
    try {
      final TrustMarkSubjectRecord result = this.repository.findByExternalId(record.getTrustMarkSubjectRecordId())
          .or(() -> Optional.of(TrustMarkSubjectEntity.builder().build()))
          .map(entity -> this.mergeRecordIntoEntity(record,entity))
          .map(this.repository::save)
          .map(this::toRecord)
          .orElseThrow();
      this.registryAuditService.trustmarkSubjectWrite(result.getTrustMarkSubjectRecordId(),record,result);
      return result;
    }
    catch (final DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "TrustMarkSubjectRecord already exists");
    }
  }

  @Override
  public TrustMarkSubjectRecord get(final String trustMarkSubjectRecordId) {
    return this.repository.findByExternalId(trustMarkSubjectRecordId)
        .map(this::toRecord)
        .orElse(null);
  }

  @Override
  public List<TrustMarkSubjectRecord> getAll() {
    return this.repository.findAll()
        .stream()
        .map(this::toRecord)
        .toList();
  }

  /**
   * Getting all TrustMarkSubjectRecord for issuer and trustmarkid
   * @param issuer Issuer entityid
   * @param trustmarkId TrustmarkId
   * @return List of TrustMarkSubjectRecord
   */
  public List<TrustMarkSubjectRecord> getAll(final String issuer, final String trustmarkId) {
    return this.repository.findByIssuerAndTrustmarkId(issuer,trustmarkId)
        .stream()
        .map(this::toRecord)
        .toList();
  }

  @Override
  public TrustMarkSubjectRecord update(final String trustMarkSubjectRecordId, final TrustMarkSubjectRecord record) {
    if (!trustMarkSubjectRecordId.equals(record.getTrustMarkSubjectRecordId())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "TrustMarkSubjectRecordId has to match in json payload.");
    }
    return this.create(record);
  }

  @Override
  public void delete(final String trustMarkSubjectRecordId) {
    this.repository.findByExternalId(trustMarkSubjectRecordId)
        .map(trustMarkSubjectEntity -> {
          this.registryAuditService
              .trustmarkSubjectDelete(trustMarkSubjectRecordId,this.toRecord(trustMarkSubjectEntity));
          return trustMarkSubjectEntity;
        })
        .ifPresent(this.repository::delete);
  }

  private TrustMarkSubjectRecord toRecord(final TrustMarkSubjectEntity entity){
    try {
      return this.objectMapper.readValue(entity.getTrustmarksubjectJson(), TrustMarkSubjectRecord.class);
    }
    catch (final JsonProcessingException e) {
      throw new RuntimeException("Unable to map json entity to record",e);
    }
  }

  private TrustMarkSubjectEntity mergeRecordIntoEntity(
      final TrustMarkSubjectRecord record,
      final TrustMarkSubjectEntity entity){

    try {
      final TrustMarkSubjectEntity newEntity = entity.toBuilder()
          .issuer(record.getIssuer())
          .trustmarkId(record.getTrustMarkId())
          .subject(record.getSubject())
          .trustmarksubjectJson(this.objectMapper.writeValueAsString(record))
          .build();
      if(entity.getExternalId() == null){
        newEntity.setExternalId(record.getTrustMarkSubjectRecordId());
      }
      return newEntity;
    }
    catch (final JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to map record to TrustMarkSubjectEntity",e);
    }
  }

}
