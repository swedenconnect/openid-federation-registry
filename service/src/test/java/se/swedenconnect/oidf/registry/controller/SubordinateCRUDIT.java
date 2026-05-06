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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.SubordinatesApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Subordinate;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Subordinate CRUD operations, including the metadataPolicy field.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
class SubordinateCRUDIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private SubordinatesApi subordinatesApi;
  private EntitiesApi entitiesApi;
  private ModulesApi modulesApi;

  @BeforeEach
  void setUp() {
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost:" + this.port);
    apiClient.setBearerToken(this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM));
    apiClient.setApiKey(JwtTestUtils.OrganisationType.PM.orgId);

    this.subordinatesApi = new SubordinatesApi(apiClient);
    this.entitiesApi = new EntitiesApi(apiClient);
    this.modulesApi = new ModulesApi(apiClient);
  }

  /**
   * Creates a FederationEntity with a TrustAnchor and returns the TrustAnchor ID.
   */
  private UUID setupTrustAnchor(final String entityIdentifier) {
    final UUID entityId = UUID.randomUUID();
    this.entitiesApi.createFederationEntityWithId(entityId, new FederationEntity().entityIdentifier(entityIdentifier));

    final UUID trustAnchorId = UUID.randomUUID();
    this.modulesApi.createTrustAnchorWithId(trustAnchorId, new TrustAnchor().entityId(entityId).active(true));
    return trustAnchorId;
  }

  // ========== metadataPolicy CRUD ==========

  @Test
  @DisplayName("Create subordinate with metadataPolicy stores the JSON field")
  void createSubordinateWithSubordinatePolicyStoresJson() {
    final UUID trustAnchorId = this.setupTrustAnchor("https://www.pm.se/oidf/sub-policy-create");
    final Map<String, Object> policy = Map.of("metadata_policy", Map.of(
        "openid_provider", Map.of(
            "subject_types_supported", Map.of("value", List.of("pairwise")))));

    final Subordinate created = this.subordinatesApi.createSubordinate(new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/create")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(policy));

    assertThat(created).isNotNull();
    assertThat(created.getSubordinateId()).isNotNull();
    assertThat(created.getMetadataPolicy()).isEqualTo(policy);
  }

  @Test
  @DisplayName("Create subordinate without metadataPolicy stores null")
  void createSubordinateWithoutSubordinatePolicyStoresNull() {

    final UUID trustAnchorId = this.setupTrustAnchor("https://www.pm.se/oidf/sub-policy-null");

    final Subordinate created = this.subordinatesApi.createSubordinate(new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/no-policy")
        .jwks(TestDataOperations.genJWKS().toJSONObject()));

    assertThat(created.getMetadataPolicy()).isEqualTo(Collections.emptyMap());
  }

  @Test
  @DisplayName("Get subordinate returns stored metadataPolicy")
  void getSubordinateReturnsSubordinatePolicy() {
    final UUID trustAnchorId = this.setupTrustAnchor("https://www.pm.se/oidf/sub-policy-get");
    final Map<String, Object> policy = Map.of("metadata_policy", Map.of(
        "federation_entity", Map.of(
            "organization_name", Map.of("value", "Test Org"))));

    final UUID subordinateId = UUID.randomUUID();
    this.subordinatesApi.createSubordinateWithId(subordinateId, new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/get")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(policy));

    final Subordinate retrieved = this.subordinatesApi.getSubordinate(subordinateId);

    assertThat(retrieved.getMetadataPolicy()).isEqualTo(policy);
  }

  @Test
  @DisplayName("Update subordinate sets a new metadataPolicy")
  void updateSubordinateSetsNewSubordinatePolicy() {
    final UUID trustAnchorId = this.setupTrustAnchor("https://www.pm.se/oidf/sub-policy-update");
    final Map<String, Object> initialPolicy = Map.of("metadata_policy", Map.of(
        "openid_provider", Map.of(
            "subject_types_supported", Map.of("value", List.of("public")))));
    final Map<String, Object> updatedPolicy = Map.of("metadata_policy", Map.of(
        "openid_provider", Map.of(
            "subject_types_supported", Map.of("value", List.of("pairwise")))));

    final UUID subordinateId = UUID.randomUUID();
    this.subordinatesApi.createSubordinateWithId(subordinateId, new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/update")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(initialPolicy));

    final Subordinate updated = this.subordinatesApi.updateSubordinate(subordinateId, new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/update")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(updatedPolicy));

    assertThat(updated.getMetadataPolicy()).isEqualTo(updatedPolicy);
  }

  @Test
  @DisplayName("Update subordinate clears metadataPolicy when set to null")
  void updateSubordinateClearsSubordinatePolicy() {
    final UUID trustAnchorId = this.setupTrustAnchor("https://www.pm.se/oidf/sub-policy-clear");
    final Map<String, Object> policy = Map.of("metadata_policy", Map.of(
        "openid_provider", Map.of(
            "subject_types_supported", Map.of("value", List.of("public")))));

    final UUID subordinateId = UUID.randomUUID();
    this.subordinatesApi.createSubordinateWithId(subordinateId, new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/clear")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(policy));

    final Subordinate updated = this.subordinatesApi.updateSubordinate(subordinateId, new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/clear")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(null));

    assertThat(updated.getMetadataPolicy()).isNull();
  }

  @Test
  @DisplayName("Delete subordinate removes it and subsequent get returns 404")
  void deleteSubordinateRemovesIt() {
    final UUID trustAnchorId = this.setupTrustAnchor("https://www.pm.se/oidf/sub-policy-delete");
    final Map<String, Object> policy = Map.of("metadata_policy", Map.of(
        "openid_provider", Map.of(
            "subject_types_supported", Map.of("value", List.of("public")))));
    final UUID subordinateId = UUID.randomUUID();
    this.subordinatesApi.createSubordinateWithId(subordinateId, new Subordinate()
        .taImId(trustAnchorId)
        .entityIdentifier("https://sub.example.se/delete")
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .metadataPolicy(policy));

    assertThat(this.subordinatesApi.getSubordinate(subordinateId)).isNotNull();

    this.subordinatesApi.deleteSubordinate(subordinateId);

    assertThatThrownBy(() -> this.subordinatesApi.getSubordinate(subordinateId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value()).isEqualTo(404));
  }
}