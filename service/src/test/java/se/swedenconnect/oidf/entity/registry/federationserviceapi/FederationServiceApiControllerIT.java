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

package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.fixture.EntityFactory;
import se.swedenconnect.oidf.entity.registry.fixture.PolicyFactory;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing federation api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class FederationServiceApiControllerIT {
  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void trustMarkRecordNotFound() {
    final ResponseEntity<String> fedRes = this.restTemplate
        .getForEntity("/api/v1/federationservice/trustmarksubject_record"
            + "?iss=http://tmi.swedenconnect.se&trustmark_id=http://www.swedenconnect.se/loa", String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.NOT_FOUND).isEqualTo(fedRes.getStatusCode());
  }

  /**
   * Creating TrustMarkSubjectRecord then gets the federation JWT to make sure it works
   * @throws ParseException
   */
  @Test
  void trustMarkRecordSuccess() throws ParseException, JsonProcessingException {

    final TrustMarkSubjectRecord record = TrustMarkSubjectRecord.builder()
        .trustMarkSubjectRecordId(UUID.randomUUID().toString())
        .issuer("http://www.swedenconnect.se/issuer")
        .trustMarkId("http://www.swedenconnect.se/trustmarkid")
        .subject("http://www.swedenconnect.se/subject")
        .revoked(true)
        .granted(OffsetDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS))
        .expires(OffsetDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS))
        .build();

    final ResponseEntity<String> response =
        this.restTemplate.postForEntity("/registry/v1/trustmarksubjects", record, String.class);
    if (response.getStatusCode().isError()) {
      log.info(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> fedResEmptySub = this.restTemplate
        .getForEntity("/api/v1/federationservice/trustmarksubject_record"
            + "?iss=%s&trustmark_id=%s&sub=".formatted(record.getIssuer(),record.getTrustMarkId()), String.class);
    if(fedResEmptySub.getStatusCode().isError()){
      log.error(fedResEmptySub.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(fedResEmptySub.getStatusCode());

    final ResponseEntity<String> fedResSubjectSearch = this.restTemplate
        .getForEntity("/api/v1/federationservice/trustmarksubject_record"
            + "?iss=%s&trustmark_id=%s".formatted(record.getIssuer(),record.getTrustMarkId()), String.class);
    if(fedResSubjectSearch.getStatusCode().isError()){
      log.error(fedResSubjectSearch.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(fedResSubjectSearch.getStatusCode());


    final ResponseEntity<String> fedRes = this.restTemplate
        .getForEntity("/api/v1/federationservice/trustmarksubject_record"
            + "?iss=%s&trustmark_id=%s".formatted(record.getIssuer(),record.getTrustMarkId()), String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(fedRes.getStatusCode());

    final SignedJWT tms = SignedJWT.parse(Objects.requireNonNull(fedRes.getBody()));
    final JWTClaimsSet claimsSet = tms.getJWTClaimsSet();
    final List<Object> records = claimsSet.getListClaim("trustmark_records");

    records.stream()
        .map(o -> (Map<String,Object>)o)
        .forEach(claimMap -> {
          log.info("Record:{} Claim{}", record, claimMap.toString());
          assertEquals(record.getSubject(), claimMap.get("subject"));
          assertNotNull(claimMap.get("expires"));
          assertNotNull(claimMap.get("granted"));
          assertEquals(record.getExpires().toString(), claimMap.get("expires"));
          assertEquals(record.getGranted().toString(), claimMap.get("granted"));
          assertEquals(record.getRevoked(), claimMap.get("revoked"));

        });

  }

  @Test
  void policyRecordSuccess() throws ParseException {
    final PolicyRecord policy = PolicyFactory.record();

    final ResponseEntity<PolicyRecord> response =
        this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> fedRes = this.restTemplate
        .getForEntity("/api/v1/federationservice/policy_record?policy_record_id="+response.getBody().getPolicyRecordId(), String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(fedRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    

    final SignedJWT signedJWT  = SignedJWT.parse(fedRes.getBody());
    assertEquals(new JOSEObjectType("policy-record+jwt"),signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());

    final Map<String, Object> claim = signedJWT.getJWTClaimsSet().getJSONObjectClaim("policy_record");
    assertNotNull(claim);
    assertTrue(!claim.isEmpty());
    assertThat( (String)claim.get("policy_record_id")).isNotEmpty();
    assertNotNull(claim.get("policy"));
    final Map<String,Object> policyClaim = (Map<String, Object>) claim.get("policy");
    assertNotNull(policyClaim.get("openid_relying_party"));

  }

  @Test
  void policyRecordBadRequest() throws ParseException {

    final ResponseEntity<String> fedRes = this.restTemplate
        .getForEntity("/api/v1/federationservice/policy_record?policy_record_id=notAnUuid", String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.BAD_REQUEST).isEqualTo(fedRes.getStatusCode());

  }

  @Test
  void policyRecordNotFound() throws ParseException {

    final ResponseEntity<String> fedRes = this.restTemplate
        .getForEntity("/api/v1/federationservice/policy_record?policy_record_id=" + UUID.randomUUID(), String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.NOT_FOUND).isEqualTo(fedRes.getStatusCode());


  }

  @Test
  void entityRecordSuccess() throws ParseException {
    final String issuer = "http://tmi.digg.se/" + UUID.randomUUID();
    final EntityRecord entity = EntityFactory.createDefaultEntity(issuer,"http://sub.digg.se");
    entity.setPolicyRecordId(createPolicy());
    final ResponseEntity<EntityRecord> createResponse =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss="+issuer, String.class);

    if(response.getStatusCode().isError()){
      log.error(response.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(response.getStatusCode());

    final SignedJWT signedJWT  = SignedJWT.parse(response.getBody());
    assertEquals(new JOSEObjectType("entity-records+jwt"),signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());

    final List<Object> claim = signedJWT.getJWTClaimsSet().getListClaim("entity_records");
    assertNotNull(claim);
    assertTrue(!claim.isEmpty());

  }

  @Test
  void submoduleRecordSuccess() throws ParseException {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/submodules?instanceid=" + UUID.randomUUID(), String.class);

    if (response.getStatusCode().isError()) {
      log.error(response.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(response.getStatusCode());

    final SignedJWT signedJWT = SignedJWT.parse(response.getBody());
    assertEquals(new JOSEObjectType("module-records+jwt"), signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());

    final Map<String, Object> claim = signedJWT.getJWTClaimsSet().getJSONObjectClaim("module_records");
    assertNotNull(claim);
    assertTrue(!claim.isEmpty());
    final ModuleResponse moduleResponse = ModuleResponse.fromJson(claim);
    assertNotNull(moduleResponse.getResolvers());
    assertNotNull(moduleResponse.getTrustAnchors());
    assertNotNull(moduleResponse.getTrustMarkIssuers());

    assertTrue(moduleResponse.getResolvers().isEmpty());
    assertTrue(moduleResponse.getTrustAnchors().isEmpty());
    assertTrue(moduleResponse.getTrustMarkIssuers().isEmpty());

  }

  @Test
  void entityRecordNotFound() throws ParseException {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=http://notfound.digg.se", String.class);
    if(response.getStatusCode().isError()){
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

  }

  @Test
  void entityRecordBadRequest() throws ParseException {

    final ResponseEntity<String> response = this.restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=f", String.class);
    if(response.getStatusCode().isError()){
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

  }

  private String createPolicy(){
    final PolicyRecord policy = PolicyFactory.record();
    // Act
    final ResponseEntity<PolicyRecord> response =
        this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return policy.getPolicyRecordId();
  }

}