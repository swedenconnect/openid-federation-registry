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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.policy.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.policy.PolicyRepository;
import se.swedenconnect.oidf.entity.util.EntityFactory;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link JpaEntityService} class.
 * <p>
 * This class is responsible for testing the various methods and functionalities of the {@link JpaEntityService} class
 * using mocked dependencies. The tests ensure the correct behavior of CRUD operations, exception handling, and
 * interactions with the {@link EntityRepository} and {@link ObjectMapper}.
 *
 * @author David Goldring
 */
class JpaEntityServiceTest {

  private EntityRepository repository;
  private PolicyRepository policyRepository;
  private JpaEntityService entityService;
  private ObjectMapper objectMapper;

  /**
   * Sets up the necessary objects and mocks for the test cases in JpaEntityServiceTest.
   */
  @BeforeEach
  void setUp() {
    repository = Mockito.mock(EntityRepository.class);
    policyRepository = Mockito.mock(PolicyRepository.class);
    objectMapper = Mockito.spy(new ObjectMapper());
    entityService = new JpaEntityService(repository,policyRepository, objectMapper);
  }

  /**
   * Tests the successful creation of an entity.
   * <p>
   * This test case verifies that an entity can be created successfully with the given subject and that the saved entity
   * is returned as expected. It mocks the repository save operation to ensure the method call occurs and checks that
   * the returned entity has the intended subject.
   *
   * @throws JsonProcessingException if there is a processing error when converting the entity to a JSON string.
   */
  @Test
  void testCreateEntitySuccess() throws JsonProcessingException {
    // Given
    final EntityRecord entity = EntityFactory.createDefaultEntity();
    final EntityEntity entityEntity = new EntityEntity();
    entityEntity.setSubject(EntityFactory.SUBJECT_DEFAULT);
    entityEntity.setEntity(objectMapper.writeValueAsString(entity));

    when(this.policyRepository.findByExternalId(anyString())).thenReturn(Optional.of(new PolicyEntity()));

    when(this.repository.save(any(EntityEntity.class))).thenReturn(entityEntity);

    // When
    final EntityRecord result = entityService.create(entity);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSubject()).isEqualTo(EntityFactory.SUBJECT_DEFAULT);
    verify(this.repository, times(1)).save(any(EntityEntity.class));
  }

  /**
   * Tests the creation of an entity when a JsonProcessingException occurs.
   * <p>
   * This test case verifies that a ResponseStatusException with HttpStatus.BAD_REQUEST is thrown when the ObjectMapper
   * fails to process the entity into a JSON string due to a JsonProcessingException.
   *
   * @throws JsonProcessingException if there is a processing error when converting the entity to a JSON string.
   */
  @Test
  void testCreateEntityJsonProcessingException() throws JsonProcessingException {
    // Given
    final EntityRecord entity = EntityFactory.createDefaultEntity();

    // Mock objectMapper to throw JsonProcessingException
    doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any(EntityRecord.class));

    // When
    final Throwable thrown = catchThrowable(() -> entityService.create(entity));

    // Then
    assertThat(thrown).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(this.repository, never()).save(any(EntityEntity.class));
  }

  /**
   * Tests the creation of an entity when a DataIntegrityViolationException occurs.
   * <p>
   * This test case verifies that a ResponseStatusException with HttpStatus.CONFLICT is thrown when the repository save
   * operation throws a DataIntegrityViolationException. It ensures that the repository's save method is called once
   * with any EntityDao instance and checks that the thrown exception has the expected status code.
   */
  @Test
  void testCreateEntityDataIntegrityViolationException() {
    // Given
    final EntityRecord entity = EntityFactory.createDefaultEntity();
    when(this.policyRepository.findByExternalId(anyString())).thenReturn(Optional.of(new PolicyEntity()));

    doThrow(DataIntegrityViolationException.class).when(this.repository).save(any(EntityEntity.class));

    // When
    final Throwable thrown = catchThrowable(() -> entityService.create(entity));

    // Then
    assertThat(thrown).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(this.repository, times(1)).save(any(EntityEntity.class));
  }

  /**
   * Tests the retrieval of an entity by its subject.
   * <p>
   * This test case verifies that the `EntityService.get` method correctly retrieves an entity when provided with a
   * valid subject. It sets up a mock repository to return an `EntityDao` object with the specific subject and checks
   * that the returned `Entity` object is not null and has the expected subject.
   *
   * @throws JsonProcessingException if there is a processing error when converting the entity to a JSON string.
   */
  @Test
  void testGetEntity() throws JsonProcessingException {
    // Given
    final String subject = "https://example.com/subject/12";
    final EntityEntity entityEntity = new EntityEntity();
    entityEntity.setSubject(subject);
    entityEntity.setEntity(objectMapper.writeValueAsString(EntityFactory.createDefaultEntity(subject)));

    when(this.repository.findByExternalId(subject)).thenReturn(Optional.of(entityEntity));

    // When
    final EntityRecord result = entityService.get(subject);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSubject()).isEqualTo(subject);
  }

  /**
   * Tests the retrieval of an entity when the entity is not found.
   * <p>
   * This test case verifies the behavior of the `EntityService.get` method when the entity repository does not contain
   * an entity with the specified subject. The repository is mocked to return an empty Optional, and the test checks
   * that the result is null, indicating that the entity does not exist.
   */
  @Test
  void testGetEntityNotFound() {
    // Given
    String subject = EntityFactory.SUBJECT_DEFAULT;
    when(repository.findByExternalId(anyString())).thenReturn(Optional.empty());

    // When
    EntityRecord result = entityService.get(subject);

    // Then
    assertThat(result).isNull();
  }

  /**
   * Tests the retrieval of all entities.
   * <p>
   * This test case verifies that the `EntityService.getAll` method correctly retrieves all entities stored in the
   * repository. It sets up mock repository to return a predefined list of `EntityDao` objects and checks that the
   * returned `List<Entity>` is not empty and contains the expected number of entities.
   *
   * @throws JsonProcessingException if there is a processing error when converting entities to JSON strings.
   */
  @Test
  void testGetAllEntities() throws JsonProcessingException {
    // Given
    int numberOfDaos = 42;
    List<EntityEntity> entityEntities = new ArrayList<>();
    for (int i = 1; i <= numberOfDaos; i++) {
      EntityEntity entityEntity = new EntityEntity();
      entityEntity.setSubject("https://example.com/subject/" + i);
      entityEntity.setEntity(objectMapper.writeValueAsString(new EntityRecord()));
      entityEntities.add(entityEntity);
    }

    when(repository.findAll()).thenReturn(entityEntities);

    // When
    List<EntityRecord> result = entityService.getAll();

    // Then
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(numberOfDaos);
  }

  /**
   * Tests the successful update of an existing entity.
   * <p>
   * This test case verifies that an entity can be updated successfully with the given subject. It ensures that the
   * repository's `save` method is called once with an `EntityDao` instance, and that the returned `Entity` has the
   * intended subject.
   *
   * @throws JsonProcessingException if there is a processing error when converting the entity to a JSON string.
   */
  @Test
  void testUpdateEntitySuccess() throws JsonProcessingException {
    // Given
    final EntityRecord record = EntityFactory.createDefaultEntity();

    final String extId = UUID.randomUUID().toString();
    final EntityEntity entityEntity = new EntityEntity();
    entityEntity.setSubject(record.getSubject());
    entityEntity.setIssuer(record.getIssuer());
    entityEntity.setEntity(objectMapper.writeValueAsString(record));
    record.setEntityRecordId(extId);
    when(this.repository.findByExternalId(extId)).thenReturn(Optional.of(entityEntity));
    when(this.policyRepository.findByExternalId(anyString())).thenReturn(Optional.of(new PolicyEntity()));
    when(this.repository.save(any(EntityEntity.class))).then(invocationOnMock -> invocationOnMock.getArguments()[0]);

    // When
    final EntityRecord resultRecord = entityService.update(extId, record);

    // Then
    assertThat(resultRecord).isNotNull();
    assertThat(resultRecord.getEntityRecordId()).isEqualTo(extId);
    verify(this.repository, times(1)).save(any(EntityEntity.class));
  }

  /**
   * Tests the deletion of an existing entity.
   * <p>
   * This method verifies that the `EntityService.delete` method successfully deletes an entity with a given subject. It
   * mocks the repository to return an existing `EntityDao` when the subject is queried and checks that the `delete`
   * method of the repository is called exactly once.
   */
  @Test
  void testDeleteEntity() {
    // Given
    String subject = EntityFactory.SUBJECT_3;
    EntityEntity entityEntity = new EntityEntity();
    entityEntity.setSubject(subject);
    when(repository.findByExternalId(subject)).thenReturn(Optional.of(entityEntity));

    // When
    entityService.delete(subject);

    // Then
    verify(repository, times(1)).findByExternalId(subject);
    verify(repository, times(1)).delete(entityEntity);
  }

  /**
   * Tests the behavior of the `delete` method when attempting to delete a non-existent entity.
   * <p>
   * This test case verifies that when the `entityService.delete` method is called with a subject that does not
   * correspond to any existing entity in the repository, the delete operation is not performed. It mocks the repository
   * to return an empty Optional when queried for the subject and checks that the `repository.delete` method is never
   * called.
   */
  @Test
  void testDeleteEntityNotFound() {
    // Given
    String subject = EntityFactory.SUBJECT_3;
    when(repository.findByExternalId(subject)).thenReturn(Optional.empty());

    // When
    entityService.delete(subject);

    // Then
    verify(repository, times(1)).findByExternalId(subject);
    verify(repository, never()).delete(any(EntityEntity.class));
  }
}