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

package se.swedenconnect.oidf.registry.service;

import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.registry.federation.service.NotifyService;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventType;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;

import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Unit tests for the {@link NotifyService} class
 *
 * @author Per Fredrik Plars
 */
@Slf4j
class NotifyServiceTest {
  @RegisterExtension
  static final WireMockExtension wireMockExtension = WireMockExtension.newInstance()
      .options(options().dynamicPort())
      .build();
  final UUID organizationUUID = UUID.randomUUID();
  final UUID instanceUUID = UUID.randomUUID();
  private NotifyService notifyService;

  @BeforeEach
  void setUp() throws JOSEException {
    final RSAKey rsaJWK = new RSAKeyGenerator(2048)
        .keyID("NotifyId")
        .generate();

    notifyService = new NotifyService(
        RestClient.builder().build(),
        List.of(
            new RegistryProperties.FederationAPIProperties.NotificationProperties(
                URI.create(wireMockExtension.baseUrl() + "/test1"),
                instanceUUID),
            new RegistryProperties.FederationAPIProperties.NotificationProperties(
                URI.create(wireMockExtension.baseUrl() + "/test2"),
                instanceUUID),
            new RegistryProperties.FederationAPIProperties.NotificationProperties(
                URI.create(wireMockExtension.baseUrl() + "/test3"),
                instanceUUID),
            new RegistryProperties.FederationAPIProperties.NotificationProperties(
                URI.create(wireMockExtension.baseUrl() + "/test4"),
                instanceUUID),
            new RegistryProperties.FederationAPIProperties.NotificationProperties(
                URI.create(wireMockExtension.baseUrl() + "/test5"),
                instanceUUID)
        ),
        rsaJWK
    );

    wireMockExtension.stubFor(WireMock.post(WireMock.urlMatching("/test[1-5]"))
        .withRequestBody(WireMock.matching(".*"))
        .willReturn(WireMock.aResponse().withStatus(200))); // Respond with 200 OK
  }

  private RegistryAuditEventType getAuditEventType() {
    final int length = RegistryAuditEventType.values().length;
    final int random = new Random().nextInt(length);
    return RegistryAuditEventType.values()[random];
  }

  @Test
  @DisplayName( "Notify service - should reply")
  void testNotifyServiceWithWireMock() throws InterruptedException {
    Stream.generate(() -> FederationAuditEvent.builder()
            .event(getAuditEventType())
            .extId(new Random().nextInt(1000) + "")
            .organizationId(organizationUUID.toString())
            .instanceId(instanceUUID.toString())
            .build())
        .limit(50)
        .forEach(notifyService::onAuditEvent);

    for (int i = 0; i < 5; i++) {
      try {
        Thread.sleep(1000);
        wireMockExtension.verify(new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN, 1),
            WireMock.postRequestedFor(WireMock.urlMatching("/test[1-5]")));
        return;
      }
      catch (VerificationException e) {
        log.warn("Failed to verify notification looping:{}", i, e);
      }
    }

    // We should never get here
    throw new AssertionError("Did not receive any notifications on the wiremock endpoints");
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    notifyService.destroy();
  }

}