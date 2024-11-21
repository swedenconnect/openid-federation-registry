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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for the {@link JpaPolicyService}.
 * <p>
 * This class is responsible for testing the various methods and functionalities
 * of the {@link JpaPolicyService} class using mocked dependencies.
 * The tests ensure the correct behavior of CRUD operations, exception handling,
 * and interactions with the {@link PolicyRepository} and {@link ObjectMapper}.
 *
 * @author David Goldring
 */
public class JpaPolicyServiceTest {

  @Mock
  private PolicyRepository policyRepository;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private JpaPolicyService jpaPolicyService;

  /**
   * Sets up the test environment before each test execution.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Tests the {@code create} method of the {@code JpaPolicyService} class.
   * <p>
   * This test validates that a new policy can be successfully created and saved.
   * The test asserts that the created policy is not null and that its properties match the original input data.
   * It also verifies that the {@code save} method of the repository is called exactly once.
   */
  @Test
  public void testCreateValidPolicy() {
    // Given
    final String policyName = "Test Policy";

    PolicyRecord policyRecord = new PolicyRecord.Builder().name(policyName).policy("{\"Test Policy\":\"value\"}").build();
    PolicyEntity policyEntity = new PolicyEntity();
    policyEntity.setName(policyRecord.getName());
    policyEntity.setPolicy(policyRecord.getPolicy());

    when(policyRepository.save(any(PolicyEntity.class))).thenReturn(policyEntity);

    // When
    PolicyRecord createdPolicy = jpaPolicyService.create(policyRecord);

    // Then
    assertThat(createdPolicy).isNotNull();
    assertThat(createdPolicy.getName()).isEqualTo(policyRecord.getName());
    assertThat(createdPolicy.getPolicy()).isEqualTo(policyRecord.getPolicy());
    verify(policyRepository, times(1)).save(any(PolicyEntity.class));
  }

  /**
   * Tests the {@code create} method of the {@code JpaPolicyService} class when given an invalid policy.
   * <p>
   * This test ensures that an attempt to create a policy with invalid JSON content results
   * in a {@link ResponseStatusException} with a {@link HttpStatus#BAD_REQUEST BAD_REQUEST} status.
   *
   * @throws JsonProcessingException if there is a problem processing JSON content
   */
  @Test
  public void testCreateInvalidPolicy() throws JsonProcessingException {
    // Given
    PolicyRecord policyRecord = new PolicyRecord.Builder().name("Invalid Policy").policy("Invalid JSON").build();
    doThrow(JsonMappingException.class).when(objectMapper).readTree(any(String.class));

    // When
    Throwable thrown = catchThrowable(() -> jpaPolicyService.create(policyRecord));

    // Then
    assertThat(thrown).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    verify(policyRepository, never()).save(any(PolicyEntity.class));
  }

  /**
   * Tests the {@code get} method of the {@code JpaPolicyService} class.
   * <p>
   * This test verifies that the {@code get} method correctly retrieves a policy by its name.
   */
  @Test
  public void testGetPolicy() {
    // Given
    PolicyEntity policyEntity = new PolicyEntity();
    policyEntity.setName("Test Policy");
    policyEntity.setPolicy("{\"key\":\"value\"}");

    when(policyRepository.findByExternalId("Test Policy")).thenReturn(Optional.of(policyEntity));

    // When
    PolicyRecord foundPolicy = jpaPolicyService.get("Test Policy");

    // Then
    assertThat(foundPolicy).isNotNull();
    assertThat(foundPolicy.getName()).isEqualTo(policyEntity.getName());
    assertThat(foundPolicy.getPolicy()).isEqualTo(policyEntity.getPolicy());
  }

  /**
   * Tests the {@code getAll} method of the {@code JpaPolicyService} class.
   * <p>
   * This test verifies that the {@code getAll} method returns a list of policies correctly.
   */
  @Test
  public void testGetAllPolicies() {
    // Given
    int numberOfPolicies = 42;
    List<PolicyEntity> policyEntities = new java.util.ArrayList<>();
    for (int i = 1; i <= numberOfPolicies; i++) {
      PolicyEntity policyEntity = new PolicyEntity();
      policyEntity.setName("Policy " + i);
      policyEntity.setPolicy("{\"key" + i + "\":\"value" + i + "\"}");
      policyEntities.add(policyEntity);
    }

    when(policyRepository.findAll()).thenReturn(policyEntities);

    // When
    List<PolicyRecord> policies = jpaPolicyService.getAll();

    // Then
    assertThat(policies).isNotNull();
    assertThat(policies).hasSize(numberOfPolicies);
    assertThat(policies.getFirst().getName()).isEqualTo("Policy 1");
  }

  /**
   * Tests the {@code update} method of the {@code JpaPolicyService} class.
   *
   * @throws Exception if an error occurs during the update process
   */
  @Test
  public void testUpdatePolicy() throws Exception {
    // Given
    PolicyEntity existingPolicy = new PolicyEntity();
    existingPolicy.setName("Existing Policy");
    existingPolicy.setPolicy("{\"oldKey\":\"oldValue\"}");

    PolicyRecord updateDto = new PolicyRecord.Builder().name("Updated Policy").policy("{\"newKey\":\"newValue\"}").build();

    when(policyRepository.findByExternalId("Existing Policy")).thenReturn(Optional.of(existingPolicy));
    when(objectMapper.writeValueAsString(any(PolicyRecord.class))).thenReturn(updateDto.getPolicy());

    // When
    PolicyRecord updatedPolicy = jpaPolicyService.update("Existing Policy", updateDto);

    // Then
    assertThat(updatedPolicy).isNotNull();
    assertThat(updatedPolicy.getName()).isEqualTo(updateDto.getName());
    assertThat(updatedPolicy.getPolicy()).isEqualTo(updateDto.getPolicy());
    verify(policyRepository, times(1)).save(existingPolicy);
  }

  /**
   * Tests the {@code delete} method of the {@code JpaPolicyService} class.
   */
  @Test
  public void testDeletePolicy() {
    // Given
    PolicyEntity policyEntity = new PolicyEntity();
    policyEntity.setName("Policy to be deleted");
    policyEntity.setPolicy("{\"key\":\"value\"}");

    when(policyRepository.findByExternalId("Policy to be deleted")).thenReturn(Optional.of(policyEntity));

    // When
    jpaPolicyService.delete("Policy to be deleted");

    // Then
    verify(policyRepository, times(1)).delete(policyEntity);
  }
}