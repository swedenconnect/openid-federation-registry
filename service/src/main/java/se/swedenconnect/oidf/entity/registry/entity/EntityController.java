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

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.api.OidfEntityRegistryApi;
import se.swedenconnect.oidf.registry.api.model.Entity;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST Controller for handling entity registry operations.
 * <p>
 * Provides endpoints for creating, retrieving, updating, and deleting entities
 * in the registry. This controller inherits from the {@link OidfEntityRegistryApi} interface and
 * interacts with the EntityService to perform CRUD operations on entities.
 * <p>
 * The {@link Entity} object is serialized to a JSON structure and saved to some storage with the
 * {@code subject} member of the {@code Entity} object as searchable key.
 *
 * @author David Goldring
 */
@Slf4j
@RestController
@RequestMapping("/registry/v1/entities")
public class EntityController {

  private final EntityService entityService;

  /**
   * Constructs a RegistryController with a specified EntityService.
   *
   * @param entityService the entity service used for CRUD operations on entities
   */
  public EntityController(@Qualifier("jpaEntityService") final EntityService entityService) {
    this.entityService = entityService;
  }

  /**
   * Creates a new entity in the registry.
   *
   * @param entity the entity to be created
   * @param response the HTTP servlet response to set the status code
   * @return the created entity
   */
  @PostMapping
  public Entity createEntity(@RequestBody final Entity entity, final HttpServletResponse response) {
    log.debug("POST: {}", entity);
    Entity createdEntity = this.entityService.create(entity);
    response.setStatus(HttpServletResponse.SC_CREATED);

    return createdEntity;
  }

  /**
   * Retrieves all entities from the registry.
   *
   * @return a list of all entities
   */
  @GetMapping
  public List<Entity> getAllEntities() {
    List<Entity> entities = this.entityService.getAll();
    log.debug("GET: {}", entities);

    return ResponseEntity.ok(entities).getBody();
  }

  /**
   * Retrieves an entity by its identifier.
   *
   * @param entityId the unique identifier of the entity to be retrieved
   * @return the entity corresponding to the provided identifier
   */
  @GetMapping("/{entityId}")
  public Entity getEntityById(@PathVariable("entityId") final String entityId) {
    log.debug("GET: by id: {}", entityId);

    final var decoded = URLDecoder.decode(entityId, StandardCharsets.UTF_8);
    final Entity entity = this.entityService.get(decoded);
    if (entity == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
    }

    log.debug("GET result: {}", entity);

    return entity;
  }

  /**
   * Updates an existing entity with the provided entity details.
   *
   * @param entityId the unique identifier of the entity to be updated
   * @param entity the entity object containing updated details
   * @return the updated entity object
   */
  @PutMapping("/{entityId}")
  public Entity updateEntity(@PathVariable("entityId") final String entityId, @RequestBody Entity entity) {
    log.debug("PUT: {}", entity);

    final var decoded = URLDecoder.decode(entityId, StandardCharsets.UTF_8);

    return this.entityService.update(decoded, entity);
  }

  /**
   * Deletes an entity identified by the provided entity ID.
   *
   * @param entityId the unique identifier of the entity to be deleted
   * @param response the HttpServletResponse object to set the response status
   */
  @DeleteMapping("/{entityId}")
  public void deleteEntity(@PathVariable("entityId") final String entityId, final HttpServletResponse response) {
    log.debug("DELETE: {}", entityId);

    this.entityService.delete(URLDecoder.decode(entityId, StandardCharsets.UTF_8));
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

}
