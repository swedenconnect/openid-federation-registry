/*
 * Copyright 2024 Sweden Connect.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.oidf.entity.registry.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.Entity;

import java.util.List;

/**
 * JpaEntityService is an implementation of the EntityService interface that uses a JPA repository
 * to perform CRUD operations on Entity objects.
 * <p>
 * This service utilizes the ObjectMapper from the Jackson library to handle JSON conversion
 * between Entity objects and their DAO ({@link EntityEntity}) representations.
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
   * @param objectMapper the ObjectMapper used for JSON conversion between Entity objects and their DAO representations
   */
  public JpaEntityService(final EntityRepository repository, final ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  public Entity create(final Entity entity) {
    final var dao = new EntityEntity();
    try {
      dao.setIssuer(entity.getIssuer());
      dao.setSubject(entity.getSubject());
      dao.setEntity(this.objectMapper.writeValueAsString(entity));
      this.repository.save(dao);
    }
    catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON: " + e.getMessage());
    }
    catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists:");
    }
    return entity;
  }

  @Override
  public Entity get(final String subject) {
    return this.repository.findBySubject(subject)
        .map(dao -> {
          try {
            return this.objectMapper.readValue(dao.getEntity(), Entity.class);
          }
          catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .orElse(null);
  }

  @Override
  public List<Entity> getAll() {
    return this.repository.findAll()
        .stream().map(dao -> {
          try {
            return this.objectMapper.readValue(dao.getEntity(), Entity.class);
          }
          catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        }).toList();
  }

  @Override
  public Entity update(final String subject, final Entity entity) {
    final var dao = this.repository.findBySubject(subject).orElse(null);
    if (dao != null) {
      try {
        dao.setSubject(subject);
        dao.setEntity(this.objectMapper.writeValueAsString(entity));
        this.repository.save(dao);
        return entity;
      }
      catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Override
  public void delete(final String subject) {
    this.repository.findBySubject(subject).ifPresent(this.repository::delete);
  }
}
