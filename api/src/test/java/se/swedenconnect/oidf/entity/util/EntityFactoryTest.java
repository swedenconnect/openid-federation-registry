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
import static org.junit.jupiter.api.Assertions.*;
import se.swedenconnect.oidf.registry.api.model.Entity;
import se.swedenconnect.oidf.registry.api.model.JwkSource;
import se.swedenconnect.oidf.registry.api.model.Hosted;

import java.util.ArrayList;
import java.util.List;

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
    Entity entity = EntityFactory.createDefaultEntity();

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
    String subject = EntityFactory.SUBJECT_2;

    Entity entity = EntityFactory.createDefaultEntity(subject);

    assertNotNull(entity);

    assertEquals(subject, entity.getSubject());
  }

  /**
   * Tests the creation of an entity using the EntityFactory with a Hosted object and additional parameters.
   * <p>
   * The method performs the following checks:
   * - Verifies that the created entity is not null.
   * - Validates that the subject, location, policy, and hosted attributes of the entity match the expected values.
   * - Confirms that the intermediate flag is set correctly.
   * - Ensures that the entity's JWK list is null.
   */
  @Test
  public void testCreateEntityWithHosted() {
    // Prepare data for the test
    String subject = EntityFactory.SUBJECT_1;
    String location = "https://example.com/location";
    String policy = "policy-file";
    boolean intermediate = true;

    Hosted hosted = new Hosted(/* parameters */);

    // Test creating an entity with a Hosted object
    Entity entity = EntityFactory.createEntityWithHosted(subject, location, policy, hosted, intermediate);

    assertNotNull(entity);

    // Verify attributes
    assertEquals(subject, entity.getSubject());
    assertEquals(location, entity.getLocation());
    assertEquals(policy, entity.getPolicy());
    assertEquals(intermediate, entity.getIntermediate());
    assertEquals(hosted, entity.getHosted());

    // Ensure JwkSource list is null
    assertNull(entity.getJwk());
  }

  /**
   * Tests the creation of an entity using the EntityFactory with a list of JwkSource objects.
   * <p>
   * This method verifies the following:
   * - An entity is successfully created and is not null.
   * - The entity's subject, location, policy, intermediate flag, and JWK list are set to the expected values.
   * - The Hosted object within the entity is null.
   */
  @Test
  public void testCreateEntityWithJwkSource() {
    // Prepare data for the test
    List<JwkSource> jwkList = new ArrayList<>();
    jwkList.add(new JwkSource(/* parameters */));

    String subject = EntityFactory.SUBJECT_1;
    String location = "https://example.com/location";
    String policy = "policy-file";
    boolean intermediate = false;

    // Test creating an entity with a list of JwkSource
    Entity entity = EntityFactory.createEntityWithJwkSource(subject, jwkList, location, policy, intermediate);

    assertNotNull(entity);

    // Verify attributes
    assertEquals(subject, entity.getSubject());
    assertEquals(location, entity.getLocation());
    assertEquals(policy, entity.getPolicy());
    assertEquals(intermediate, entity.getIntermediate());
    assertEquals(jwkList, entity.getJwk());

    // Ensure Hosted object is null
    assertNull(entity.getHosted());
  }

  /**
   * Tests the creation of a JwkSource using the EntityFactory.
   * <p>
   * This method verifies the following:
   * - The JwkSource is successfully created and is not null.
   * - The kid attribute of the JwkSource matches the expected value.
   * - The certLoc attribute of the JwkSource matches the expected value.
   * - The base64jwk attribute of the JwkSource matches the expected value.
   */
  @Test
  public void testCreateJwkSource() {
    // Prepare data for the test
    String kid = "key-id";
    String certLoc = "key-type";
    String base64jwk = "sig";

    // Test creating a JwkSource
    JwkSource jwkSource = EntityFactory.createJwkSource(kid, certLoc, base64jwk);

    // Verify that the JwkSource is not null
    assertNotNull(jwkSource);

    // Verify attributes
    assertEquals(kid, jwkSource.getKid());
    assertEquals(certLoc, jwkSource.getCertLocation());
    assertEquals(base64jwk, jwkSource.getBase64jwk());
  }
}