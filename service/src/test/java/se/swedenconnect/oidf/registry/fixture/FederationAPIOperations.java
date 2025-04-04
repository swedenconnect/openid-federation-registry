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

package se.swedenconnect.oidf.registry.fixture;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class FederationAPIOperations {

  private final TestRestTemplate restTemplate;

  public FederationAPIOperations(TestRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public SignedJWT callTrustMark(UUID instanceId) throws ParseException {
    return getSignedJWT("trustmarks_record", instanceId);
  }

  public SignedJWT callSubmodule(UUID instanceId) throws ParseException {
    return getSignedJWT("submodules", instanceId);
  }

  public SignedJWT callEntity(UUID instanceId) throws ParseException {
    return getSignedJWT("entity_record", instanceId);
  }

  private @NotNull SignedJWT getSignedJWT(final String action, final UUID instanceId) throws ParseException {

    final ResponseEntity<String> fedRes = this.restTemplate
        .getForEntity("/api/v1/federationservice/%s?instanceid=%s"
            .formatted(action, instanceId), String.class);

    if (fedRes.getStatusCode().isError()) {
      log.error(fedRes.getBody());
    }
    assertThat(HttpStatus.OK).isEqualTo(fedRes.getStatusCode());

    return SignedJWT.parse(Objects.requireNonNull(fedRes.getBody()));
  }
}
