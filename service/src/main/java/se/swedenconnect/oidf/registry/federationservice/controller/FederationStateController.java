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
package se.swedenconnect.oidf.registry.federationservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.federationservice.service.FederationStateService;

import java.util.UUID;

/**
 * FederationService API for reading the changes of the records of an instance, each record under a key.
 *
 * @author Felix Hellman
 */
@RestController
@RequestMapping("/api/v1/federationservice")
@Tag(name = "OpenID Federation Service API", description = "API that OpenID Federation calls to get "
    + "configuration for the federation setup")
public class FederationStateController {

  private final FederationStateService stateService;

  /**
   * Constructor.
   *
   * @param stateService the state service
   */
  public FederationStateController(final FederationStateService stateService) {
    this.stateService = stateService;
  }

  /**
   * Gets a page of the changes after a sequence number, ordered by sequence number.
   *
   * @param instanceId the instance ID
   * @param since the sequence number the caller has read up to
   * @param plain whether to return plain JSON
   * @return the page of changes
   */
  @GetMapping(value = "/changes")
  public ResponseEntity<String> changes(@RequestParam(name = "instanceid") final UUID instanceId,
      @RequestParam(name = "since") final long since,
      @RequestParam(name = "plain", defaultValue = "false") final boolean plain) {
    return respond(this.stateService.changes(instanceId, since, plain), plain);
  }

  private static ResponseEntity<String> respond(final String content, final boolean plain) {
    String contentType = "application/jwt";
    if (plain) {
      contentType = MediaType.APPLICATION_JSON_VALUE;
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, contentType)
        .body(content);
  }
}
