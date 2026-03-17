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

package se.swedenconnect.oidf.registry.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.fixture.TestRestClientFactory;

import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testing api support controller
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class EntityConfigurationControllerIT {
  @LocalServerPort
  protected int port;

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  final String entityConfig =
      "eyJraWQiOiI2YzEwMTY5MC0zNTlmLTQyMmMtYWM1MS1kZjM4MmFhYzBhZjYiLCJ0eXAiOiJlbnRpdHktc3RhdGVtZW50K2p3dCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJodHRwczovL2Rldi5zd2VkZW5jb25uZWN0LnNlL29pZGYtdGVzdC9zZXJ2aWNleiIsIm1ldGFkYXRhIjp7Im9wZW5pZF9yZWx5aW5nX3BhcnR5Ijp7InRva2VuX2VuZHBvaW50X2F1dGhfc2lnbmluZ19hbGciOiJSUzI1NiIsInBvc3RfbG9nb3V0X3JlZGlyZWN0X3VyaXMiOlsiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmLXRlc3Qvc2VydmljZXovbG9nb3V0Il0sImdyYW50X3R5cGVzIjpbImF1dGhvcml6YXRpb25fY29kZSJdLCJqd2tzIjp7ImtleXMiOlt7Imt0eSI6IlJTQSIsImUiOiJBUUFCIiwidXNlIjoic2lnIiwia2lkIjoiMTk5MGQzNTEtNzUwYi00MDFjLThmM2ItZTBiMzUzYTg0MjcxIiwieDVjIjpbIk1JSUM2akNDQWRLZ0F3SUJBZ0lJVXJsZDd4ZHJFdDh3RFFZSktvWklodmNOQVFFTEJRQXdOVEVUTUJFR0ExVUVBd3dLVTJWc1psTnBaMjVsWkRFUk1BOEdBMVVFQ2d3SVQybGtabFJsYzNReEN6QUpCZ05WQkFZVEFsTkZNQjRYRFRJMU1USXhOVEEzTlRJek9Wb1hEVEkzTVRJeE5UQTNOVE16T1Zvd05URVRNQkVHQTFVRUF3d0tVMlZzWmxOcFoyNWxaREVSTUE4R0ExVUVDZ3dJVDJsa1psUmxjM1F4Q3pBSkJnTlZCQVlUQWxORk1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBdENtN1RLN2lhcWpDU2RGM0VmQVlaYkVhMWxiYlhsNjRkS0ZGeG1vdmNNSlhnSFQxMlhzbWwrKzFNalBHbVh0NmYvNkNoZ05hVTQ3U2twQlRIYmVTSGVDNEk1b0dxVzQrUkRrK2YycDNJUFRTSHd5bUZGL1c5TEpoU2lERmdsaEdOWHRvT3hYMGhaRTB1dDZkMnN6Ym03NnZKTVE5dTNKNGRVanMvUThIR3N5RVd2S3NGY1BKTmcwOCtrK3c0cU9xZ1dENGRUTW45TVl5OC9oSEpsWjltTGszWWtldnNkc2wxOTZyN25Bc20zUDNqSzdkOS9pWGZpcHRZUmU1VEtGT0RkVDVXSk5IOXZmMVYxbFJDTkNMa0pQYzFBMHdBdFZIMGRYbnF1VGVtVnJGc2ZCRjBXYjlZZGdZSTJ6dTlaVHFHV1U4aHA1ZStkb2ZlS1JLRk5JdXVRSURBUUFCTUEwR0NTcUdTSWIzRFFFQkN3VUFBNElCQVFDRGg2MjZlOTVOWnpvU0ZYTzVHUWpqUlBVRnp5M2V5NTA2dHErQ0ZzWUw5THh1OGVnVHVvTUZSV0ozWEU2aEQvMkVuMjZzT2ZqR2duVGovN1d4aVlCa3pmcDQzN2lqYTcwUVhDMFZlM01Vay9vakFVaG1BN2QwaE10SmtCd3RsS3RQTlMxcTg1ckRZYWI4OWNrYkZIa0FMTUcyclRWTWZ3YWhnYU05L3BlcHVpY1JTVjRpMDkvakd2OFJHRUNzUkRROWNXdVQ2SVlzTldHQlR2WjNOQ1BKMTQwVWdLUTdRYlFhS1pBU3lQQ1lNcEYvcW4zVnpxUk1IR01IdVYxZ3pjazMzRWxFaGlBTTNURjdCYzJZVGJUc1hkL3NZREdOS1hmVWxwKytkdk95T2VRTE51K1htR1hUdTdCYXl4azdaREtwcmUxWWg4T0JpeFhIV254dWhwM0YiXSwiYWxnIjoiUlMyNTYiLCJuIjoidENtN1RLN2lhcWpDU2RGM0VmQVlaYkVhMWxiYlhsNjRkS0ZGeG1vdmNNSlhnSFQxMlhzbWwtLTFNalBHbVh0NmZfNkNoZ05hVTQ3U2twQlRIYmVTSGVDNEk1b0dxVzQtUkRrLWYycDNJUFRTSHd5bUZGX1c5TEpoU2lERmdsaEdOWHRvT3hYMGhaRTB1dDZkMnN6Ym03NnZKTVE5dTNKNGRVanNfUThIR3N5RVd2S3NGY1BKTmcwOC1rLXc0cU9xZ1dENGRUTW45TVl5OF9oSEpsWjltTGszWWtldnNkc2wxOTZyN25Bc20zUDNqSzdkOV9pWGZpcHRZUmU1VEtGT0RkVDVXSk5IOXZmMVYxbFJDTkNMa0pQYzFBMHdBdFZIMGRYbnF1VGVtVnJGc2ZCRjBXYjlZZGdZSTJ6dTlaVHFHV1U4aHA1ZS1kb2ZlS1JLRk5JdXVRIn1dfSwiYXBwbGljYXRpb25fdHlwZSI6IndlYiIsImxvZ29fdXJpIjoiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmLXRlc3Qvc2VydmljZXovbG9nby5wbmciLCJkZXNjcmlwdGlvbiI6IkRlc2NyaXB0aW9uOnNlcnZpY2V6IiwicmVkaXJlY3RfdXJpcyI6WyJodHRwczovL2Rldi5zd2VkZW5jb25uZWN0LnNlL29pZGYtdGVzdC9zZXJ2aWNlei9jYiJdLCJkaXNwbGF5X25hbWUiOiJEaXNwTmFtZTpzZXJ2aWNleiIsInRva2VuX2VuZHBvaW50X2F1dGhfbWV0aG9kIjoicHJpdmF0ZV9rZXlfand0IiwidXNlcmluZm9fc2lnbmVkX3Jlc3BvbnNlX2FsZyI6IlJTMjU2IiwiY2xpZW50X25hbWUiOiJUZXN0IFNlcnZpY2U6c2VydmljZXoiLCJjb250YWN0cyI6WyJzZXJ2aWNlekBkaWdnLnNlIl0sInJlc3BvbnNlX3R5cGVzIjpbImNvZGUiXSwiaWRfdG9rZW5fc2lnbmVkX3Jlc3BvbnNlX2FsZyI6IlJTMjU2In19LCJqd2tzIjp7ImtleXMiOlt7Imt0eSI6IlJTQSIsImUiOiJBUUFCIiwidXNlIjoic2lnIiwia2lkIjoiNmMxMDE2OTAtMzU5Zi00MjJjLWFjNTEtZGYzODJhYWMwYWY2IiwieDVjIjpbIk1JSUM2akNDQWRLZ0F3SUJBZ0lJSE5aMjZmekZCZ293RFFZSktvWklodmNOQVFFTEJRQXdOVEVUTUJFR0ExVUVBd3dLVTJWc1psTnBaMjVsWkRFUk1BOEdBMVVFQ2d3SVQybGtabFJsYzNReEN6QUpCZ05WQkFZVEFsTkZNQjRYRFRJMU1USXhOVEEzTlRJek9Gb1hEVEkzTVRJeE5UQTNOVE16T0Zvd05URVRNQkVHQTFVRUF3d0tVMlZzWmxOcFoyNWxaREVSTUE4R0ExVUVDZ3dJVDJsa1psUmxjM1F4Q3pBSkJnTlZCQVlUQWxORk1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBbWl4ZlF5emhnUHJ4ekRLdFNsdVJTcGZTTVNlT2xnM1hkUmNEUkF3K09zMThtWTB3WWVQL3ZDYWpCZHNXMmlNZnpDOXNNalFFRERJYmZvQ0xnNWNRbHc4QVV4ek9Wb2trZS9KOHU1VUlpK2kydnZrMHhzcjdjWUYvYi9xVlc2QXJKM1AralBETDRTWE1hSHNtNmVFZWtPeDcybmFPR09MZXRhMHhFYldGaGpkRGlrV1c3dXVaam5mZmh0dTVCL1dnQ200dDRKMnQyVHhYWXNWLzdRdzN4TWY0UGtNNzdDMUxkVG9BYnFQaDBxMVR2QmhhVjBCdDQ3elMzZCtqcGhmdERNTGR5dzVBMWM1Ky91N1dBUGlSTDdaU2Z6UEdab2hyT1VHU3BGNFVnY3pRK3pZWTh4M0dtNFk2dk1WdWRsVzRLbzBIN1BhM3pWNWVNN1RWa1JMYTl3SURBUUFCTUEwR0NTcUdTSWIzRFFFQkN3VUFBNElCQVFCR09IOXRTZkM3c2ZmdG1KZzZnaFRaVmJEZGZ6dnV2ZTE2R1FYeDZPVUZaOGxKRGZUQ2lzZ0drZm9OY3lnL1g1WXpFNUkyRjNIRXJuMHVBakpWdzZFSzRJVFhmVDdGRzdyY0R6UENsUVUyZW41VEtVaGUrVnA1Rkg2WGhyTkFPb01hbFl1dENueHAyT0Mvbit3YWZvTUVreXQzUkRkVWJYaDNNdVJwTHNlRDkwak5WWHhpMWxvTUVlOHlzalNPNVR3azJHUUczSlEyKzQ4bVNXZXpwR2lDNzdvMVZRZmtQenhCWDVOaTZEMk1TSzlxZysvWlZDSDRLS0E5cGtUZTNHVUpjUEdLN1NhNHliZThFMEJBNlcxdHJEakNTKzhnQkdMdHMxeUtwdXJkRzJ2dS9CR2I5S1pLOGp3NFVMV2lWNnpGMVU5OFh6Q1pnM2hjQ1M4U0lrSUYiXSwiYWxnIjoiUlMyNTYiLCJuIjoibWl4ZlF5emhnUHJ4ekRLdFNsdVJTcGZTTVNlT2xnM1hkUmNEUkF3LU9zMThtWTB3WWVQX3ZDYWpCZHNXMmlNZnpDOXNNalFFRERJYmZvQ0xnNWNRbHc4QVV4ek9Wb2trZV9KOHU1VUlpLWkydnZrMHhzcjdjWUZfYl9xVlc2QXJKM1AtalBETDRTWE1hSHNtNmVFZWtPeDcybmFPR09MZXRhMHhFYldGaGpkRGlrV1c3dXVaam5mZmh0dTVCX1dnQ200dDRKMnQyVHhYWXNWXzdRdzN4TWY0UGtNNzdDMUxkVG9BYnFQaDBxMVR2QmhhVjBCdDQ3elMzZC1qcGhmdERNTGR5dzVBMWM1LV91N1dBUGlSTDdaU2Z6UEdab2hyT1VHU3BGNFVnY3pRLXpZWTh4M0dtNFk2dk1WdWRsVzRLbzBIN1BhM3pWNWVNN1RWa1JMYTl3In1dfSwiaXNzIjoiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmLXRlc3Qvc2VydmljZXoiLCJhdXRob3JpdHlfaGludHMiOlsiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmL2Rvcm90ZWEvaW0iXSwiZXhwIjoxNzY2NTY4NjE0LCJpYXQiOjE3NjYzOTU4MTR9.H5-QbTk9piXnORC7CJKlswND64gUAEzp_1ubIhqGiw_Xmb0Bp0kgBL4E8CShlQeC9AhoPjQC_skPLGatp_eX6XUFK86dL6KLYMxdquBfFvCLqNLawEGBvlbtaIYv9xkltWKuvgLGjsKbAd-FEGVzl3nKEf6pdOzPiCRnHfUIylJ2gciLps33-TcxVqKIiQzzH7RWj4ztsFV_ueDRHDBz2gMI1jijGvFWYXjBD4aPkY6PpBFCPQ9qavJcdrM4_yrCOBL3k1AXUVTpJV8JGkVfBIli8G4jJbQ53ZPqGxTxXhD8wRJZ0qgAJtQY1nfLt8XgwJ23-En6dxb1DlwiY3WNHw";
  WireMockServer wireMockServer;

  @BeforeEach
  public void setUp() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .port(6789)
        .httpsPort(6890)
        .keystorePath("classpath:wiremock-keystore.p12")
        .keystorePassword("Test1234")
        .keystoreType("PKCS12")
        .keyManagerPassword("Test1234")

        .notifier(new ConsoleNotifier("entityconfiguration", true)));
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());

    stubFor(get(urlPathEqualTo("/ok/.well-known/openid-federation"))
        .willReturn(ok(entityConfig)));

    stubFor(get(urlPathEqualTo("/fail/.well-known/openid-federation"))
        .willReturn(ok("Not a valid reply")));

    stubFor(get(urlPathEqualTo("/notfound/.well-known/openid-federation"))
        .willReturn(notFound()));

    stubFor(get(urlPathEqualTo("/error/.well-known/openid-federation"))
        .willReturn(serverError()
            .withHeader("Content-Type", "application/problem+json")
            .withBody(
                "{\"type\":\"about:blank\",\"title\":\"Internal Server Error\",\"status\":500,\"detail\":\"500 Internal Server Error: \\\"{\\\"timestamp\\\":\\\"2026-01-08T12:59:46.778+00:00\\\",\\\"status\\\":500,\\\"error\\\":\\\"Internal Server Error\\\",\\\"path\\\":\\\"/oidf/sc/tmi/trust_mark\\\"}\\\"\",\"instance\":\"/oidf-test/serviceq/.well-known/openid-federation\"}")));
  }

  @AfterEach
  public void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void ipv6() {
    final RestClient restClient = TestRestClientFactory.createAuthenticated(this.port, "entity-registry");

    final HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://fe80::1a2b:3c4d:5e6f:7a8b:" + wireMockServer.httpsPort() + "/error")
          //.body("https://dev.swedenconnect.se/oidf-test/serviceq")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
  }

  @Test
  void getJWKS_Invalid() {
    final RestClient restClient = TestRestClientFactory.createAuthenticated(this.port, "entity-registry");

    final HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://localhost:" + wireMockServer.httpsPort() + "/error")
          //.body("https://dev.swedenconnect.se/oidf-test/serviceq")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));

    final HttpClientErrorException failEx = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://localhost:" + wireMockServer.httpsPort() + "/fail")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(failEx.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));

    final HttpClientErrorException notfound = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://localhost:" + wireMockServer.httpsPort() + "/notfound")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(notfound.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));

    final HttpClientErrorException r = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://127.0.0.1:" + wireMockServer.httpsPort() + "/notfound")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(r.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));

    final HttpClientErrorException unknownHost = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://unknown.host")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(unknownHost.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));

    final HttpClientErrorException blocked = assertThrows(HttpClientErrorException.class, () -> {
      restClient.post()
          .uri("/registry/v1/entityconfiguration/jwks")
          .contentType(MediaType.APPLICATION_JSON)
          .body("https://plars.org")
          .retrieve()
          .toEntity(String.class);
    });
    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));

  }

  @Test
  @Disabled
    // test is disabled since only real DNS can be used to load data from
  void getJWKS_OK() throws ParseException {
    final RestClient restClient = TestRestClientFactory.createAuthenticated(this.port, "entity-registry");

    final ResponseEntity<String> jwks =
        restClient.post()
            .uri("/registry/v1/entityconfiguration/jwks")
            .contentType(MediaType.APPLICATION_JSON)
            //.body("https://localhost:" + wireMockServer.httpsPort() + "/ok")
            .body("https://dev.swedenconnect.se/oidf-test/servicez")
            .retrieve()
            .toEntity(String.class);
    assertThat(jwks.getBody()).isNotNull();
    assertEquals("6c101690-359f-422c-ac51-df382aac0af6", JWKSet.parse(jwks.getBody()).getKeys().getFirst().getKeyID());
  }

}