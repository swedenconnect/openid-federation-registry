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
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClientResponseException;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.SubordinatesApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Subordinate;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.api.model.TrustmarkSubject;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An entity identifier must not end with a slash, see issue #100. A trailing slash is a different entity identifier
 * of the same entity, and the endpoints and {@code ec_location} the registry builds from it get a double slash. The
 * rule is checked where an entity identifier is accepted: on a federation entity, a hosted entity, a subordinate
 * statement and a trust mark subject.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class EntityIdTrailingSlashIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType ORGANIZATION = JwtTestUtils.OrganisationType.PM;
  /** The organization's entity prefix in the test configuration, which a federation entity has to start with. */
  private static final String PREFIX = "https://www.pm.se/oidf";

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;

  private EntitiesApi entities;
  private ModulesApi modules;
  private SubordinatesApi subordinates;
  private TrustmarksApi trustmarks;

  @BeforeEach
  void setUp() {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(ORGANIZATION));
    this.entities = new EntitiesApi(client);
    this.modules = new ModulesApi(client);
    this.subordinates = new SubordinatesApi(client);
    this.trustmarks = new TrustmarksApi(client);
  }

  @Test
  @DisplayName("A federation entity identifier ending with a slash is refused, without it accepted")
  void federationEntity() {
    final String identifier = PREFIX + "/ta/" + UUID.randomUUID();

    this.assertRefused("entityIdentifier", () -> this.entities.createFederationEntity(TENANT, ORGANIZATION.orgId,
        FederationEntity.builder().entityIdentifier(identifier + "/").build()));

    assertThat(this.entities.createFederationEntity(TENANT, ORGANIZATION.orgId,
        FederationEntity.builder().entityIdentifier(identifier).build()).getEntityIdentifier())
        .isEqualTo(identifier);
  }

  @Test
  @DisplayName("A hosted entity identifier ending with a slash is refused, without it accepted")
  void hostedEntity() {
    final String identifier = PREFIX + "/rp/" + UUID.randomUUID();

    this.assertRefused("entityIdentifier", () -> this.entities.createHostedEntity(TENANT, ORGANIZATION.orgId,
        this.hostedEntity(identifier + "/")));

    assertThat(this.entities.createHostedEntity(TENANT, ORGANIZATION.orgId, this.hostedEntity(identifier))
        .getEntityIdentifier()).isEqualTo(identifier);
  }

  @Test
  @DisplayName("A subordinate entity identifier ending with a slash is refused, without it accepted")
  void subordinate() {
    final UUID taImId = this.trustAnchor();
    final String identifier = PREFIX + "/subordinate/" + UUID.randomUUID();

    this.assertRefused("entityIdentifier", () -> this.subordinates.createSubordinate(TENANT, ORGANIZATION.orgId,
        this.subordinate(taImId, identifier + "/")));

    assertThat(this.subordinates.createSubordinate(TENANT, ORGANIZATION.orgId, this.subordinate(taImId, identifier))
        .getEntityIdentifier()).isEqualTo(identifier);
  }

  @Test
  @DisplayName("A trust mark subject ending with a slash is refused, without it accepted")
  void trustmarkSubject() {
    final UUID trustmarkId = this.trustmark();
    final String subject = PREFIX + "/subject/" + UUID.randomUUID();

    this.assertRefused("subject", () -> this.trustmarks.createTrustmarkSubject(TENANT, ORGANIZATION.orgId,
        new TrustmarkSubject().trustmarkId(trustmarkId).subject(subject + "/").revoked(false)));

    assertThat(this.trustmarks.createTrustmarkSubject(TENANT, ORGANIZATION.orgId,
        new TrustmarkSubject().trustmarkId(trustmarkId).subject(subject).revoked(false)).getSubject())
        .isEqualTo(subject);
  }

  /** Asserts that the call is refused with 400 and that the answer names the field and the rule. */
  private void assertRefused(final String field, final Runnable call) {
    assertThatThrownBy(call::run)
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(thrown -> {
          final RestClientResponseException response = (RestClientResponseException) thrown;
          assertThat(response.getStatusCode().value()).isEqualTo(400);
          assertThat(response.getResponseBodyAsString())
              .contains(field)
              .contains("EntityId must not end with /");
        });
  }

  private HostedEntity hostedEntity(final String identifier) {
    return HostedEntity.builder()
        .entityIdentifier(identifier)
        .metadata(Map.of("openid_relying_party", Map.of("client_name", "Trailing slash test")))
        .build();
  }

  private Subordinate subordinate(final UUID taImId, final String identifier) {
    return Subordinate.builder()
        .taImId(taImId)
        .entityIdentifier(identifier)
        .entityTypes(List.of("openid_relying_party"))
        .jwks(TestDataOperations.genJWKS().toJSONObject())
        .build();
  }

  /** A Trust Anchor of the organization, to hang the subordinates on. */
  private UUID trustAnchor() {
    final FederationEntity entity = this.entities.createFederationEntity(TENANT, ORGANIZATION.orgId,
        FederationEntity.builder().entityIdentifier(PREFIX + "/ta-for-subordinate/" + UUID.randomUUID()).build());
    return this.modules.createTrustAnchor(TENANT, ORGANIZATION.orgId,
        TrustAnchor.builder().entityId(entity.getEntityId()).active(true).build()).getTrustAnchorId();
  }

  /** A trust mark of the organization's own trust mark issuer, to hang the subjects on. */
  private UUID trustmark() {
    final FederationEntity entity = this.entities.createFederationEntity(TENANT, ORGANIZATION.orgId,
        FederationEntity.builder().entityIdentifier(PREFIX + "/tmi/" + UUID.randomUUID()).build());
    final UUID issuerId = this.modules.createTrustmarkIssuer(TENANT, ORGANIZATION.orgId,
        TrustmarkIssuer.builder().entityId(entity.getEntityId()).active(true).build()).getTrustmarkIssuerId();
    return this.trustmarks.createTrustmark(TENANT, ORGANIZATION.orgId, Trustmark.builder()
        .trustmarkissuerId(issuerId)
        .trustmarkType(PREFIX + "/loa/" + UUID.randomUUID())
        .build()).getTrustmarkId();
  }
}
