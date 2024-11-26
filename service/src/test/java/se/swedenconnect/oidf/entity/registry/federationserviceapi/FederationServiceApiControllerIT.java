package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.util.EntityFactory;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing federation api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class FederationServiceApiControllerIT {
  @Autowired
  private TestRestTemplate restTemplate;


  @Test
  void trustMarkRecordNotFound() {
    final ResponseEntity<String> fedRes = restTemplate
        .getForEntity("/api/v1/federationservice/trust_mark"
            + "?iss=http://tmi.digg.se&trustmark_id=http://www.digg.se/loa", String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.NOT_FOUND).isEqualTo(fedRes.getStatusCode());
  }

  @Test
  void policyRecordSuccess() throws ParseException {
    final PolicyRecord policy = new PolicyRecord.Builder()
        .name("policy-name")
        .policy(" {\"policy\":\"default\"}")
        .build();

    final ResponseEntity<PolicyRecord> response = this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> fedRes = restTemplate
        .getForEntity("/api/v1/federationservice/policy_record?policy_id="+response.getBody().getPolicyId(), String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(fedRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    

    final SignedJWT signedJWT  = SignedJWT.parse(fedRes.getBody());
    assertEquals(new JOSEObjectType("policy-record+jwt"),signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());
    final List<Object> claim = signedJWT.getJWTClaimsSet().getListClaim("policy_record");
    assertNotNull(claim);
  }

  @Test
  void policyRecordNotFound() throws ParseException {

    final ResponseEntity<String> fedRes = restTemplate
        .getForEntity("/api/v1/federationservice/policy_record?policy_id=" + UUID.randomUUID(), String.class);
    if(fedRes.getStatusCode().isError()){
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.NOT_FOUND).isEqualTo(fedRes.getStatusCode());


  }

  @Test
  void entityRecordSuccess() throws ParseException {
    final String issuer = "http://tmi.digg.se/" + UUID.randomUUID();
    final EntityRecord entity = EntityFactory.createDefaultEntity(issuer,"http://sub.digg.se");
    final ResponseEntity<EntityRecord> createResponse = restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> response = restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss="+issuer, String.class);

    if(response.getStatusCode().isError()){
      log.error(response.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(response.getStatusCode());

    final SignedJWT signedJWT  = SignedJWT.parse(response.getBody());
    assertEquals(new JOSEObjectType("entity-record+jwt"),signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());
    final List<Object> claim = signedJWT.getJWTClaimsSet().getListClaim("entity_record");
    assertNotNull(claim);

  }

  @Test
  void entityRecordNotFound() throws ParseException {

    final ResponseEntity<String> response = restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=http://notfound.digg.se", String.class);
    if(response.getStatusCode().isError()){
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

  }

  @Test
  void entityRecordBadRequest() throws ParseException {

    final ResponseEntity<String> response = restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=f", String.class);
    if(response.getStatusCode().isError()){
      log.error(response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

  }
}