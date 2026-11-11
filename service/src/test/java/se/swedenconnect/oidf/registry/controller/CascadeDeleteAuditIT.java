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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.SubordinatesApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.Intermediate;
import se.swedenconnect.oidf.registry.api.model.Resolver;
import se.swedenconnect.oidf.registry.api.model.Subordinate;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.api.model.TrustmarkSubject;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that every record deleted together with its parent record is audited, as when it is deleted on
 * its own.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class CascadeDeleteAuditIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType PM = JwtTestUtils.OrganisationType.PM;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private AuditEventRepository auditEventRepository;

  private EntitiesApi entitiesApi;
  private ModulesApi modulesApi;
  private SubordinatesApi subordinatesApi;
  private TrustmarksApi trustmarksApi;

  @BeforeEach
  void setUp() {
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost:" + this.port);
    apiClient.setBearerToken(this.jwtTestUtils.createJwt(PM));
    this.entitiesApi = new EntitiesApi(apiClient);
    this.modulesApi = new ModulesApi(apiClient);
    this.subordinatesApi = new SubordinatesApi(apiClient);
    this.trustmarksApi = new TrustmarksApi(apiClient);
  }

  @Test
  @DisplayName("Deleting a federation entity audits its trust anchor and the subordinates under it")
  void deletingFederationEntityAuditsTrustAnchorAndSubordinates() {
    final UUID entityId = this.createEntity("https://www.pm.se/oidf/ta/" + UUID.randomUUID());
    final UUID trustAnchorId = this.modulesApi.createTrustAnchorWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new TrustAnchor().entityId(entityId).active(true)).getTrustAnchorId();
    final UUID subordinateId = this.createSubordinate(trustAnchorId);

    this.entitiesApi.deleteFederationEntity(TENANT, PM.orgId, entityId);

    assertThat(this.audited("SUBORDINATE_DELETED", subordinateId)).isTrue();
    assertThat(this.audited("TRUST_ANCHOR_DELETED", trustAnchorId)).isTrue();
    assertThat(this.audited("FEDERATION_ENTITY_DELETED", entityId)).isTrue();
  }

  @Test
  @DisplayName("Deleting a federation entity audits its intermediate, resolver and trust mark issuer with its records")
  void deletingFederationEntityAuditsEveryModule() {
    final UUID entityId = this.createEntity("https://www.pm.se/oidf/im/" + UUID.randomUUID());
    final UUID intermediateId = this.modulesApi.createIntermediateWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new Intermediate().entityId(entityId).active(true)).getIntermediateId();
    final UUID subordinateId = this.createSubordinate(intermediateId);
    final UUID resolverId = this.modulesApi.createResolverWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new Resolver()
            .entityId(entityId)
            .active(true)
            .trustAnchor("https://www.pm.se/oidf/ta")
            .trustedKeys(TestDataOperations.genJWKS().toJSONObject())
            .stepCachedValueThreshold(10)
            .resolveResponseDuration("PT1H"))
        .getResolverId();
    final UUID issuerId = this.createTrustmarkIssuer(entityId);
    final UUID trustmarkId = this.createTrustmark(issuerId);
    final UUID subjectId = this.createSubject(trustmarkId);

    this.entitiesApi.deleteFederationEntity(TENANT, PM.orgId, entityId);

    assertThat(this.audited("SUBORDINATE_DELETED", subordinateId)).isTrue();
    assertThat(this.audited("INTERMEDIATE_DELETED", intermediateId)).isTrue();
    assertThat(this.audited("RESOLVER_DELETED", resolverId)).isTrue();
    assertThat(this.audited("TRUSTMARK_SUBJECT_DELETED", subjectId)).isTrue();
    assertThat(this.audited("TRUSTMARK_DELETED", trustmarkId)).isTrue();
    assertThat(this.audited("TRUSTMARK_ISSUER_DELETED", issuerId)).isTrue();
    assertThat(this.audited("FEDERATION_ENTITY_DELETED", entityId)).isTrue();
  }

  @Test
  @DisplayName("Deleting a trust mark issuer audits its trust marks and their subjects")
  void deletingTrustmarkIssuerAuditsTrustmarksAndSubjects() {
    final UUID issuerId =
        this.createTrustmarkIssuer(this.createEntity("https://www.pm.se/oidf/tmi/" + UUID.randomUUID()));
    final UUID trustmarkId = this.createTrustmark(issuerId);
    final UUID subjectId = this.createSubject(trustmarkId);

    this.modulesApi.deleteTrustmarkIssuer(TENANT, PM.orgId, issuerId);

    assertThat(this.audited("TRUSTMARK_SUBJECT_DELETED", subjectId)).isTrue();
    assertThat(this.audited("TRUSTMARK_DELETED", trustmarkId)).isTrue();
    assertThat(this.audited("TRUSTMARK_ISSUER_DELETED", issuerId)).isTrue();
  }

  @Test
  @DisplayName("Deleting a trust anchor or a trust mark on its own audits the records below it")
  void deletingModuleOrTrustmarkAuditsRecordsBelow() {
    final UUID entityId = this.createEntity("https://www.pm.se/oidf/ta/" + UUID.randomUUID());
    final UUID trustAnchorId = this.modulesApi.createTrustAnchorWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new TrustAnchor().entityId(entityId).active(true)).getTrustAnchorId();
    final UUID subordinateId = this.createSubordinate(trustAnchorId);
    final UUID trustmarkId = this.createTrustmark(
        this.createTrustmarkIssuer(this.createEntity("https://www.pm.se/oidf/tmi/" + UUID.randomUUID())));
    final UUID subjectId = this.createSubject(trustmarkId);

    this.modulesApi.deleteTrustAnchor(TENANT, PM.orgId, trustAnchorId);
    this.trustmarksApi.deleteTrustmark(TENANT, PM.orgId, trustmarkId);

    assertThat(this.audited("SUBORDINATE_DELETED", subordinateId)).isTrue();
    assertThat(this.audited("TRUSTMARK_SUBJECT_DELETED", subjectId)).isTrue();
  }

  private boolean audited(final String type, final UUID extId) {
    return this.auditEventRepository.find(null, null, type).stream()
        .anyMatch(event -> extId.toString().equals(event.getData().get("extId")));
  }

  private UUID createEntity(final String entityIdentifier) {
    final UUID entityId = UUID.randomUUID();
    this.entitiesApi.createFederationEntityWithId(TENANT, PM.orgId, entityId,
        new FederationEntity().entityIdentifier(entityIdentifier));
    return entityId;
  }

  private UUID createSubordinate(final UUID taImId) {
    final String entityIdentifier = "https://www.pm.se/oidf/leaf/" + UUID.randomUUID();
    return this.subordinatesApi.createSubordinate(TENANT, PM.orgId,
        new Subordinate()
            .taImId(taImId)
            .entityIdentifier(entityIdentifier)
            .entityTypes(List.of("openid_relying_party"))
            .jwks(TestDataOperations.genJWKS().toJSONObject())
            .ecLocationAutomaticResolve(false)
            .ecLocation(entityIdentifier + "/.well-known/openid-federation"))
        .getSubordinateId();
  }

  private UUID createTrustmarkIssuer(final UUID entityId) {
    return this.modulesApi.createTrustmarkIssuerWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new TrustmarkIssuer().entityId(entityId).active(true)).getTrustmarkIssuerId();
  }

  private UUID createTrustmark(final UUID issuerId) {
    final UUID trustmarkId = UUID.randomUUID();
    this.trustmarksApi.createTrustmarkWithId(TENANT, PM.orgId, trustmarkId,
        new Trustmark().trustmarkissuerId(issuerId).trustmarkType("https://www.pm.se/oidf/loa/" + UUID.randomUUID()));
    return trustmarkId;
  }

  private UUID createSubject(final UUID trustmarkId) {
    return this.trustmarksApi.createTrustmarkSubject(TENANT, PM.orgId,
        new TrustmarkSubject().trustmarkId(trustmarkId).subject("https://www.pm.se/oidf/leaf/" + UUID.randomUUID())
            .revoked(false)).getTrustmarksubjectId();
  }
}
