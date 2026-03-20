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
package se.swedenconnect.oidf.registry.fixture;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.registry.infrastructure.auth.AuthConstants;

/**
 * Test utility class for creating a RestClient instance for use in tests. Makes call to our test authorization server
 * to get an access token.
 *
 * @author Felix Hellman
 * @author Per Fredrik Plars
 */
public class TestRestClientFactory {

  public static RestClient createAuthenticated(int port, String registrationId) {
    HttpClient client = HttpClientBuilder.create()
        .setDefaultCookieStore(new BasicCookieStore())
        .build();

    final HttpComponentsClientHttpRequestFactory clientHttpRequestFactory =
        new HttpComponentsClientHttpRequestFactory(client);
    final RestClient restClient = RestClient.builder()
        .requestFactory(clientHttpRequestFactory)
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + new JwtTestUtils().createJwt(JwtTestUtils.OrganisationType.PM))
        .defaultHeader(AuthConstants.SELECTED_ORG_NUMBER_ATTRIBUTE, JwtTestUtils.OrganisationType.PM.orgId)
        .baseUrl("http://localhost:%d".formatted(port))
        .build();
/*
    final ResponseEntity<String> response =
        restClient.get().uri("/oauth2/authorization/" + registrationId)
            .retrieve()
            .toEntity(String.class);
    Assertions.assertEquals(200, response.getStatusCode().value());
  */
    return restClient;
  }
}
