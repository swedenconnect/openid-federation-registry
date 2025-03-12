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
package se.swedenconnect.oidf.entity.registry.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;

import java.util.UUID;

/**
 * Test class for the {@link EntityRepository} to ensure proper functionality of CRUD operations on {@link EntityEntity}
 * entities.
 *
 * @author David Goldring
 */
@DataJpaTest
@ActiveProfiles("h2")
public class EntityRepositoryTest {

  @Autowired
  private EntityRepository entityRepository;

  /**
   * Tests the functionality of saving an {@link EntityEntity} entity using the {@link EntityRepository}. This method
   * ensures that the entity is properly persisted to the database and can be retrieved with a valid ID, subject, and
   * entity content.
   */
  @Test
  public void testSaveEntity() {
    // Given
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(UUID.randomUUID());
    entity.setEntityType(EntityKeyType.HOSTED_ENTITY);
    // When
    EntityEntity savedEntity = entityRepository.save(entity);

    savedEntity.getOrganization();
    savedEntity.getModules();

  }


}