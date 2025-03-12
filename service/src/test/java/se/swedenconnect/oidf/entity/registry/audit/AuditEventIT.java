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

package se.swedenconnect.oidf.entity.registry.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.swedenconnect.oidf.entity.registry.audit.RegistryAuditEventType.OPTIONS_CREATED;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuditEventIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  TestDataOperations testDataOperations;

  @Autowired
  private AuditEventRepository auditEventRepository;

  @Test
  public void testThatAuditEventsExist() throws JsonProcessingException {

    final UUID entityId = testDataOperations.createHostedEntity(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultHostedEntity());

    final UUID tmiId1 = testDataOperations.createTMI(UUID.randomUUID(),
        JwtTestUtils.OrganisationType.SKATT,
        HttpStatus.CREATED,
        TestDataOperations.defaultTrustMarkIssuer(entityId));


    final List<AuditEvent> events = auditEventRepository.find(null, null, OPTIONS_CREATED.name());
    assertNotNull(events.getFirst().getPrincipal());
    assertNotNull(events.getFirst().getTimestamp());
    assertNotNull(events.getFirst().getData());
    assertTrue(events.getFirst().getData().size() >= 4);
    assertNotNull(events.getFirst().getData().get("optionType"));
    assertNotNull(events.getFirst().getData().get("oldData"));
    assertNotNull(events.getFirst().getData().get("newData"));
    assertNotNull(events.getFirst().getData().get("optionId"));

    final List<AuditEvent> policyEvent = auditEventRepository.find(null, null, OPTIONS_CREATED.name());
    assertNotNull(policyEvent.getFirst().getPrincipal());
    assertNotNull(policyEvent.getFirst().getTimestamp());
    assertNotNull(policyEvent.getFirst().getData());
    assertTrue(policyEvent.getFirst().getData().size() >= 2);
    assertNotNull(events.getFirst().getData().get("optionType"));
    assertNotNull(events.getFirst().getData().get("oldData"));
    assertNotNull(events.getFirst().getData().get("newData"));
    assertNotNull(events.getFirst().getData().get("optionId"));
  }
}
