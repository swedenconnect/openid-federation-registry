package se.swedenconnect.oidf.entity.registry.audit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.fixture.EntityFactory;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.swedenconnect.oidf.entity.registry.audit.RegistryAuditEventType.ENTITY_CREATED_UPDATE;
import static se.swedenconnect.oidf.entity.registry.audit.RegistryAuditEventType.POLICY_CREATE_UPDATED;

@Slf4j
@ActiveProfiles("h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuditEventIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private AuditEventRepository auditEventRepository;

  @Test
  public void testThatAuditEventsExist() {

    final EntityRecord entity = EntityFactory.createDefaultEntity();
    entity.setPolicyRecordId(createPolicy());

    final ResponseEntity<EntityRecord> response =
        this.restTemplate.postForEntity("/registry/v1/entities", entity, EntityRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final List<AuditEvent> events =  auditEventRepository.find(null,null,ENTITY_CREATED_UPDATE.name());
    assertNotNull(events.getFirst().getPrincipal());
    assertNotNull(events.getFirst().getTimestamp());
    assertNotNull(events.getFirst().getData());
    assertEquals(4,events.getFirst().getData().size());
    assertNotNull(events.getFirst().getData().get("extId"));
    assertNotNull(events.getFirst().getData().get("subject"));
    assertNotNull(events.getFirst().getData().get("issuer"));
    assertNotNull(events.getFirst().getData().get("newData"));


    final List<AuditEvent> policyEvent =  auditEventRepository.find(null,null,POLICY_CREATE_UPDATED.name());
    assertNotNull(policyEvent.getFirst().getPrincipal());
    assertNotNull(policyEvent.getFirst().getTimestamp());
    assertNotNull(policyEvent.getFirst().getData());
    assertEquals(2,policyEvent.getFirst().getData().size());
    assertNotNull(policyEvent.getFirst().getData().get("extId"));
    assertNotNull(policyEvent.getFirst().getData().get("newData"));
  }

  private String createPolicy(){
    final PolicyRecord policy = new PolicyRecord.Builder()
        .name("policy-name")
        .policy(Map.of("key","value"))
        .policyRecordId(UUID.randomUUID().toString())
        .build();
    // Act
    final ResponseEntity<PolicyRecord> response =
        this.restTemplate.postForEntity("/registry/v1/policies", policy, PolicyRecord.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return policy.getPolicyRecordId();
  }

}
