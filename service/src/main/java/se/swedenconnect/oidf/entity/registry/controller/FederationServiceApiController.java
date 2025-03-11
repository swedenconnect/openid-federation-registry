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

package se.swedenconnect.oidf.entity.registry.controller;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.entity.registry.service.FederationApiService;

import java.util.UUID;

/**
 * FederationService API
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/federationservice")
public class FederationServiceApiController {

  private final FederationApiService federationApiService;

  /**
   * FederationService API
   *
   * @param federationApiService FederationService
   */
  public FederationServiceApiController(final FederationApiService federationApiService) {
    this.federationApiService = federationApiService;
  }

  /**
   * Retrieves trust mark information for a specific instance using the provided instance identifier.
   *
   * @param instanceid the unique identifier for the instance in UUID format.
   * @return a signed JWT containing the trust mark details.
   */
  @GetMapping(value = "/trustmarks_record", produces = "application/jwt")
  public String trustMarkRecord(
      @RequestParam(name = "instanceid") final UUID instanceid) {

    return this.federationApiService.trustMarkRecord(instanceid);
  }

  /**
   * Retrieves a policy record using the provided policy record identifier.
   *
   * @param policyRecordId the unique identifier for the policy record in UUID format.
   * @return a signed JWT containing the policy record details.
   */
  @GetMapping(value = "/policy_record", produces = "application/jwt")
  public String policyRecord(@RequestParam(name = "policy_record_id") final UUID policyRecordId) {
    return this.federationApiService.policyRecord(policyRecordId);
  }

  /**
   * Getting entity records
   *
   * @param issuer Issuer is mandatory
   * @return SignedJWT with a claim for entity_record
   */
  @GetMapping(value = "/entity_record", produces = "application/jwt")
  public String entityRecord(@RequestParam(name = "iss") final String issuer) {
    return this.federationApiService.entityRecord(new EntityID(issuer));
  }

  /**
   * Retrieves submodules using the provided instance identifier.
   *
   * @param instanceId the unique identifier for the instance group.
   * @return a signed JWT containing claims for the entity_record
   */
  @GetMapping(value = "/submodules", produces = "application/jwt")
  public String submoduleRecord(@RequestParam(name = "instanceid") final UUID instanceId) {
    return this.federationApiService.submoduleRecord(instanceId);
  }

}
