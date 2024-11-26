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
package se.swedenconnect.oidf.entity.registry.trustmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

import java.util.List;
import java.util.Optional;

/**
 * Handling trustmark CRUD opertions
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class JpaTrustMarkSubjectService implements TrustMarkSubjectService {
  private final TrustMarkSubjectRepository repository;
  private final ObjectMapper objectMapper;

  /**
   * Constructor for JpaTrustMarkSubjectService
   * @param repository TrustMarkSubjectRepository
   * @param objectMapper Objectmapper
   */
  public JpaTrustMarkSubjectService(final TrustMarkSubjectRepository repository,
      final ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  public TrustMarkSubjectRecord create(final TrustMarkSubjectRecord record) {
    try {
      return this.repository.findByExternalId(record.getTrustMarkSubjectRecordId())
          .or(() -> Optional.of( TrustMarkSubjectEntity.builder().build()))
          .map(entity -> mergeRecordIntoEntity(record,entity))
          .map(this.repository::save)
          .map(this::toRecord)
          .orElseThrow();
    }
    catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "TrustMarkSubjectRecord already exists");
    }
  }

  @Override
  public TrustMarkSubjectRecord get(final String trustMarkSubjectId) {
    return this.repository.findByExternalId(trustMarkSubjectId)
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
  public List<TrustMarkSubjectRecord> getAll(String issuer,String trustmarkId) {
    return this.repository.findByIssuerAndTrustmarkId(issuer,trustmarkId)
        .stream()
        .map(this::toRecord)
        .toList();
  }

  @Override
  public TrustMarkSubjectRecord update(final String trustMarkSubjectId, final TrustMarkSubjectRecord record) {
    if (!trustMarkSubjectId.equals(record.getTrustMarkSubjectRecordId())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trustMarkSubjectId has to match in json payload.");
    }
    return this.create(record);
  }

  @Override
  public void delete(final String trustMarkSubjectId) {
    this.repository.findByExternalId(trustMarkSubjectId).ifPresent(this.repository::delete);
  }

  private TrustMarkSubjectRecord toRecord(TrustMarkSubjectEntity entity){
    try {
      return this.objectMapper.readValue(entity.getTrustmarksubjectJson(), TrustMarkSubjectRecord.class);
    }
    catch (JsonProcessingException e) {
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
          .trustmarksubjectJson(objectMapper.writeValueAsString(record))
          .build();
      if(entity.getExternalId() == null){
        newEntity.setExternalId(record.getTrustMarkSubjectRecordId());
      }
      return newEntity;
    }
    catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to map record to TrustMarkSubjectEntity",e);
    }
  }

}
