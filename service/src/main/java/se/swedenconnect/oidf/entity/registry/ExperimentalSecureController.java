/*
 * Copyright 2025 Sweden Connect
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
 *  limitations under the License.
 */
package se.swedenconnect.oidf.entity.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.UUID;

/**
 * This controller provides experimental endpoints for secure operations. It is designed to handle GET and POST requests
 * and dynamically generates a UUID for each request.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequestMapping("/secure")
public class ExperimentalSecureController {

  @Autowired
  RegistryAuditService auditService;

  /**
   * GET
   *
   * @return UUID
   */
  @GetMapping("/policies")
  public String policiesRead() {
    this.auditService.policyWrite(UUID.randomUUID().toString(),
        PolicyRecord.builder().name("ExperimentalSecureController-READ").policyRecordId(UUID.randomUUID().toString())
            .build(), PolicyRecord.builder().build());
    return "Read UUID: " + this.generateUuid();
  }

  /**
   * Post
   *
   * @return UUID
   */
  @PostMapping("/policies")
  public String policiesWrite() {
    this.auditService.policyWrite(UUID.randomUUID().toString(),
        PolicyRecord.builder().name("ExperimentalSecureController-WRITE")
            .policyRecordId(UUID.randomUUID().toString()).build(),
        PolicyRecord.builder().policyRecordId(UUID.randomUUID().toString()).build());

    return "Write UUID: " + this.generateUuid();
  }

  /**
   * GET
   *
   * @return UUID
   */
  @GetMapping
  public String handleGetRequest() {
    return "GET response with UUID: " + this.generateUuid();
  }

  /**
   * Post
   *
   * @return UUID
   */
  @PostMapping
  public String handlePostRequest() {
    return "POST response with UUID: " + this.generateUuid();
  }

  private String generateUuid() {
    return java.util.UUID.randomUUID().toString();
  }
}
