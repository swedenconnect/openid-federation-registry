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
import java.util.UUID;

/**
 * PolicyController is a REST controller for managing policies in the entity registry. It provides endpoints for
 * creating, updating, retrieving, and deleting policy records.
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
   * @return a {@link PolicyRecord} object representing the created policy
   */
  @PostMapping
  public ResponseEntity<PolicyRecord> createPolicy(@RequestBody final PolicyRecord policy) {
    log.debug("POST: {}", policy);
    final PolicyRecord dto = this.policyService.create(policy);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  /**
   * Retrieves a list of all policy records from the entity registry.
   *
   * @return a list of {@link PolicyRecord} representing the policy records
   */
  @GetMapping
  public ResponseEntity<List<PolicyRecord>> getAllPolicies() {
    List<PolicyRecord> policies = this.policyService.getAll();
    log.debug("GET all: {}", policies);
    return ResponseEntity.ok(policies);
  }

  /**
   * Retrieves a policy by its policyRecordId from the entity registry.
   *
   * @param policyRecordId the name of the policy to be retrieved
   * @return a {@link PolicyRecord} object representing the policy, if found
   */
  @GetMapping("/{policyRecordId}")
  public PolicyRecord getPolicyByPolicyId(@PathVariable("policyRecordId") final UUID policyRecordId) {
    log.debug("GET by policyRecordId: {}", policyRecordId);

    final PolicyRecord record = this.policyService.get(policyRecordId.toString());
    if (record == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
    }

    return record;
  }

  /**
   * Updates an existing policy in the entity registry.
   *
   * @param policyRecordId The policyRecordId of the policy to update.
   * @param policy A {@link PolicyRecord} object containing the updated details of the policy.
   * @return A {@link PolicyRecord} object representing the updated policy.
   */
  @PutMapping("/{policyRecordId}")
  public PolicyRecord updatePolicy(
      @PathVariable("policyRecordId") final UUID policyRecordId,
      @RequestBody PolicyRecord policy) {
    log.debug("PUT: {}", policy);
    if (!policyRecordId.toString().equals(policy.getPolicyRecordId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PolicyId has to be the same in path and object");
    }
    return this.policyService.update(policyRecordId.toString(), policy);
  }

  /**
   * Deletes a policy by its policyRecordId from the entity registry.
   *
   * @param policyRecordId the policyRecordId of the policy to be deleted
   * @return ResponseEntity no content
   */
  @DeleteMapping("/{policyRecordId}")
  public ResponseEntity<Void> deletePolicy(@PathVariable("policyRecordId") final UUID policyRecordId) {
    log.debug("DELETE: {}", policyRecordId);
    this.policyService.delete(policyRecordId.toString());
    return ResponseEntity.noContent().build();
  }
}
