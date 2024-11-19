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
package se.swedenconnect.oidf.entity.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.controller.PolicyDto;
import se.swedenconnect.oidf.entity.registry.model.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.service.PolicyService;

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
  public PolicyDto create(final PolicyDto policy) {
    if (this.isValidPolicy(policy)) {
      PolicyEntity policyEntity = new PolicyEntity();
      policyEntity.setPolicy(policy.policy());
      policyEntity.setName(policy.name());
      this.policyRepository.save(policyEntity);
    }
    else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON policy");
    }
    return policy;
  }

  @Override
  public PolicyDto get(final String name) {
    return this.policyRepository.findByName(name)
        .map(dao -> new PolicyDto(dao.getName(), dao.getPolicy()))
        .orElse(null);
  }

  @Override
  public List<PolicyDto> getAll() {
    return this.policyRepository.findAll()
        .stream().map(dao -> new PolicyDto(dao.getName(), dao.getPolicy()))
        .toList();
  }

  @Override
  public PolicyDto update(final String name, final PolicyDto policy) {
    final var dao = this.policyRepository.findByName(name).orElse(null);
    if (dao != null) {
      try {
        dao.setPolicy(this.objectMapper.writeValueAsString(policy));
        dao.setName(policy.name());
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
  public void delete(final String name) {
    this.policyRepository.findByName(name).ifPresent(this.policyRepository::delete);
  }

  /**
   * Validates if the provided policy DTO contains a valid JSON structure.
   *
   * @param policy the policy data transfer object containing the JSON policy string to validate
   * @return {@code true} if the policy string is valid JSON and contains the expected structure; {@code false} otherwise
   */
  private boolean isValidPolicy(final PolicyDto policy) {
    try {
      this.objectMapper.readTree(policy.policy());
      return true;
    }
    catch (Exception e) {
      log.debug("Invalid JSON policy: {}", e.getMessage());
      return false;
    }
  }
}
