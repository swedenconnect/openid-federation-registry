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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the EntityDao class.
 * <p>
 * This class contains test methods to verify the functionality of setters, getters, the default constructor, and the
 * toString method of the EntityDao class.
 *
 * @author David Goldring
 */
public class EntityEntityTest {

  /**
   * Verifies the functionality of the getters and setters of the EntityDao class.
   */
  @Test
  public void testEntityDaoSettersAndGetters() {
    EntityEntity entity = new EntityEntity();

    entity.setId(1L);
    entity.setSubject("https://example.com/subject/1");
    entity.setEntity("{\"name\": \"Example Entity\"}");

    assertThat(entity.getId()).isEqualTo(1L);
    assertThat(entity.getSubject()).isEqualTo("https://example.com/subject/1");
    assertThat(entity.getEntity()).isEqualTo("{\"name\": \"Example Entity\"}");
  }

  /**
   * Tests the default constructor of the EntityDao class.
   */
  @Test
  public void testEntityDaoDefaultConstructor() {
    EntityEntity entity = new EntityEntity();

    assertThat(entity.getId()).isEqualTo(0L);
    assertThat(entity.getSubject()).isNull();
    assertThat(entity.getEntity()).isNull();
  }

  /**
   * Tests the toString method of the EntityDao class.
   */
  @Test
  public void testEntityDaoToString() {
    EntityEntity entity = new EntityEntity();
    entity.setId(1L);
    entity.setSubject("https://example.com/subject/1");
    entity.setEntity("{\"name\": \"Example Entity\"}");

    assertThat(entity.toString()).contains("1", "https://example.com/subject/1", "{\"name\": \"Example Entity\"}");
  }
}

