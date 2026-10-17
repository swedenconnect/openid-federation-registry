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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the CSRF protection of state-changing requests. The SPA is authorized by the session cookie, so such a
 * request must carry the token from the {@code XSRF-TOKEN} cookie in the {@code X-XSRF-TOKEN} header. A
 * {@code text/plain} POST is used, since a browser sends it cross-site without a CORS preflight. The CSRF check runs
 * before authentication, so a rejected token gives 403 while a request that passes it gives 401 without credentials.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class CsrfProtectionIT {

  private static final String PATH = "/registry/v1/Swedenconnect/%s/entityconfiguration/view"
      .formatted(JwtTestUtils.OrganisationType.PM.orgId);

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  @DisplayName("A POST without a CSRF token is rejected with 403")
  void postWithoutCsrfTokenIsRejected() throws Exception {
    assertThat(this.post(Map.of()).statusCode()).isEqualTo(403);
  }

  @Test
  @DisplayName("A POST with a CSRF token that does not match the cookie is rejected with 403")
  void postWithWrongCsrfTokenIsRejected() throws Exception {
    final String token = this.fetchCsrfToken();

    assertThat(this.post(Map.of("Cookie", "XSRF-TOKEN=" + token, "X-XSRF-TOKEN", "wrong")).statusCode())
        .isEqualTo(403);
  }

  @Test
  @DisplayName("A POST with the token from the XSRF-TOKEN cookie in the X-XSRF-TOKEN header passes the CSRF check")
  void postWithCsrfTokenPassesCsrfCheck() throws Exception {
    final String token = this.fetchCsrfToken();

    assertThat(this.post(Map.of("Cookie", "XSRF-TOKEN=" + token, "X-XSRF-TOKEN", token)).statusCode())
        .isEqualTo(401);
  }

  @Test
  @DisplayName("Logout with GET is not possible")
  void logoutWithGetIsNotPossible() throws Exception {
    final HttpResponse<Void> response = this.httpClient.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/logout")).GET().build(),
        HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isNotEqualTo(302);
  }

  @Test
  @DisplayName("Logout with POST is rejected with 403 without a CSRF token")
  void logoutWithoutCsrfTokenIsRejected() throws Exception {
    assertThat(this.postLogout(Map.of(), "").statusCode()).isEqualTo(403);
  }

  @Test
  @DisplayName("Logout with POST and the token from the XSRF-TOKEN cookie as the _csrf form parameter is performed")
  void logoutWithCsrfParameterIsPerformed() throws Exception {
    final String token = this.fetchCsrfToken();

    final HttpResponse<Void> response = this.postLogout(Map.of("Cookie", "XSRF-TOKEN=" + token), "_csrf=" + token);

    assertThat(response.statusCode()).isEqualTo(302);
  }

  @Test
  @DisplayName("A POST with a bearer token does not need a CSRF token")
  void postWithBearerTokenIsExempt() throws Exception {
    final String bearer = this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM, "swedenconnect", "admin");

    assertThat(this.post(Map.of("Authorization", "Bearer " + bearer)).statusCode()).isNotIn(401, 403);
  }

  private String fetchCsrfToken() throws IOException, InterruptedException {
    final HttpResponse<Void> response = this.httpClient.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/")).GET().build(),
        HttpResponse.BodyHandlers.discarding());
    return response.headers().allValues("Set-Cookie").stream()
        .flatMap(header -> HttpCookie.parse(header).stream())
        .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
        .map(HttpCookie::getValue)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No XSRF-TOKEN cookie in " + response.headers().map()));
  }

  private HttpResponse<Void> postLogout(final Map<String, String> headers, final String form)
      throws IOException, InterruptedException {
    final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/logout"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form));
    headers.forEach(request::header);
    return this.httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
  }

  private HttpResponse<Void> post(final Map<String, String> headers) throws IOException, InterruptedException {
    final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + PATH))
        .header("Content-Type", "text/plain")
        .POST(HttpRequest.BodyPublishers.ofString("https://entity.example.se"));
    headers.forEach(request::header);
    return this.httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
  }
}
