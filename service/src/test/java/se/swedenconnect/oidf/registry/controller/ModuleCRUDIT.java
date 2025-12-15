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

package se.swedenconnect.oidf.registry.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Resolver;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Module CRUD operations (TrustAnchor, Resolver, Trustmark) using the generated OpenAPI client.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ModuleCRUDIT {

  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private ModulesApi modulesApi;
  private EntitiesApi entitiesApi;
  private TrustmarksApi trustmarksApi;
  private ApiClient apiClient;

  @BeforeEach
  void setUp() {
    this.apiClient = new ApiClient();
    this.apiClient.setBasePath("http://localhost:" + this.port);

    // Configure authentication
    this.apiClient.setBearerToken(this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM));
    this.apiClient.setApiKey(JwtTestUtils.OrganisationType.PM.orgId);

    this.modulesApi = new ModulesApi(this.apiClient);
    this.entitiesApi = new EntitiesApi(this.apiClient);
    this.trustmarksApi = new TrustmarksApi(this.apiClient);
  }

  /**
   * Test data container for trustmark setup.
   */
  public static class TrustmarkTestData {
    private final UUID entityId;
    private final UUID trustmarkIssuerId;

    public TrustmarkTestData(final UUID entityId, final UUID trustmarkIssuerId) {
      this.entityId = entityId;
      this.trustmarkIssuerId = trustmarkIssuerId;
    }

    public UUID getEntityId() {
      return this.entityId;
    }

    public UUID getTrustmarkIssuerId() {
      return this.trustmarkIssuerId;
    }
  }

  /**
   * Helper method to create a complete trustmark test setup: 1. Creates a FederationEntity 2. Creates a TrustmarkIssuer
   * on that entity Optionally creates one or more Trustmarks on the issuer
   *
   * @param entitySubject the subject for the federation entity
   * @param entityIssuer the issuer for the federation entity
   * @param trustmarkIssuerId optional trustmark issuer ID (if null, auto-generated)
   * @param trustmarkIssuerActive whether the trustmark issuer should be active
   * @param trustmarkIssuerValidityDuration validity duration for trustmark issuer
   * @return test data container with entityId and trustmarkIssuerId
   */
  private TrustmarkTestData setupTrustmarkTestData(
      final String entitySubject,
      final String entityIssuer,
      final UUID trustmarkIssuerId,
      final Boolean trustmarkIssuerActive,
      final String trustmarkIssuerValidityDuration) {
    // Step 1: Create FederationEntity
    final UUID entityId = UUID.randomUUID();
    final FederationEntity entityInput = new FederationEntity()
        .subject(entitySubject)
        .issuer(entityIssuer);
    this.entitiesApi.createFederationEntityWithId(entityId, entityInput);

    // Step 2: Create TrustmarkIssuer on the FederationEntity
    final UUID issuerId = trustmarkIssuerId != null ? trustmarkIssuerId : UUID.randomUUID();
    final TrustmarkIssuer issuerInput = new TrustmarkIssuer()
        .entityId(entityId)
        .active(trustmarkIssuerActive)
        .trustMarkTokenValidityDuration(trustmarkIssuerValidityDuration);

    final TrustmarkIssuer createdIssuer = this.modulesApi.createTrustmarkIssuerWithId(issuerId, issuerInput);

    return new TrustmarkTestData(entityId, createdIssuer.getTrustmarkIssuerId());
  }

  /**
   * Convenience method to create trustmark test data with default values.
   *
   * @param entitySubject the subject for the federation entity
   * @param entityIssuer the issuer for the federation entity
   * @return test data container with entityId and trustmarkIssuerId
   */
  private TrustmarkTestData setupTrustmarkTestData(final String entitySubject, final String entityIssuer) {
    return this.setupTrustmarkTestData(entitySubject, entityIssuer, null, true, "PT1H");
  }

  /**
   * Helper method to create a Trustmark on an existing TrustmarkIssuer.
   *
   * @param trustmarkIssuerId the trustmark issuer ID
   * @param trustmarkId optional trustmark ID (if null, auto-generated)
   * @param trustMarkEntityId the trustmark entity ID
   * @param logoUri optional logo URI
   * @param refUri optional reference URI
   * @param delegation optional delegation JWT
   * @return the created trustmark
   */
  private Trustmark createTrustmark(
      final UUID trustmarkIssuerId,
      final UUID trustmarkId,
      final String trustMarkEntityId,
      final String logoUri,
      final String refUri,
      final String delegation) {
    final Trustmark input = new Trustmark()
        .trustmarkissuerId(trustmarkIssuerId)
        .trustMarkEntityId(trustMarkEntityId);
    if (logoUri != null) {
      input.setLogoUri(logoUri);
    }
    if (refUri != null) {
      input.setRefUri(refUri);
    }
    if (delegation != null) {
      input.setDelegation(delegation);
    }

    if (trustmarkId != null) {
      return this.trustmarksApi.createTrustmarkWithId(trustmarkId, input);
    }
    else {
      return this.trustmarksApi.createTrustmark(input);
    }
  }

  /**
   * Helper method to create a FederationEntity for testing.
   *
   * @param subject the entity subject
   * @param issuer the entity issuer
   * @return the created entity ID
   */
  private UUID createFederationEntity(final String subject, final String issuer) {
    final UUID entityId = UUID.randomUUID();
    final FederationEntity entityInput = new FederationEntity()
        .subject(subject)
        .issuer(issuer);
    this.entitiesApi.createFederationEntityWithId(entityId, entityInput);
    return entityId;
  }

  /**
   * Helper method to create a HostedEntity for testing.
   *
   * @param subject the entity subject
   * @param issuer the entity issuer
   * @return the created entity ID
   */
  private UUID createHostedEntity(final String subject, final String issuer) {
    final UUID entityId = UUID.randomUUID();
    final HostedEntity entityInput = new HostedEntity()
        .subject(subject)
        .issuer(issuer);
    this.entitiesApi.createHostedEntityWithId(entityId, entityInput);
    return entityId;
  }

  // ========== TrustAnchor Tests ==========

  @Test
  void testCreateTrustAnchorWithAutoGeneratedId() {
    // Arrange - Create a federation entity first (required for trust anchor)
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/ta-entity",
        "https://www.pm.se/oidf/ta-entity");

    final TrustAnchor input = new TrustAnchor()
        .entityId(entityId)
        .active(true)
        .trustMarkIssuers(List.of("https://www.pm.se/oidf/tmi1", "https://www.pm.se/oidf/tmi2"));

    // Act
    final TrustAnchor created = this.modulesApi.createTrustAnchor(input);

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getTrustAnchorId()).isNotNull();
    assertThat(created.getEntityId()).isEqualTo(entityId);
    assertThat(created.getActive()).isTrue();
    assertThat(created.getTrustMarkIssuers()).contains("https://www.pm.se/oidf/tmi1", "https://www.pm.se/oidf/tmi2");
  }

  @Test
  void testCreateTrustAnchorWithSpecifiedId() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/ta-entity2",
        "https://www.pm.se/oidf/ta-entity2");

    final UUID moduleId = UUID.randomUUID();
    final TrustAnchor input = new TrustAnchor()
        .entityId(entityId)
        .active(false);

    // Act
    final TrustAnchor created = this.modulesApi.createTrustAnchorWithId(moduleId, input);

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getTrustAnchorId()).isEqualTo(moduleId);
    assertThat(created.getActive()).isFalse();
  }

  @Test
  void testGetTrustAnchor() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/ta-entity3",
        "https://www.pm.se/oidf/ta-entity3");

    final UUID moduleId = UUID.randomUUID();
    final TrustAnchor input = new TrustAnchor()
        .entityId(entityId)
        .active(true);
    this.modulesApi.createTrustAnchorWithId(moduleId, input);

    // Act
    final TrustAnchor retrieved = this.modulesApi.getTrustAnchor(moduleId);

    // Assert
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getTrustAnchorId()).isEqualTo(moduleId);
    assertThat(retrieved.getEntityId()).isEqualTo(entityId);
  }

  @Test
  void testUpdateTrustAnchor() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/ta-entity4",
        "https://www.pm.se/oidf/ta-entity4");

    final UUID moduleId = UUID.randomUUID();
    final TrustAnchor createInput = new TrustAnchor()
        .entityId(entityId)
        .active(true);
    this.modulesApi.createTrustAnchorWithId(moduleId, createInput);

    final TrustAnchor updateInput = new TrustAnchor()
        .entityId(entityId)
        .active(false)
        .trustMarkIssuers(List.of("https://www.pm.se/oidf/tmi-updated"));

    // Act
    final TrustAnchor updated = this.modulesApi.updateTrustAnchor(moduleId, updateInput);

    // Assert
    assertThat(updated).isNotNull();
    assertThat(updated.getActive()).isFalse();
    assertThat(updated.getTrustMarkIssuers()).contains("https://www.pm.se/oidf/tmi-updated");
  }

  @Test
  void testDeleteTrustAnchor() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/ta-entity5",
        "https://www.pm.se/oidf/ta-entity5");

    final UUID moduleId = UUID.randomUUID();
    final TrustAnchor input = new TrustAnchor()
        .entityId(entityId)
        .active(true);
    this.modulesApi.createTrustAnchorWithId(moduleId, input);

    // Verify it exists
    final TrustAnchor beforeDelete = this.modulesApi.getTrustAnchor(moduleId);
    assertThat(beforeDelete).isNotNull();

    // Act
    this.modulesApi.deleteTrustAnchor(moduleId);

    // Assert
    assertThatThrownBy(() -> this.modulesApi.getTrustAnchor(moduleId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(exception -> {
          final RestClientResponseException apiException = (RestClientResponseException) exception;
          assertThat(apiException.getStatusCode().value()).isEqualTo(404);
        });
  }

  // ========== Resolver Tests ==========

  @Test
  void testCreateResolverWithAutoGeneratedId() {
    // Arrange - Create a federation entity first
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/resolver-entity",
        "https://www.pm.se/oidf/resolver-entity");

    final Resolver input = new Resolver()
        .entityId(entityId)
        .active(true)
        .trustAnchor("https://www.pm.se/oidf/ta")
        .trustedKeys(TestDataOperations.genJWKS().toString())
        .stepRetryDuration("PT30M")
        .resolveResponseDuration("PT1H");

    // Act
    final Resolver created = this.modulesApi.createResolver(input);

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getResolverId()).isNotNull();
    assertThat(created.getEntityId()).isEqualTo(entityId);
    assertThat(created.getActive()).isTrue();
    assertThat(created.getResolveResponseDuration()).isEqualTo("PT1H");
    assertThat(created.getTrustAnchor()).isEqualTo("https://www.pm.se/oidf/ta");
  }

  @Test
  void testCreateResolverWithSpecifiedId() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/resolver-entity2",
        "https://www.pm.se/oidf/resolver-entity2");

    final UUID resolverId = UUID.randomUUID();
    final Resolver input = new Resolver()
        .entityId(entityId)
        .active(false)
        .trustAnchor("https://www.pm.se/oidf/ta")
        .trustedKeys(TestDataOperations.genJWKS().toString())
        .stepRetryDuration("PT5M")
        .resolveResponseDuration("PT2H");

    // Act
    final Resolver created = this.modulesApi.createResolverWithId(resolverId, input);

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getResolverId()).isEqualTo(resolverId);
    assertThat(created.getActive()).isFalse();
  }

  @Test
  void testGetResolver() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/resolver-entity3",
        "https://www.pm.se/oidf/resolver-entity3");

    final UUID resolverId = UUID.randomUUID();
    final Resolver input = new Resolver()
        .entityId(entityId)
        .active(true)
        .trustAnchor("https://www.pm.se/oidf/ta")
        .trustedKeys(TestDataOperations.genJWKS().toString())
        .stepRetryDuration("PT5M")
        .resolveResponseDuration("PT1H");
    this.modulesApi.createResolverWithId(resolverId, input);

    // Act
    final Resolver retrieved = this.modulesApi.getResolver(resolverId);

    // Assert
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getResolverId()).isEqualTo(resolverId);
    assertThat(retrieved.getEntityId()).isEqualTo(entityId);
  }

  @Test
  void testUpdateResolver() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/resolver-entity4",
        "https://www.pm.se/oidf/resolver-entity4");

    final UUID resolverId = UUID.randomUUID();
    final Resolver createInput = new Resolver()
        .entityId(entityId)
        .active(true)
        .trustAnchor("https://www.pm.se/oidf/ta")
        .trustedKeys(TestDataOperations.genJWKS().toString())
        .stepRetryDuration("PT5M")
        .resolveResponseDuration("PT1H");
    this.modulesApi.createResolverWithId(resolverId, createInput);

    final Resolver updateInput = new Resolver()
        .entityId(entityId)
        .active(false)
        .trustAnchor("https://www.pm.se/oidf/ta-updated")
        .trustedKeys(TestDataOperations.genJWKS().toString())
        .stepRetryDuration("PT5M")
        .resolveResponseDuration("PT3H");

    // Act
    final Resolver updated = this.modulesApi.updateResolver(resolverId, updateInput);

    // Assert
    assertThat(updated).isNotNull();
    assertThat(updated.getActive()).isFalse();
    assertThat(updated.getResolveResponseDuration()).isEqualTo("PT3H");
    assertThat(updated.getTrustAnchor()).isEqualTo("https://www.pm.se/oidf/ta-updated");
  }

  @Test
  void testDeleteResolver() {
    // Arrange
    final UUID entityId = this.createFederationEntity(
        "https://www.pm.se/oidf/resolver-entity5",
        "https://www.pm.se/oidf/resolver-entity5");

    final UUID resolverId = UUID.randomUUID();
    final Resolver input = new Resolver()
        .entityId(entityId)
        .active(true)
        .trustAnchor("https://www.pm.se/oidf/ta")
        .trustedKeys(TestDataOperations.genJWKS().toString())
        .stepRetryDuration("PT5M")
        .resolveResponseDuration("PT1H");
    this.modulesApi.createResolverWithId(resolverId, input);

    // Verify it exists
    final Resolver beforeDelete = this.modulesApi.getResolver(resolverId);
    assertThat(beforeDelete).isNotNull();

    // Act
    this.modulesApi.deleteResolver(resolverId);

    // Assert
    assertThatThrownBy(() -> this.modulesApi.getResolver(resolverId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(exception -> {
          final RestClientResponseException apiException = (RestClientResponseException) exception;
          assertThat(apiException.getStatusCode().value()).isEqualTo(404);
        });
  }

  // ========== Trustmark Tests ==========

  @Test
  void testCreateTrustmarkWithAutoGeneratedId() {
    // Arrange - Setup: FederationEntity -> TrustmarkIssuer
    final TrustmarkTestData testData = this.setupTrustmarkTestData(
        "https://www.pm.se/oidf/tmi-entity",
        "https://www.pm.se/oidf/tmi-entity");

    // Act - Create Trustmark on the TrustmarkIssuer
    final Trustmark created = this.createTrustmark(
        testData.getTrustmarkIssuerId(),
        null, // auto-generated ID
        "https://www.pm.se/oidf/trustmark",
        "https://www.pm.se/logo.png",
        "https://www.pm.se/ref",
        // This is not a propper delegation JWT.
        this.jwtTestUtils.createJwt(JwtTestUtils.OrganisationType.PM));

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getTrustmarkId()).isNotNull();
    assertThat(created.getTrustmarkissuerId()).isEqualTo(testData.getTrustmarkIssuerId());
    assertThat(created.getTrustMarkEntityId()).isEqualTo("https://www.pm.se/oidf/trustmark");
    assertThat(created.getLogoUri()).isEqualTo("https://www.pm.se/logo.png");
  }

  @Test
  void testCreateTrustmarkWithSpecifiedId() {
    // Arrange - Setup: FederationEntity -> TrustmarkIssuer
    final TrustmarkTestData testData = this.setupTrustmarkTestData(
        "https://www.pm.se/oidf/tmi-entity2",
        "https://www.pm.se/oidf/tmi-entity2");

    final UUID trustmarkId = UUID.randomUUID();

    // Act - Create Trustmark with specified ID
    final Trustmark created = this.createTrustmark(
        testData.getTrustmarkIssuerId(),
        trustmarkId,
        "https://www.pm.se/oidf/trustmark2",
        null,
        null,
        null);

    // Assert
    assertThat(created).isNotNull();
    assertThat(created.getTrustmarkId()).isEqualTo(trustmarkId);
    assertThat(created.getTrustMarkEntityId()).isEqualTo("https://www.pm.se/oidf/trustmark2");
  }

  @Test
  void testGetTrustmark() {
    // Arrange - Setup: FederationEntity -> TrustmarkIssuer -> Trustmark
    final TrustmarkTestData testData = this.setupTrustmarkTestData(
        "https://www.pm.se/oidf/tmi-entity3",
        "https://www.pm.se/oidf/tmi-entity3");

    final UUID trustmarkId = UUID.randomUUID();
    this.createTrustmark(
        testData.getTrustmarkIssuerId(),
        trustmarkId,
        "https://www.pm.se/oidf/trustmark3",
        null,
        null,
        null);

    // Act
    final Trustmark retrieved = this.trustmarksApi.getTrustmark(trustmarkId);

    // Assert
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getTrustmarkId()).isEqualTo(trustmarkId);
    assertThat(retrieved.getTrustMarkEntityId()).isEqualTo("https://www.pm.se/oidf/trustmark3");
  }

  @Test
  void testUpdateTrustmark() {
    // Arrange - Setup: FederationEntity -> TrustmarkIssuer -> Trustmark
    final TrustmarkTestData testData = this.setupTrustmarkTestData(
        "https://www.pm.se/oidf/tmi-entity4",
        "https://www.pm.se/oidf/tmi-entity4");

    final UUID trustmarkId = UUID.randomUUID();
    this.createTrustmark(
        testData.getTrustmarkIssuerId(),
        trustmarkId,
        "https://www.pm.se/oidf/trustmark4",
        null,
        null,
        null);

    final Trustmark updateInput = new Trustmark()
        .trustmarkissuerId(testData.getTrustmarkIssuerId())
        .trustMarkEntityId("https://www.pm.se/oidf/trustmark4-updated")
        .logoUri("https://www.pm.se/logo-updated.png")
        .refUri("https://www.pm.se/ref-updated");

    // Act
    final Trustmark updated = this.trustmarksApi.updateTrustmark(trustmarkId, updateInput);

    // Assert
    assertThat(updated).isNotNull();
    assertThat(updated.getTrustMarkEntityId()).isEqualTo("https://www.pm.se/oidf/trustmark4-updated");
    assertThat(updated.getLogoUri()).isEqualTo("https://www.pm.se/logo-updated.png");
  }

  @Test
  void testDeleteTrustmark() {
    // Arrange - Setup: FederationEntity -> TrustmarkIssuer -> Trustmark
    final TrustmarkTestData testData = this.setupTrustmarkTestData(
        "https://www.pm.se/oidf/tmi-entity5",
        "https://www.pm.se/oidf/tmi-entity5");

    final UUID trustmarkId = UUID.randomUUID();
    this.createTrustmark(
        testData.getTrustmarkIssuerId(),
        trustmarkId,
        "https://www.pm.se/oidf/trustmark5",
        null,
        null,
        null);

    // Verify it exists
    final Trustmark beforeDelete = this.trustmarksApi.getTrustmark(trustmarkId);
    assertThat(beforeDelete).isNotNull();

    // Act
    this.trustmarksApi.deleteTrustmark(trustmarkId);

    // Assert
    assertThatThrownBy(() -> this.trustmarksApi.getTrustmark(trustmarkId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(exception -> {
          final RestClientResponseException apiException = (RestClientResponseException) exception;
          assertThat(apiException.getStatusCode().value()).isEqualTo(404);
        });
  }
}

