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
package se.swedenconnect.oidf.entity.registry.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.model.EntityDao;
import java.util.Optional;

/**
 * Test class for the {@link EntityRepository} to ensure proper functionality
 * of CRUD operations on {@link EntityDao} entities.
 *
 * @author David Goldring
 */
@DataJpaTest
@ActiveProfiles("test")
public class EntityRepositoryTest {

  @Autowired
  private EntityRepository entityRepository;

  /**
   * Tests the functionality of saving an {@link EntityDao} entity using the {@link EntityRepository}.
   * This method ensures that the entity is properly persisted to the database and can be retrieved
   * with a valid ID, subject, and entity content.
   */
  @Test
  public void testSaveEntity() {
    // Given
    EntityDao entity = new EntityDao();
    entity.setSubject("https://example.com/subject/1");
    entity.setEntity("{\"name\": \"Example Entity\"}");

    // When
    EntityDao savedEntity = entityRepository.save(entity);

    // Then
    assertThat(savedEntity.getSubject()).isEqualTo("https://example.com/subject/1");
    assertThat(savedEntity.getEntity()).isEqualTo("{\"name\": \"Example Entity\"}");
  }

  /**
   * Tests the functionality of finding an {@link EntityDao} entity by its ID using the {@link EntityRepository}.
   * This method ensures that an entity can be retrieved correctly after being persisted to the database.
   * The test verifies that the ID, subject, and entity content match the values of the saved entity.
   */
  @Test
  public void testFindById() {
    // Given
    EntityDao entity = new EntityDao();
    entity.setSubject("https://example.com/subject/2");
    entity.setEntity("{\"name\": \"Another Entity\"}");
    EntityDao savedEntity = entityRepository.save(entity);

    // When
    Optional<EntityDao> foundEntity = entityRepository.findById(savedEntity.getId());

    // Then
    assertThat(foundEntity).isPresent();
    assertThat(foundEntity.get().getId()).isEqualTo(savedEntity.getId());
    assertThat(foundEntity.get().getSubject()).isEqualTo(savedEntity.getSubject());
    assertThat(foundEntity.get().getEntity()).isEqualTo(savedEntity.getEntity());
  }

  /**
   * Tests the functionality of deleting an {@link EntityDao} entity using the {@link EntityRepository}.
   * This method ensures that the entity is properly removed from the database and cannot be retrieved
   * by its ID after deletion.
   */
  @Test
  public void testDeleteEntity() {
    // Given
    EntityDao entity = new EntityDao();
    entity.setSubject("https://example.com/subject/3");
    entity.setEntity("{\"name\": \"Entity to be deleted\"}");
    EntityDao savedEntity = entityRepository.save(entity);

    // When
    entityRepository.deleteById(savedEntity.getId());
    Optional<EntityDao> deletedEntity = entityRepository.findById(savedEntity.getId());

    // Then
    assertThat(deletedEntity).isNotPresent();
  }
}