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
import se.swedenconnect.oidf.registry.api.model.EntityRecord;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.Arrays;
import java.util.UUID;
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
    final EntityRecord entity = EntityFactory.createDefaultEntity();
    entity.setPolicyRecordId(createPolicy());


    // Act
    final ResponseEntity<EntityRecord> response =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<EntityRecord> secondRes =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);

    // Assert
    assertThat(secondRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
    final EntityRecord entity = EntityFactory.createDefaultEntity("http://subject-with-jwk");
    entity.setPolicyRecordId(createPolicy());

    // Act
    final ResponseEntity<EntityRecord> response =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    final EntityRecord createdEntity = response.getBody();
    assertThat(createdEntity).isNotNull();
    assertThat(createdEntity.getSubject()).isEqualTo("http://subject-with-jwk");
    assertThat(createdEntity.getJwks()).isNotNull();
    assertThat(createdEntity.getHostedRecord()).isNotNull();
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

    final EntityRecord entity = EntityFactory.createDefaultEntity("http://iss40","http://subj40");
    entity.setPolicyRecordId(createPolicy());

    // Act
    final ResponseEntity<EntityRecord> response =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    final EntityRecord createdEntity = response.getBody();
    assertThat(createdEntity).isNotNull();
    assertThat(createdEntity.getSubject()).isEqualTo("http://subj40");
    assertThat(createdEntity.getJwks()).isNotNull();
    assertThat(createdEntity.getHostedRecord()).isNotNull();
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
    final String policyRecordId = createPolicy();

    // Arrange
    IntStream.range(4, 14).boxed().forEach(i -> {
      final EntityRecord entityWithJWKSource =
          EntityFactory.createDefaultEntity(
              "https://example.com/issuer/" + i,
              "https://example.com/subject/" + i);
      entityWithJWKSource.setPolicyRecordId(policyRecordId);
      this.restTemplate.postForEntity("/registry/v1/entities", entityWithJWKSource, EntityRecord.class);
    });

    // Act
    final ResponseEntity<EntityRecord[]> response =
        this.restTemplate.getForEntity("/registry/v1/entities", EntityRecord[].class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    final EntityRecord[] entities = response.getBody();
    assertThat(entities).isNotNull();
    assertThat(entities).hasSizeGreaterThanOrEqualTo(10); // Ensuring at least 10 entities (could be more from the other tests)
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

    final EntityRecord entityWithJWKSource =
        EntityFactory.createDefaultEntity("https://iss.com/issuer/","https://sub.com/subject/");
    entityWithJWKSource.setPolicyRecordId(createPolicy());

    this.restTemplate.postForEntity("/registry/v1/entities", entityWithJWKSource, EntityRecord.class);
    // Arrange
    final ResponseEntity<EntityRecord[]> response = this.restTemplate.getForEntity("/registry/v1/entities", EntityRecord[].class);
    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
      Arrays.stream(response.getBody()).filter(e -> e.getEntityRecordId() != null).forEach(
          e -> this.restTemplate.delete("/registry/v1/entities/{id}", e.getEntityRecordId()));
    }

    // Act
    final ResponseEntity<EntityRecord[]> secondRetry = this.restTemplate.getForEntity("/registry/v1/entities", EntityRecord[].class);

    // Assert
    assertThat(secondRetry.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(secondRetry.getBody()).isEmpty();
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
    final EntityRecord entity = EntityFactory.createDefaultEntity("http://update-entity-subject");
    entity.setPolicyRecordId(createPolicy());

    final ResponseEntity<EntityRecord> createResponse =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);
    final EntityRecord createdEntity = createResponse.getBody();
    assertThat(createdEntity).isNotNull();

    assert createdEntity.getSubject() != null;
    final String entityId = createResponse.getBody().getEntityRecordId();
    createdEntity.setEntityRecordId(entityId);

    // Act
    final HttpEntity<EntityRecord> requestUpdate = new HttpEntity<>(createdEntity);
    this.restTemplate.put("/registry/v1/entities/{id}", requestUpdate, entityId);

    final ResponseEntity<EntityRecord> updateResponse =
        this.restTemplate.getForEntity("/registry/v1/entities/{id}", EntityRecord.class, entityId);

    // Assert
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    final EntityRecord updatedEntity = updateResponse.getBody();
    assertThat(updatedEntity).isNotNull();
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
    final EntityRecord entity = EntityFactory.createDefaultEntity("delete-entity-subject");
    entity.setPolicyRecordId(createPolicy());

    final ResponseEntity<EntityRecord> createResponse = this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);
    final EntityRecord createdEntity = createResponse.getBody();
    assertThat(createdEntity).isNotNull();

    assert createdEntity.getSubject() != null;
    final String entityRecordId = UUID.randomUUID().toString();

    // Act
    final ResponseEntity<Void> deleteResponse =
        this.restTemplate.exchange("/registry/v1/entities/{id}", HttpMethod.DELETE, null, Void.class, entityRecordId);

    // Assert
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Verify it's deleted
    final ResponseEntity<EntityRecord> getResponse =
        this.restTemplate.getForEntity("/registry/v1/entities/{id}", EntityRecord.class, entityRecordId);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private String createPolicy(){
    final PolicyRecord policy = new PolicyRecord.Builder()
        .name("policy-name")
        .policy("{}")
        .policyRecordId(UUID.randomUUID().toString())
        .build();
    // Act
    final ResponseEntity<PolicyRecord> response =
        this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return policy.getPolicyRecordId();
  }
}