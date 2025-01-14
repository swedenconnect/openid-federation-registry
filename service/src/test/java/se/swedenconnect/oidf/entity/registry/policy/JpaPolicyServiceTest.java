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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.fixture.PolicyFactory;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link JpaPolicyService}.
 * <p>
 * This class is responsible for testing the various methods and functionalities of the {@link JpaPolicyService} class
 * using mocked dependencies. The tests ensure the correct behavior of CRUD operations, exception handling, and
 * interactions with the {@link PolicyRepository} and {@link ObjectMapper}.
 *
 * @author David Goldring
 */
public class JpaPolicyServiceTest {

  @Mock
  private PolicyRepository policyRepository;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

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
   * This test validates that a new policy can be successfully created and saved. The test asserts that the created
   * policy is not null and that its properties match the original input data. It also verifies that the {@code save}
   * method of the repository is called exactly once.
   */
  @Test
  public void testCreateValidPolicy() throws JsonProcessingException {

    final PolicyRecord policyRecord = PolicyFactory.record();

    final PolicyEntity entityReturnedOnSave = PolicyFactory.entity();
    entityReturnedOnSave.setName(policyRecord.getName());
    entityReturnedOnSave.setExternalId(policyRecord.getPolicyRecordId());

     when(this.policyRepository.save(any(PolicyEntity.class))).thenReturn(entityReturnedOnSave);

    // When
    final PolicyRecord createdPolicy = this.jpaPolicyService.create(policyRecord);

    // Then
    assertThat(createdPolicy).isNotNull();
    assertThat(createdPolicy.getName()).isEqualTo(policyRecord.getName());
    assertThat(createdPolicy.getPolicy()).isEqualTo(policyRecord.getPolicy());
    verify(this.policyRepository, times(1)).save(any(PolicyEntity.class));
  }




  /**
   * Tests the {@code get} method of the {@code JpaPolicyService} class.
   * <p>
   * This test verifies that the {@code get} method correctly retrieves a policy by its name.
   */
  @Test
  public void testGetPolicy() {
    // Given
    final PolicyEntity policyEntity = PolicyFactory.entity();

    when(this.policyRepository.findByExternalId(policyEntity.getExternalId())).thenReturn(Optional.of(policyEntity));

    // When
    final PolicyRecord foundPolicy = jpaPolicyService.get(policyEntity.getExternalId());

    // Then
    assertThat(foundPolicy).isNotNull();
    assertThat(foundPolicy.getName()).isEqualTo(policyEntity.getName());
    assertThat(foundPolicy.getPolicy()).isNotEmpty();
  }

  /**
   * Tests the {@code getAll} method of the {@code JpaPolicyService} class.
   * <p>
   * This test verifies that the {@code getAll} method returns a list of policies correctly.
   */
  @Test
  public void testGetAllPolicies() {

    int entityCount = 42;
    when(this.policyRepository.findAll()).thenReturn(PolicyFactory.entities().limit(entityCount).toList());

    // When
    final List<PolicyRecord> policies = jpaPolicyService.getAll();

    // Then
    assertThat(policies).isNotNull();
    assertThat(policies).hasSize(entityCount);
    assertThat(policies.getFirst().getName()).isEqualTo("policy-name-test:1");
  }

  /**
   * Tests the {@code delete} method of the {@code JpaPolicyService} class.
   */
  @Test
  public void testDeletePolicy() {
    // Given
    final PolicyEntity policyEntity = new PolicyEntity();
    policyEntity.setName("Policy to be deleted");
    policyEntity.setPolicy("{\"key\":\"value\"}");
    policyEntity.setExternalId(UUID.randomUUID().toString());
    when(this.policyRepository.findByExternalId(policyEntity.getExternalId())).thenReturn(Optional.of(policyEntity));

    // When
    this.jpaPolicyService.delete(policyEntity.getExternalId());

    // Then
    verify(this.policyRepository, times(1)).delete(policyEntity);
  }
}