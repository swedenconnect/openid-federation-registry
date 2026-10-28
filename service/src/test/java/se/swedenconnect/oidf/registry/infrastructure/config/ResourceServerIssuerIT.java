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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@code iss} claim of access tokens is checked when the keys are fetched from a JWK set, which is
 * how the registry is deployed. The JWK set holds the test signing key and is served by a local HTTP server.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class ResourceServerIssuerIT {

  private static final String TENANT = "Swedenconnect";

  private static final HttpServer JWKS_SERVER = startJwksServer();

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @DynamicPropertySource
  static void resourceServerProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.public-key-location", () -> "");
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/certs");
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> JwtTestUtils.ISSUER);
  }

  @AfterAll
  static void stopJwksServer() {
    JWKS_SERVER.stop(0);
  }

  @Test
  @DisplayName("A bearer token from the configured issuer is accepted")
  void tokenFromConfiguredIssuerIsAccepted() throws Exception {
    final String token = this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM, "account",
        JwtTestUtils.ISSUER, List.of(Map.of("function", "swedenconnect", "right", "admin")));

    assertThat(this.getEntities(token).statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("A bearer token with a mismatched iss claim is rejected with 401")
  void tokenWithMismatchedIssuerIsRejected() throws Exception {
    final String token = this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM, "account",
        "https://other-issuer.example.se", List.of(Map.of("function", "swedenconnect", "right", "admin")));

    assertThat(this.getEntities(token).statusCode()).isEqualTo(401);
  }

  private HttpResponse<Void> getEntities(final String token) throws IOException, InterruptedException {
    final URI uri = URI.create("http://localhost:%d/registry/v1/%s/%s/entities"
        .formatted(this.port, TENANT, JwtTestUtils.OrganisationType.PM.orgId));
    final HttpRequest request = HttpRequest.newBuilder(uri)
        .header("Authorization", "Bearer " + token)
        .GET()
        .build();
    return this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
  }

  private static HttpServer startJwksServer() {
    try {
      final String pem = new ClassPathResource("public_auth_cert.pem").getContentAsString(StandardCharsets.UTF_8);
      final byte[] jwks = new JWKSet(JWK.parseFromPEMEncodedObjects(pem)).toString(true)
          .getBytes(StandardCharsets.UTF_8);
      final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext("/certs", exchange -> {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jwks.length);
        try (OutputStream body = exchange.getResponseBody()) {
          body.write(jwks);
        }
      });
      server.start();
      return server;
    }
    catch (final Exception e) {
      throw new IllegalStateException("Could not start the JWK set server", e);
    }
  }
}
