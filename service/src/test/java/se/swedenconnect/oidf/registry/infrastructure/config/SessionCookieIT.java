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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the attributes of the session cookie with the session and proxy settings from the main
 * {@code application.yml}, which the test configuration does not include. TLS is terminated in a proxy, and with
 * {@code forward-headers-strategy: framework} Tomcat, which creates the session cookie, never sees https. The cookie
 * must therefore be configured as {@code Secure} explicitly.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.servlet.session.cookie.same-site=lax",
    "server.servlet.session.cookie.secure=true",
    "server.forward-headers-strategy=framework"
})
@Testcontainers
@AutoConfigureRestTestClient
class SessionCookieIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  @DisplayName("The session cookie is Secure, HttpOnly and SameSite=Lax")
  void sessionCookieIsSecure() throws Exception {
    assertThat(this.sessionCookie(Map.of()))
        .contains("; Secure")
        .contains("; HttpOnly")
        .contains("; SameSite=Lax");
  }

  @Test
  @DisplayName("The session cookie is Secure behind a TLS-terminating proxy")
  void sessionCookieIsSecureBehindProxy() throws Exception {
    assertThat(this.sessionCookie(Map.of("X-Forwarded-Proto", "https", "X-Forwarded-Host", "registry.example.se")))
        .contains("; Secure")
        .contains("; HttpOnly")
        .contains("; SameSite=Lax");
  }

  /**
   * Starts a login, which stores the authorization request in a new session.
   */
  private String sessionCookie(final Map<String, String> headers) throws IOException, InterruptedException {
    final HttpRequest.Builder request = HttpRequest.newBuilder(
        URI.create("http://localhost:" + this.port + "/oauth2/authorization/entity-registry")).GET();
    headers.forEach(request::header);
    final HttpResponse<Void> response = this.httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
    return response.headers().allValues("Set-Cookie").stream()
        .filter(header -> header.startsWith("JSESSIONID="))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No JSESSIONID cookie in " + response.headers().map()));
  }
}
