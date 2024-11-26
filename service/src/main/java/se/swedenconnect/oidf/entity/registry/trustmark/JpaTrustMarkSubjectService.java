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
  public TrustMarkSubjectRecord create(final TrustMarkSubjectRecord trustMarkSubjectRecord) {
    try {
      final TrustMarkSubjectEntity entity = TrustMarkSubjectEntity.builder()
          .issuer(trustMarkSubjectRecord.getIssuer())
          .trustmarkId(trustMarkSubjectRecord.getTrustMarkId())
          .subject(trustMarkSubjectRecord.getSubject())
          .trustmarksubjectJson(objectMapper.writeValueAsString(trustMarkSubjectRecord))
          .build();

      final TrustMarkSubjectEntity trustMarkSubjectEntity = repository.save(entity);
      return trustMarkSubjectRecord.toBuilder()
          .trustMarkSubjectId(trustMarkSubjectEntity.getExternalId())
          .build();
    }
    catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON: " + e.getMessage());
    }
    catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "TrustMarkSubjectRecord already exists:");
    }
  }

  @Override
  public TrustMarkSubjectRecord get(final String trustMarkSubjectId) {
    final TrustMarkSubjectEntity entity = this.repository.findByExternalId(trustMarkSubjectId).orElse(null);
    if (entity == null) {
      return null;
    }
    try {
      return this.objectMapper.readValue(entity.getTrustmarksubjectJson(), TrustMarkSubjectRecord.class);
    }
    catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON: " + e.getMessage());
    }

  }

  @Override
  public List<TrustMarkSubjectRecord> getAll() {
    throw new IllegalArgumentException("Method is not implemented");
  }

  /**
   * Getting all TrustMarkSubjectRecord for issuer and trustmarkid
   * @param issuer Issuer entityid
   * @param trustmarkId TrustmarkId
   * @return List of TrustMarkSubjectRecord
   */
  public List<TrustMarkSubjectRecord> getAll(String issuer,String trustmarkId) {
    return this.repository.findByIssuerAndTrustmarkId(issuer,trustmarkId)
        .stream().map(dao -> {
          try {
            return this.objectMapper.readValue(dao.getTrustmarksubjectJson(), TrustMarkSubjectRecord.class);
          }
          catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON: " + e.getMessage());
          }
        }).toList();
  }

  @Override
  public TrustMarkSubjectRecord update(final String trustMarkSubjectId, final TrustMarkSubjectRecord record) {
    final TrustMarkSubjectEntity entity = this.repository.findByExternalId(trustMarkSubjectId).orElse(null);
    if (entity != null) {
      try {
        if (record.getIssuer() == null || record.getIssuer().equals(entity.getIssuer())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not change issuer");
        }
        if (record.getSubject() == null || record.getSubject().equals(entity.getSubject())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not change subject");
        }
        if (record.getTrustMarkId() == null || record.getTrustMarkId().equals(entity.getTrustmarkId())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not change trustmark");
        }

        entity.setTrustmarksubjectJson(this.objectMapper.writeValueAsString(entity));
        this.repository.save(entity);
        return record;
      }
      catch (JsonProcessingException e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON: " + e.getMessage());
      }
    }
    return null;
  }

  @Override
  public void delete(final String trustMarkSubjectId) {
    this.repository.findByExternalId(trustMarkSubjectId).ifPresent(this.repository::delete);
  }

}
