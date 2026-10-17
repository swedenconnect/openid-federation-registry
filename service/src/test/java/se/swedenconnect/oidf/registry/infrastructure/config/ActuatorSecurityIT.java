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
package se.swedenconnect.oidf.registry.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that only the monitoring actuator endpoints are reachable without authentication. Every endpoint is exposed
 * here on purpose, so that the security rules are tested on their own and not hidden by the exposure setting.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.endpoints.web.exposure.include=*")
@Testcontainers
@AutoConfigureRestTestClient
class ActuatorSecurityIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @DisplayName("Sensitive actuator endpoints are not available without authentication")
  @ParameterizedTest
  @ValueSource(strings = { "heapdump", "env", "configprops", "beans", "mappings", "loggers", "threaddump",
      "prometheus" })
  void sensitiveEndpointsRequireAuthentication(final String endpoint) throws Exception {
    assertThat(this.get("/actuator/" + endpoint).statusCode()).isNotEqualTo(200);
  }

  @DisplayName("Monitoring actuator endpoints are available without authentication")
  @ParameterizedTest
  @ValueSource(strings = { "health", "info" })
  void monitoringEndpointsArePublic(final String endpoint) throws Exception {
    assertThat(this.get("/actuator/" + endpoint).statusCode()).isEqualTo(200);
  }

  private HttpResponse<Void> get(final String path) throws IOException, InterruptedException {
    final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path)).GET().build();
    return this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
  }
}
