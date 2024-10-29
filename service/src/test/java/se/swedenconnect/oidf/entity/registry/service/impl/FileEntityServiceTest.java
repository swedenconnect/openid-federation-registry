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
package se.swedenconnect.oidf.entity.registry.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.entity.util.EntityFactory;
import se.swedenconnect.oidf.registry.api.model.Entity;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FileEntityServiceTest is a test class for testing the functionality of the FileEntityService class.
 * It uses JUnit 5 framework for writing unit tests to ensure the correctness and integrity of the
 * FileEntityService implementation.
 *
 * @author David Goldring
 */
public class FileEntityServiceTest {

  private static final String FILE_PATH = FileEntityService.FILE_PATH;
  private FileEntityService fileEntityService;

  /**
   * Sets up the test environment before each test execution.
   */
  @BeforeEach
  public void setUp() {
    deleteFile();

    fileEntityService = spy(new FileEntityService());
    doReturn(new HashMap<String, Entity>()).when(fileEntityService).loadFromFile();
  }

  /**
   * Cleans up the test environment after each test execution.
   */
  @AfterEach
  public void tearDown() {
    deleteFile();
  }

  private void deleteFile() {
    File file = new File(FILE_PATH);
    if (file.exists()) {
      //noinspection ResultOfMethodCallIgnored
      file.delete();
    }
  }

  /**
   * Tests the creation of an entity using the fileEntityService.
   * <p>
   * This test covers the following:
   * 1. Verifies that the created entity is not null.
   * 2. Ensures that the saveToFile() method of fileEntityService is called exactly once.
   *
   */
  @Test
  public void testCreateEntity() {
    // Given
    Entity entity = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);

    // When
    Entity createdEntity = fileEntityService.create(entity);

    // Then
    assertThat(createdEntity).isNotNull();
    verify(fileEntityService, times(1)).saveToFile();
  }

  /**
   * Tests the retrieval of an entity using the fileEntityService.
   * <p>
   * This test covers the following:
   * 1. Verifies that the retrieved entity is not null.
   * 2. Ensures that the subject of the retrieved entity matches the expected subject.
   */
  @Test
  public void testGetEntity() {
    // Given
    Entity entity = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);
    fileEntityService.create(entity);

    // When
    Entity result = fileEntityService.get(EntityFactory.SUBJECT_1);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSubject()).isEqualTo(EntityFactory.SUBJECT_1);
  }

  /**
   * Tests the retrieval of a non-existent entity using the fileEntityService.
   * <p>
   * This test covers the following:
   * 1. Verifies that the retrieved entity is null when an invalid ID is requested.
   */
  @Test
  public void testGetNonExistentEntity() {
    // When
    Entity result = fileEntityService.get("non-existent");

    // Then
    assertThat(result).isNull();
  }

  /**
   * Tests the retrieval of all entities using the fileEntityService.
   * <p>
   * This test performs the following steps:
   * 1. Creates two different entities using EntityFactory and the fileEntityService.
   * 2. Verifies that the getAll method of fileEntityService returns a non-empty list.
   * 3. Ensures that the size of the returned list equals the number of created entities.
   */
  @Test
  public void testGetAllEntities() {
    // Given
    Entity entity1 = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);
    fileEntityService.create(entity1);

    Entity entity2 = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_2);
    fileEntityService.create(entity2);

    // When
    List<Entity> result = fileEntityService.getAll();

    // Then
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(2);
  }

  /**
   * Tests the update functionality of an entity using the fileEntityService.
   * <p>
   * This test performs the following steps:
   * 1. Creates a default entity and saves it using fileEntityService.
   * 2. Creates an updated version of the same entity with modifications.
   * 3. Calls the update method to update the entity.
   * 4. Verifies the returned entity is not null and that the location is updated correctly.
   * 5. Ensures the saveToFile method of fileEntityService is called twice (once for creation and once for update).
   *
   */
  @Test
  public void testUpdateEntity() {
    // Given
    Entity entity = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);
    fileEntityService.create(entity);

    Entity updatedEntity = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);
    updatedEntity.setLocation("updated-location");

    // When
    Entity result = fileEntityService.update(EntityFactory.SUBJECT_1, updatedEntity);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getLocation()).isEqualTo("updated-location");
    verify(fileEntityService, times(2)).saveToFile();  // 1 for create, 1 for update
  }

  /**
   * Tests the deletion of an entity using the fileEntityService.
   * <p>
   * This test follows these steps:
   * 1. Creates a default entity using EntityFactory and the fileEntityService.
   * 2. Deletes the created entity by its subject.
   * 3. Verifies that the entity is null after deletion.
   * 4. Ensures the saveToFile method of fileEntityService is called exactly twice (once for creation and once for deletion).
   *
   */
  @Test
  public void testDeleteEntity() {
    // Given
    Entity entity = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);
    fileEntityService.create(entity);

    // When
    fileEntityService.delete(EntityFactory.SUBJECT_1);

    // Then
    Entity result = fileEntityService.get(EntityFactory.SUBJECT_1);
    assertThat(result).isNull();
    verify(fileEntityService, times(2)).saveToFile();  // 1 for create, 1 for delete
  }

  /**
   * Tests that the saveToFile method of fileEntityService is invoked twice.
   * <p>
   * This test performs the following steps:
   * 1. Creates two different entities using EntityFactory.
   * 2. Calls the create method on fileEntityService twice, once for each entity.
   * 3. Verifies that the saveToFile method of fileEntityService is called exactly twice.
   *
   */
  @Test
  public void testSaveToFileCalledTwice() {
    // Given
    Entity entity1 = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_1);
    Entity entity2 = EntityFactory.createDefaultEntity(EntityFactory.SUBJECT_2);

    // When
    fileEntityService.create(entity1);
    fileEntityService.create(entity2);

    // Then
    verify(fileEntityService, times(2)).saveToFile();
  }

}