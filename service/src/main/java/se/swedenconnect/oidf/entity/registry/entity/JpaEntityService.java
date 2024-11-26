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
package se.swedenconnect.oidf.entity.registry.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;

import java.util.List;

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
  private final ObjectMapper objectMapper;

  /**
   * Constructs a JpaEntityService with the provided repository and object mapper.
   *
   * @param repository the JPA repository used for CRUD operations on Entity objects
   * @param objectMapper the ObjectMapper used for JSON conversion between Entity objects and their DAO
   *     representations
   */
  public JpaEntityService(final EntityRepository repository, final ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  public EntityRecord create(final EntityRecord entityRecord) {
    final EntityEntity entity = new EntityEntity();
    try {
      entity.setIssuer(entityRecord.getIssuer());
      entity.setSubject(entityRecord.getSubject());
      entity.setEntity(this.objectMapper.writeValueAsString(entityRecord));
      final EntityEntity savedEntity = this.repository.save(entity);
      entityRecord.setEntityId(savedEntity.getExternalId());
      return entityRecord;
    }
    catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON: " + e.getMessage());
    }
    catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Issuer and Subject already exist");
    }
  }

  @Override
  public EntityRecord get(final String entityid) {
    return this.repository.findByExternalId(entityid)
        .map(entity -> {
          try {
            return this.objectMapper.readValue(entity.getEntity(), EntityRecord.class);
          }
          catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .orElse(null);
  }

  @Override
  public List<EntityRecord> getAll() {
    return this.repository.findAll()
        .stream().map(entity -> {
          try {
            return this.objectMapper.readValue(entity.getEntity(), EntityRecord.class);
          }
          catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        }).toList();
  }

  @Override
  public EntityRecord update(final String entityid, final EntityRecord entityRecord) {
    if (!entityid.equals(entityRecord.getEntityId())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entityid has to match in json payload.");
    }

    final EntityEntity entity = this.repository.findByExternalId(entityid).orElse(null);
    if (entity == null) {
      return null;
    }
    try {
      if (!entityRecord.getIssuer().equals(entity.getIssuer())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not change issuer");
      }
      if (!entityRecord.getSubject().equals(entity.getSubject())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not change subject");
      }
      entity.setEntity(this.objectMapper.writeValueAsString(entityRecord));
      this.repository.save(entity);
      return entityRecord;
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(final String entityid) {
    this.repository.findByExternalId(entityid).ifPresent(this.repository::delete);
  }
}
