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

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;

/**
 * PolicyController is a REST controller for managing policies in the entity registry.
 * It provides endpoints for creating, updating, retrieving, and deleting policy records.
 * <p>
 * The controller utilizes {@link PolicyService} to perform the required operations.
 *
 * @author David Goldring
 */
@Slf4j
@RestController
@RequestMapping("/registry/v1/policies")
public class PolicyController {

  /**
   * The {@code policyService} is an instance of the {@link PolicyService} class.
   */
  private final PolicyService policyService;

  /**
   * Constructs a new PolicyController with the specified PolicyService implementation.
   *
   * @param policyService the {@link PolicyService} implementation used for managing policy operations
   */
  public PolicyController(@Qualifier("jpaPolicyService") PolicyService policyService) {
    this.policyService = policyService;
  }

  /**
   * Creates a new policy in the entity registry.
   *
   * @param policy a {@link PolicyRecord} object containing the details of the policy to be created
   * @param response the {@link HttpServletResponse} object used to set the response status
   *
   * @return a {@link PolicyRecord} object representing the created policy
   */
  @PostMapping
  public PolicyRecord createPolicy(@RequestBody final PolicyRecord policy, final HttpServletResponse response) {
    log.debug("POST: {}", policy);

    PolicyRecord dto = this.policyService.create(policy);
    response.setStatus(HttpServletResponse.SC_CREATED);

    return dto;
  }

  /**
   * Retrieves a list of all policy records from the entity registry.
   *
   * @return a list of {@link PolicyRecord} representing the policy records
   */
  @GetMapping
  public List<PolicyRecord> getAllPolicies() {
    List<PolicyRecord> policies = this.policyService.getAll();
    log.debug("GET all: {}", policies);

    return ResponseEntity.ok(policies).getBody();
  }

  /**
   * Retrieves a policy by its name from the entity registry.
   *
   * @param name the name of the policy to be retrieved
   *
   * @return a {@link PolicyRecord} object representing the policy, if found
   */
  @GetMapping("/{name}")
  public PolicyRecord getPolicyByName(@PathVariable("name") final String name) {
    log.debug("GET by name: {}", name);

    PolicyRecord dto = this.policyService.get(name);
    if (dto == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
    }

    return dto;
  }

  /**
   * Updates an existing policy in the entity registry.
   *
   * @param name The name of the policy to update.
   * @param policy A {@link PolicyRecord} object containing the updated details of the policy.
   * @return A {@link PolicyRecord} object representing the updated policy.
   */
  @PutMapping("/{name}")
   public PolicyRecord updatePolicy(@PathVariable("name") final String name, @RequestBody PolicyRecord policy) {
     log.debug("PUT: {}", policy);

     return this.policyService.update(name, policy);
   }

  /**
   * Deletes a policy by its name from the entity registry.
   *
   * @param name the name of the policy to be deleted
   * @param response the {@link HttpServletResponse} object used to set the response status
   */
  @DeleteMapping("/{name}")
   public void deletePolicy(@PathVariable("name") final String name, final HttpServletResponse response) {
     log.debug("DELETE: {}", name);

     this.policyService.delete(name);
     response.setStatus(HttpServletResponse.SC_NO_CONTENT);
   }
}
