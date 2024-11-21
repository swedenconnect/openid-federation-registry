/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;

/**
 * JpaPolicyService is an implementation of the {@link PolicyService} interface
 * that uses JPA for managing JSON Policy objects in the database.
 *
 * <ul>
 *   <li>Provide implementation for CRUD operations on policies stored as JSON objects.</li>
 *   <li>Validate policy content before performing operations.</li>
 * </ul>
 *
 * This service utilizes the ObjectMapper from the Jackson library to handle JSON conversion
 * between Entity objects and their DAO ({@link PolicyEntity}) representations.
 *
 * @author David Goldring
 */
@Slf4j
public class JpaPolicyService implements PolicyService {

  @Getter
  private final PolicyRepository policyRepository;
  private final ObjectMapper objectMapper;

  /**
   * Construct a JpaPolicyService with the provided repository and object mapper.
   *
   * @param policyRepository the JPA repository
   * @param objectMapper the ObjectMapper used for JSON conversion between Policy strings and their DAO representations
   */
  public JpaPolicyService(final PolicyRepository policyRepository, final ObjectMapper objectMapper) {
    this.policyRepository = policyRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public PolicyRecord create(final PolicyRecord policy) {
    if (this.isValidPolicy(policy)) {
      final PolicyEntity policyEntity = new PolicyEntity();
      policyEntity.setPolicy(policy.getPolicy());
      policyEntity.setName(policy.getName());
      final PolicyEntity savedPolicy = this.policyRepository.save(policyEntity);
      policy.setPolicyId(savedPolicy.getExternalId());
    }
    else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON policy");
    }
    return policy;
  }

  @Override
  public PolicyRecord get(final String policy_id) {
    return this.policyRepository.findByExternalId(policy_id)
        .map(dao -> new PolicyRecord.Builder().name(dao.getName()).policyId(policy_id).policy(dao.getPolicy()).build())
        .orElse(null);
  }

  @Override
  public List<PolicyRecord> getAll() {
    return this.policyRepository.findAll()
        .stream().map(dao -> new PolicyRecord.Builder().name(dao.getName()).policy(dao.getPolicy()).build())
        .toList();
  }

  @Override
  public PolicyRecord update(final String policy_id, final PolicyRecord policy) {
    final var dao = this.policyRepository.findByExternalId(policy_id).orElse(null);
    if (dao != null) {
      try {
        dao.setPolicy(this.objectMapper.writeValueAsString(policy));
        dao.setName(policy.getName());
        this.policyRepository.save(dao);
        return policy;
      }
      catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Override
  public void delete(final String policy_id) {
    this.policyRepository.findByExternalId(policy_id).ifPresent(this.policyRepository::delete);
  }

  /**
   * Validates if the provided policy DTO contains a valid JSON structure.
   *
   * @param policy the policy data transfer object containing the JSON policy string to validate
   * @return {@code true} if the policy string is valid JSON and contains the expected structure;
   * {@code false} otherwise
   */
  private boolean isValidPolicy(final PolicyRecord policy) {
    try {
      this.objectMapper.readTree(policy.getPolicy());
      return true;
    }
    catch (Exception e) {
      log.debug("Invalid JSON policy: {}", e.getMessage());
      return false;
    }
  }
}
