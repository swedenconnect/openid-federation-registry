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

package se.swedenconnect.oidf.entity.registry.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.entity.registry.audit.FederationAuditEvent;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditEventType;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
class NotifyServiceTest {

  private NotifyService notifyService;
  final AtomicInteger counter = new AtomicInteger(0);

  @BeforeEach
  void setUp() throws JOSEException {

    final RSAKey rsaJWK = new RSAKeyGenerator(2048)
        .keyID("rsa-key-id")
        .generate();

    notifyService = new NotifyService(
        null,
        List.of(URI.create("http://localhost:8080/test1"),
            URI.create("http://localhost:8080/test2"),
            URI.create("http://localhost:8080/test3"),
            URI.create("http://localhost:8080/test4"),
            URI.create("http://localhost:8080/test5")),
        rsaJWK) {

      @Override
      protected void callNotifyEndpoint(final URI endpoint, final String payload) {
        counter.incrementAndGet();
        if (ThreadLocalRandom.current().nextBoolean()) {
          throw new RuntimeException("Failed request");
        }
        System.out.println(String.format("Calling notify endpoint: %s JWT:%s", endpoint, payload));
      }
    };

  }

  @Test
  void testOnAuditEvent() throws InterruptedException {

    Stream.generate(() -> FederationAuditEvent.builder()
            .fkKeyType(FkKeyType.TRUSTMARKSUBJECT)
            .event(RegistryAuditEventType.ENTITY_CREATED_UPDATE)
            .optionId(UUID.randomUUID())
            .build())
        .limit(100)
        .forEach(notifyService::onAuditEvent);

    Thread.sleep(Duration.ofSeconds(1).toMillis());
    assertEquals(500, counter.get());
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    notifyService.destroy();
  }

}