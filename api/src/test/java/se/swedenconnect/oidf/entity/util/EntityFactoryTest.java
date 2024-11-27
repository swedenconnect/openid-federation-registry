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
package se.swedenconnect.oidf.entity.util;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The EntityFactoryTest class contains unit tests for the methods in the EntityFactory class.
 * It verifies the behavior of entity creation methods in various scenarios.
 *
 * @author David Goldring
 */
public class EntityFactoryTest {

  /**
   * Tests the creation of a default entity using the EntityFactory.
   * <p>
   * The method verifies:
   *  - The created entity is not null.
   *  - The default subject of the entity is set to the expected default value defined in EntityFactory.
   */
  @Test
  public void testCreateDefaultEntity() {
    final EntityRecord entity = EntityFactory.createDefaultEntity();

    assertNotNull(entity);

    assertEquals(EntityFactory.SUBJECT_DEFAULT, entity.getSubject());
  }

  /**
   * Tests the creation of an entity using EntityFactory with a specified subject.
   * <p>
   * The method validates that:
   * - An entity is successfully created and is not null.
   * - The entity's subject is set to the expected subject.
   */
  @Test
  public void testCreateDefaultEntityWithSubject() {
    final String subject = EntityFactory.SUBJECT_2;

    final EntityRecord entity = EntityFactory.createDefaultEntity(subject);

    assertNotNull(entity);

    assertEquals(subject, entity.getSubject());
  }

}