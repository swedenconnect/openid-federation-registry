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

package se.swedenconnect.oidf.entity.registry.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing the new optional api
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OptionsApiPolicyControllerIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");
  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void testCRUDPolicies() throws IOException {

    final String id_skatt = TestDataOperations.createPolicies(restTemplate, JwtTestUtils.OrganisationType.SKATT);
    getPolicy(id_skatt, HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);
    delete(id_skatt, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.AF);
    delete(id_skatt, HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);
    getPolicy(id_skatt, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.SKATT);

    final String af = TestDataOperations.createPolicies(restTemplate, JwtTestUtils.OrganisationType.AF);
    getPolicy(af, HttpStatus.OK, JwtTestUtils.OrganisationType.AF);
    TestDataOperations.updatePolicies(restTemplate, JwtTestUtils.OrganisationType.AF, af, Map.of("name", "update"));

    getPolicy(af, HttpStatus.NOT_FOUND, JwtTestUtils.OrganisationType.PM);
  }

  @Test
  public void testListPolicies() throws IOException {

    TestDataOperations.createPolicies(restTemplate, JwtTestUtils.OrganisationType.SKATT);
    TestDataOperations.createPolicies(restTemplate, JwtTestUtils.OrganisationType.SKATT);

    TestDataOperations.createPolicies(restTemplate, JwtTestUtils.OrganisationType.AF);
    TestDataOperations.createPolicies(restTemplate, JwtTestUtils.OrganisationType.AF);

    String response = getPolicyList(HttpStatus.OK, JwtTestUtils.OrganisationType.SKATT);
    System.out.println(response);

    String af = getPolicyList(HttpStatus.OK, JwtTestUtils.OrganisationType.AF);
    System.out.println(af);

    String pm = getPolicyList(HttpStatus.OK, JwtTestUtils.OrganisationType.PM);
    System.out.println(pm);
  }

  private void delete(final String policy_id, HttpStatus httpStatus,
      final JwtTestUtils.OrganisationType organizationType) {
    final HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organizationType));
    final HttpEntity<String> entity = new HttpEntity<>(headers);

    final ResponseEntity<Void> response = restTemplate.exchange(
        "/registry/v1/options/policies/" + policy_id,
        HttpMethod.DELETE,
        entity,
        Void.class
    );
    assertThat(response.getStatusCode()).isEqualTo(httpStatus);
  }

  private String getPolicy(String id, HttpStatus httpStatus, JwtTestUtils.OrganisationType organisationType) {
    final HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organisationType));
    final HttpEntity<String> entity = new HttpEntity<>(headers);

    final ResponseEntity<String> read = this.restTemplate.exchange(
        "/registry/v1/options/policies/" + id,
        HttpMethod.GET,
        entity,
        String.class
    );

    if (read.getStatusCode().isError()) {
      log.info(read.getBody());
    }

    assertThat(read.getStatusCode()).isEqualTo(httpStatus);
    return read.getBody();
  }

  private String getPolicyList(HttpStatus httpStatus, JwtTestUtils.OrganisationType organisationType) {
    final HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + new JwtTestUtils().createJwt(organisationType));
    final HttpEntity<String> entity = new HttpEntity<>(headers);

    final ResponseEntity<String> read = this.restTemplate.exchange(
        "/registry/v1/options/list/policies",
        HttpMethod.GET,
        entity,
        String.class
    );

    if (read.getStatusCode().isError()) {
      log.info(read.getBody());
    }

    assertThat(read.getStatusCode()).isEqualTo(httpStatus);
    return read.getBody();
  }

}
