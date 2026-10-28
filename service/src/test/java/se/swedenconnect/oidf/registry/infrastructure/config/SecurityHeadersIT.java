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
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Content-Security-Policy, Referrer-Policy and Permissions-Policy headers. The GUI gets the strict policy,
 * also on API responses and error responses, while Swagger UI gets the milder one.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class SecurityHeadersIT {

  @LocalServerPort
  private int port;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  @DisplayName("The GUI is served with the strict Content-Security-Policy")
  void guiHasStrictPolicy() throws Exception {
    assertThat(this.get("/").firstValue("Content-Security-Policy"))
        .hasValue(ContentSecurityPolicies.gui(Set.of()));
  }

  @Test
  @DisplayName("An unauthenticated API request gets the strict Content-Security-Policy")
  void apiHasStrictPolicy() throws Exception {
    assertThat(this.get("/registry/v1/tenants").firstValue("Content-Security-Policy"))
        .hasValue(ContentSecurityPolicies.gui(Set.of()));
  }

  @Test
  @DisplayName("Swagger UI gets the milder Content-Security-Policy")
  void swaggerHasMildPolicy() throws Exception {
    assertThat(this.get("/swagger-ui.html").firstValue("Content-Security-Policy"))
        .hasValue(ContentSecurityPolicies.SWAGGER);
    assertThat(this.get("/swagger-ui/index.html").firstValue("Content-Security-Policy"))
        .hasValue(ContentSecurityPolicies.SWAGGER);
  }

  @Test
  @DisplayName("Referrer-Policy and Permissions-Policy are sent")
  void referrerAndPermissionsPolicyAreSent() throws Exception {
    final HttpHeaders headers = this.get("/");

    assertThat(headers.firstValue("Referrer-Policy")).hasValue("same-origin");
    assertThat(headers.firstValue("Permissions-Policy")).hasValueSatisfying(policy -> assertThat(policy)
        .contains("camera=()")
        .contains("geolocation=()"));
  }

  private HttpHeaders get(final String path) throws IOException, InterruptedException {
    return this.httpClient.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path)).GET().build(),
        HttpResponse.BodyHandlers.discarding()).headers();
  }
}
