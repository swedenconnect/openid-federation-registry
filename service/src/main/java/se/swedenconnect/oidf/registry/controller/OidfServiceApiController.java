/*
 * Copyright 2026 Sweden Connect
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

package se.swedenconnect.oidf.registry.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.service.OidfApiService;

import java.util.UUID;

/**
 * FederationService API
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/federationservice")
public class OidfServiceApiController {

  private final OidfApiService federationApiService;

  /**
   * FederationService API
   *
   * @param federationApiService FederationService
   */
  public OidfServiceApiController(final OidfApiService federationApiService) {
    this.federationApiService = federationApiService;
  }

  /**
   * Getting entity records
   *
   * @param instanceId instanceId is mandatory
   * @return SignedJWT with a claim for entity_record
   */
  /**
   * Gets entity record.
   *
   * @param instanceId the instance ID
   * @param plain whether to return plain JSON
   * @return the entity record
   */
  @GetMapping(value = "/entity_record")
  public ResponseEntity<String> entityRecord(@RequestParam(name = "instanceid") final UUID instanceId,
      @RequestParam(name = "plain", defaultValue = "false") final boolean plain) {
    final String content = this.federationApiService.entityRecord(instanceId, plain);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, plain ? MediaType.APPLICATION_JSON_VALUE : "application/jwt")
        .body(content);
  }

  /**
   * Retrieves submodules using the provided instance identifier.
   *
   * @param instanceId the unique identifier for the instance group
   * @param plain whether to return plain JSON
   * @return a signed JWT containing claims for the entity_record
   */
  @GetMapping(value = "/submodules")
  public ResponseEntity<String> submoduleRecord(@RequestParam(name = "instanceid") final UUID instanceId,
      @RequestParam(name = "plain", defaultValue = "false") final boolean plain) {

    final String content = this.federationApiService.moduleRecord(instanceId, plain);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, plain ? MediaType.APPLICATION_JSON_VALUE : "application/jwt")
        .body(content);


  }

}
