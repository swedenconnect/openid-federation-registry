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

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.text.ParseException;
import java.util.Objects;
import java.util.UUID;

/**
 * Helper class for calling the Federation API.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class FederationAPIOperations {

  private final RestTestClient restTestClient;

  public FederationAPIOperations(RestTestClient restTestClient) {
    this.restTestClient = restTestClient;
  }

  public SignedJWT callSubmodule(UUID instanceId) throws ParseException {
    return getSignedJWT("submodules", instanceId);
  }

  public SignedJWT callEntity(UUID instanceId) throws ParseException {
    return getSignedJWT("entity_record", instanceId);
  }

  private @NotNull SignedJWT getSignedJWT(final String action, final UUID instanceId) throws ParseException {

    final String body = this.restTestClient.get()
        .uri("/api/v1/federationservice/%s?instanceid=%s".formatted(action, instanceId))
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();

    return SignedJWT.parse(Objects.requireNonNull(body));
  }
}