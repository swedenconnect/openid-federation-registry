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

package se.swedenconnect.oidf.entity.registry.federationserviceapi;


import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
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
   * @param federationApiService FederationService
   */
  public FederationServiceApiController(final FederationApiService federationApiService) {
    this.federationApiService = federationApiService;
  }

  /**
   * Getting a trust_mark sub record
   * @param issuer Issuer is mandatory
   * @param trustmarkId Trustmark is mandatory
   * @param subject Subject is optional
   * @return SignedJWT with a claim for trust_mark
   */
  @GetMapping(value="/trustmarksubject_record", produces = "application/jwt")
  public String trustMarkRecord(
      @RequestParam(name="iss") final String issuer,
      @RequestParam(name="trustmark_id") final String trustmarkId,
      @RequestParam(name="sub", required = false) final String subject){

    return this.federationApiService.trustMarkRecord(new EntityID(issuer),trustmarkId, Optional.ofNullable(subject));
  }

  /**
   * Getting policy by there policy_record_id
   * @param policyRecordId PolicyRecordId for this record
   * @return SignedJWT with a claim for policy_record
   */
  @GetMapping(value="/policy_record", produces = "application/jwt")
  public String policyRecord(@RequestParam(name="policy_record_id") final UUID policyRecordId){
    return this.federationApiService.policyRecord(policyRecordId);
  }

  /**
   * Getting entity records
   * @param issuer Issuer is mandatory
   * @return SignedJWT with a claim for entity_record
   */
  @GetMapping(value="/entity_record", produces = "application/jwt")
  public String entityRecord(@RequestParam(name="iss") final String issuer){
    return this.federationApiService.entityRecord(new EntityID(issuer));
  }



}
