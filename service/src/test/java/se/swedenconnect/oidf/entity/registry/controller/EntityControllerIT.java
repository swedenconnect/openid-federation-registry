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
package se.swedenconnect.oidf.entity.registry.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.util.EntityFactory;
import se.swedenconnect.oidf.registry.api.model.Entity;
import se.swedenconnect.oidf.registry.api.model.Hosted;
import se.swedenconnect.oidf.registry.api.model.JwkSource;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the EntityController class. This class uses Testcontainers to
 * run a MariaDB container for the tests, and Spring Boot's TestRestTemplate to interact with
 * the API endpoints.
 *
 * @author David Goldring
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EntityControllerIT {

  @Autowired
  private TestRestTemplate restTemplate;

  /**
   * A static instance of the MariaDBContainer using version 11.2 of the MariaDB image.
   * This container is managed by the Spring framework with the use of {@code @Container}
   * and {@code @ServiceConnection} annotations.
   * The instance facilitates database management for integration testing by providing
   * an isolated database environment.
   */
  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  /**
   * Tests that creating multiple entities with the same subject returns a conflict status.
   * <p>
   * This test ensures that the API correctly handles attempts to create duplicate entities.
   * Initially, it creates an entity with a default subject and verifies the response status
   * is `HttpStatus.CREATED`. Then, it tries to create another entity with the same subject
   * and verifies that the response status is `HttpStatus.CONFLICT`.
   */
  @Test
  public void testCreateMultipleEntityWithSameSubject() {
    // Arrange
    final Entity entity = EntityFactory.createDefaultEntity();

    // Act
    ResponseEntity<Entity> response = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    response = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  /**
   * Tests the creation of an Entity with a JSON Web Key (JWK) source.
   * <p>
   * This method performs the following steps:
   * 1. Arranges by creating a default entity with a subject that includes a JWK.
   * 2. Acts by sending a POST request to create the entity in the registry.
   * 3. Asserts that:
   *    - The response status code is HTTP 201 (Created).
   *    - The created entity is not null.
   *    - The subject of the created entity matches the expected subject.
   *    - The created entity includes a non-null JWK.
   *    - The hosted value of the created entity is null.
   */
  @Test
  public void testCreateEntityWithJWKSource() {
    // Arrange
    final Entity entity = EntityFactory.createDefaultEntity("subject-with-jwk");

    // Act
    final ResponseEntity<Entity> response = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    final Entity createdEntity = response.getBody();
    assertThat(createdEntity).isNotNull();
    assertThat(createdEntity.getSubject()).isEqualTo("subject-with-jwk");
    assertThat(createdEntity.getJwk()).isNotNull();
    assertThat(createdEntity.getHosted()).isNull(); // Ensuring Hosted is null
  }

  /**
   * Tests the creation of an entity with hosted metadata using an HTTP POST request.
   * This method performs the following steps:
   * - Arranges the test data including trust mark sources and hosted metadata.
   * - Creates an entity with the hosted metadata.
   * - Sends an HTTP POST request to create the entity.
   * - Asserts the expected response and validates the created entity's properties.
   * <p>
   * The test ensures:
   * - The HTTP response status is CREATED.
   * - The created entity is not null.
   * - The subject in the created entity matches the expected value.
   * - The JSON Web Key (JWK) source list is null.
   * - The hosted metadata is not null.
   */
  @Test
  public void testCreateEntityWithHosted() {
    // Arrange
    final List<TrustMarkSource> trustMarkSources = new ArrayList<>();
    trustMarkSources.add(EntityFactory.createTrustMarkSource("default-trust-mark-id", "default-issuer"));

    final Hosted hosted = EntityFactory.createHosted("default-metadata", trustMarkSources);
    final Entity entity = EntityFactory.createEntityWithHosted(
        "subject-with-hosted",
        "http://location-with-hosted.com",
        "policy-with-hosted",
        hosted,
        false
    );

    // Act
    final ResponseEntity<Entity> response = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    final Entity createdEntity = response.getBody();
    assertThat(createdEntity).isNotNull();
    assertThat(createdEntity.getSubject()).isEqualTo("subject-with-hosted");
    assertThat(createdEntity.getJwk()).isNull(); // Ensuring JWKSource list is null
    assertThat(createdEntity.getHosted()).isNotNull();
  }

  /**
   * Tests the retrieval of all entities from the registry.
   * <p>
   * This test method performs the following steps:
   * - Creates and posts multiple entities with JWKSource and Hosted data to the registry.
   * - Sends a GET request to the endpoint to retrieve the list of all entities.
   * - Asserts that the response status code is HTTP 200 (OK).
   * - Asserts that the returned list of entities is not null.
   * - Asserts that the size of the returned list of entities is at least 20.
   * <p>
   * The entities are created using a loop that generates entities with distinct fields to ensure variety in the dataset.
   * Two types of entities are created:
   * - Entities containing a JWKSource.
   * - Entities containing Hosted data.
   */
  @Test
  public void testGetAllEntities() {
    // Arrange
    IntStream.range(4, 14).boxed().forEach(i -> {
      final List<JwkSource> jwkSources = new ArrayList<>();
      jwkSources.add(EntityFactory.createJwkSource("kid" + i, "http://cert-location" + i + ".com", "base64jwk" + i));

      // Create entity with JWKSource
      final Entity entityWithJWKSource = EntityFactory.createEntityWithJwkSource(
          "https://example.com/subject/" + i,
          jwkSources,
          "http://location" + i + ".com",
          "policy" + i,
          false
      );
      restTemplate.postForEntity("/registry/v1/entities", entityWithJWKSource, Entity.class);

      final List<TrustMarkSource> trustMarkSources = new ArrayList<>();
      trustMarkSources.add(EntityFactory.createTrustMarkSource("trust-mark-id-" + i, "issuer-" + i));
      final Hosted hosted = EntityFactory.createHosted("metadata" + i, trustMarkSources);

      // Create entity with Hosted
      final Entity entityWithHosted = EntityFactory.createEntityWithHosted(
          "https://example.com/subject/" + (i + 10),
          "http://location-hosted" + i + ".com",
          "policy-hosted" + i,
          hosted,
          false
      );
      restTemplate.postForEntity("/registry/v1/entities", entityWithHosted, Entity.class);
    });

    // Act
    final ResponseEntity<Entity[]> response = restTemplate.getForEntity("/registry/v1/entities", Entity[].class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    final Entity[] entities = response.getBody();
    assertThat(entities).isNotNull();
    assertThat(entities).hasSizeGreaterThanOrEqualTo(20); // Ensuring at least 20 entities (could be more from the other tests)
  }

  /**
   * Tests the scenario where the list of entities is empty.
   * <p>
   * This test first ensures that all existing entities are deleted from the registry endpoint.
   * It then performs a request to retrieve all entities and verifies that the response status is HTTP OK
   * and that the response body is empty.
   */
  @Test
  public void testGetAllEntitiesWithNoEntities() {
    // Arrange
    ResponseEntity<Entity[]> response = restTemplate.getForEntity("/registry/v1/entities", Entity[].class);
    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
      Arrays.stream(response.getBody()).filter(e -> e.getSubject() != null).forEach(
          e -> restTemplate.delete("/registry/v1/entities/{id}",
              URLEncoder.encode(e.getSubject(), StandardCharsets.UTF_8)));
    }

    // Act
    response = restTemplate.getForEntity("/registry/v1/entities", Entity[].class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  /**
   * Tests the update functionality for the Entity resource.
   * <p>
   * This test creates a new entity, modifies its location, and then updates the entity
   * in the registry. After the update, the test verifies that the entity's location
   * has been correctly updated.
   *
   */
  @Test
  public void testUpdateEntity() {
    // Arrange
    final Entity entity = EntityFactory.createDefaultEntity("update-entity-subject");

    final ResponseEntity<Entity> createResponse = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);
    final Entity createdEntity = createResponse.getBody();
    assertThat(createdEntity).isNotNull();

    assert createdEntity.getSubject() != null;
    final String entityId = URLEncoder.encode(createdEntity.getSubject(), StandardCharsets.UTF_8);
    createdEntity.setLocation("updated-location");

    // Act
    final HttpEntity<Entity> requestUpdate = new HttpEntity<>(createdEntity);
    restTemplate.put("/registry/v1/entities/{id}", requestUpdate, entityId);

    final ResponseEntity<Entity> updateResponse = restTemplate.getForEntity("/registry/v1/entities/{id}", Entity.class, entityId);

    // Assert
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    final Entity updatedEntity = updateResponse.getBody();
    assertThat(updatedEntity).isNotNull();
    assertThat(updatedEntity.getLocation()).isEqualTo("updated-location");
  }

  /**
   * Tests the deletion of an entity in the registry.
   * <p>
   * This test performs the following actions:
   * 1. It creates a default entity with a specified subject.
   * 2. Posts the entity to the registry and confirms its creation.
   * 3. Deletes the created entity by its subject.
   * 4. Asserts that the deletion was successful by checking the response status.
   * 5. Attempts to retrieve the deleted entity to ensure it no longer exists in the registry.
   * <p>
   * The test ensures that the deletion operation works as expected and that the entity
   * is no longer retrievable after being deleted.
   */
  @Test
  public void testDeleteEntity() {
    // Arrange
    final Entity entity = EntityFactory.createDefaultEntity("delete-entity-subject");
    final ResponseEntity<Entity> createResponse = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);
    final Entity createdEntity = createResponse.getBody();
    assertThat(createdEntity).isNotNull();

    assert createdEntity.getSubject() != null;
    final String entityId = URLEncoder.encode(createdEntity.getSubject(), StandardCharsets.UTF_8);

    // Act
    final ResponseEntity<Void> deleteResponse = restTemplate.exchange("/registry/v1/entities/{id}", HttpMethod.DELETE, null, Void.class, entityId);

    // Assert
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Verify it's deleted
    final ResponseEntity<Entity> getResponse = restTemplate.getForEntity("/registry/v1/entities/{id}", Entity.class, entityId);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}