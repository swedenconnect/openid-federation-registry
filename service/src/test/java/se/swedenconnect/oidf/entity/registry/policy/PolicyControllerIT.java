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
package se.swedenconnect.oidf.entity.registry.policy;

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
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the PolicyController.
 * <p>
 * This class uses Testcontainers to run a MariaDB instance for testing purposes
 * and Spring's TestRestTemplate for executing HTTP requests.
 *
 * @author David Goldring
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PolicyControllerIT {

  @Autowired
  private TestRestTemplate restTemplate;

  /**
   * MariaDBContainer instance for setting up a MariaDB database in a Docker container.
   * Utilizes the Testcontainers library to manage the lifecycle of the Docker container.
   */
  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  /**
   * Tests that creating multiple entities with the same policy_id returns BAD_REQUEST.
   * <p>
   * This test ensures that the API correctly handles attempts to create policies entities when policy_id is set.
   * Initially, it creates an entity with a default subject and verifies the response status
   * is `HttpStatus.CREATED`. Then, it tries to create another entity with the created policy_id
   * and verifies that the response status is `HttpStatus.BAD_REQUEST`.
   */
  @Test
  public void testCreatePolicyWithPolicyIdSet() {
    // Arrange
    final PolicyRecord policy = new PolicyRecord.Builder()
        .name("policy-name")
        .policy("{}")
        .build();
    // Act
    ResponseEntity<PolicyRecord> response = this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getPolicyId()).isNotNull();

    policy.setPolicyId(response.getBody().getPolicyId());

    response = this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /**
   * Tests the retrieval of all policies from the policy registry.
   * <p>
   * This method:
   * <ul>
   *   <li>Arranges by creating multiple policy entities and sending them to the policy registry.</li>
   *   <li>Acts by making a GET request to retrieve all policies from the policy registry.</li>
   *   <li>Asserts that the response status is 200 OK, the body is not null, and contains at least 30 policies.</li>
   * </ul>
   */
  @Test
  public void testGetAllPolicies() {
    // Arrange
    IntStream.range(13, 43).boxed().forEach(i -> {
          final PolicyRecord policy = new PolicyRecord.Builder().name("policy-name-" + i).policy("{}").build();
          this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
        }
    );

    // Act
    final ResponseEntity<PolicyRecord[]> response = this.restTemplate.getForEntity("/registry/v1/policies", PolicyRecord[].class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    final PolicyRecord[] policies = response.getBody();
    assertThat(policies).isNotNull();
    assertThat(policies).hasSizeGreaterThanOrEqualTo(30);
  }

  /**
   * Tests the functionality of updating a policy in the policy registry.
   * <p>
   * This test method performs the following steps:
   * <ul>
   *   <li>Creates a new policy with a given policy body JSON</li>
   *   <li>Sends a POST request to create the policy in the registry</li>
   *   <li>Asserts that the policy has been successfully created</li>
   *   <li>Updates the policy with an additional scope in the policy body JSON</li>
   *   <li>Sends a PUT request to update the existing policy in the registry</li>
   *   <li>Sends a GET request to retrieve the updated policy from the registry</li>
   *   <li>Asserts that the policy has been successfully updated</li>
   *   <li>Asserts that the updated policy contains the new scope</li>
   * </ul>
   */
  @Test
  public void testUpdatePolicy() {
    final String policyBody = """
        {
          "openid_relying_party" : {
            "grant_types" : {
              "subset_of" : [ "authorization_code" ]
            },
            "token_endpoint_auth_method" : {
              "superset_of" : [ "private_key_jwt" ],
              "essential" : true
            },
            "response_types" : {
              "subset_of" : [ "code" ]
            }
          }
        }""";

    // Arrange
    final PolicyRecord policy = new PolicyRecord.Builder().name("openid_relying_party").policy(policyBody).build();
    final var createResponse = this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    final PolicyRecord createdPolicy = createResponse.getBody();
    assertThat(createdPolicy).isNotNull();

    // Act
    final String updatedPolicyBody = """
        {
          "openid_relying_party" : {
            "grant_types" : {
              "subset_of" : [ "authorization_code" ]
            },
            "token_endpoint_auth_method" : {
              "superset_of" : [ "private_key_jwt" ],
              "essential" : true
            },
            "response_types" : {
              "subset_of" : [ "code" ]
            },
            "scope" : {
              "subset_of" : [ "openid", "profile", "email"]
            }
          }
        }""";

    final HttpEntity<PolicyRecord> requestUpdate = new HttpEntity<>( PolicyRecord.builder()
        .name("openid_relying_party")
        .policy(updatedPolicyBody)
        .policyId(createdPolicy.getPolicyId())
        .build());

    this.restTemplate.put("/registry/v1/policies/{policy_id}", requestUpdate, createdPolicy.getPolicyId());

    final ResponseEntity<PolicyRecord> updateResponse = this.restTemplate
        .getForEntity("/registry/v1/policies/{policy_id}", PolicyRecord.class, createdPolicy.getPolicyId());

    // Assert
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    final PolicyRecord updatedPolicy = updateResponse.getBody();
    assertThat(updatedPolicy).isNotNull();
    assertThat(updatedPolicy.getPolicy())
        .containsSequence("\\\"scope\\\" : {\\n      \\\"subset_of\\\" : [ \\\"openid\\\", \\\"profile\\\", \\\"email\\\"]");
  }

  /**
   * Tests the deletion of a policy using the REST API.
   *
   * <p>This test method performs the following actions:
   * <lu>
   *   <li>Creates a new policy using a POST request and verifies that it was created successfully.
   *   <li>Deletes the created policy using a DELETE request and verifies that the deletion was successful.
   *   <li>Attempts to retrieve the deleted policy using a GET request to ensure it has been deleted and is not found.
   * </lu>
   *
   * <p>Assertions:
   * <lu>
   *   <li>Asserts that the status code of the policy creation is {@code HttpStatus.CREATED}.
   *   <li>Asserts that the status code of the policy deletion is {@code HttpStatus.NO_CONTENT}.
   *   <li>Asserts that attempting to retrieve the deleted policy returns a status code of {@code HttpStatus.NOT_FOUND}.
   * </lu>
   */
  @Test
  public void testDeletePolicy() {
    // Arrange
    final String policyName = "delete-policy-name";
    final PolicyRecord policy = new PolicyRecord.Builder().name(policyName).policy("{}").build();
    ResponseEntity<PolicyRecord> policyDtoResponseEntity =
        this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(policyDtoResponseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // Act
    final ResponseEntity<Void> deletedResponse = this.restTemplate.exchange("/registry/v1/policies/{name}", HttpMethod.DELETE, null, Void.class, policyName);

    // Assert
    assertThat(deletedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Verify it's deleted
    final ResponseEntity<PolicyRecord> getResponse = this.restTemplate.getForEntity("/registry/v1/policies/{name}", PolicyRecord.class, policyName);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
