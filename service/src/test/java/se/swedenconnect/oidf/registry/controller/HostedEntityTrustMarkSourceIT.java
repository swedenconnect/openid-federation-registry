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

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.api.model.TrustmarkSubject;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkSourceProperty;
import se.swedenconnect.oidf.registry.federationservice.serde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.federationservice.state.StateChangeStore;
import se.swedenconnect.oidf.registry.federationservice.state.StateKey;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The trust marks the registry has granted a hosted entity are published as the trust mark sources of its entity
 * record, which is what gives them to its hosted Entity Configuration.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class HostedEntityTrustMarkSourceIT {

  private static final String TENANT = "Swedenconnect";
  private static final JwtTestUtils.OrganisationType OPERATOR = JwtTestUtils.OrganisationType.PM;
  private static final UUID INSTANCE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private StateChangeStore stateChangeStore;

  private final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader();

  private EntitiesApi entitiesApi;
  private ModulesApi modulesApi;
  private TrustmarksApi trustmarksApi;

  private String hostedEntityId;
  private UUID hostedEntityUuid;
  private String issuerEntityId;
  private String trustMarkType;
  private UUID trustmarkId;

  @BeforeEach
  void setUp() {
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost:" + this.port);
    apiClient.setBearerToken(this.jwtTestUtils.createJwt(OPERATOR));
    this.entitiesApi = new EntitiesApi(apiClient);
    this.modulesApi = new ModulesApi(apiClient);
    this.trustmarksApi = new TrustmarksApi(apiClient);

    final String unique = UUID.randomUUID().toString();
    this.hostedEntityId = "https://www.pm.se/oidf/hosted-rp/" + unique;
    this.issuerEntityId = "https://www.pm.se/oidf/tmi/" + unique;
    this.trustMarkType = "https://id.swedenconnect.se/tm-prop/public-body/" + unique;

    this.hostedEntityUuid = this.entitiesApi.createHostedEntity(TENANT, OPERATOR.orgId, new HostedEntity()
        .entityIdentifier(this.hostedEntityId)
        .metadata(Map.of("openid_relying_party", Map.of("client_name", "A Relying Party")))).getEntityId();

    final FederationEntity issuerEntity = this.entitiesApi.createFederationEntity(TENANT, OPERATOR.orgId,
        new FederationEntity().entityIdentifier(this.issuerEntityId));
    final TrustmarkIssuer issuer = this.modulesApi.createTrustmarkIssuer(TENANT, OPERATOR.orgId,
        new TrustmarkIssuer().entityId(issuerEntity.getEntityId()).active(true));
    this.trustmarkId = this.trustmarksApi.createTrustmark(TENANT, OPERATOR.orgId, new Trustmark()
        .trustmarkissuerId(issuer.getTrustmarkIssuerId())
        .trustmarkType(this.trustMarkType)).getTrustmarkId();
  }

  @Test
  @DisplayName("A granted trust mark is a trust mark source of the hosted entity, a revoked one is not")
  void grantedTrustMarkIsATrustMarkSourceOfTheHostedEntity() throws ParseException {
    assertThat(this.trustMarkSourcesOfHostedEntity())
        .as("The entity has been granted nothing yet")
        .isEmpty();

    final UUID subjectId = this.grant(false);

    assertThat(this.trustMarkSourcesOfHostedEntity())
        .extracting(source -> source.issuer().getValue(), TrustMarkSourceProperty::trustMarkType)
        .containsExactly(org.assertj.core.groups.Tuple.tuple(this.issuerEntityId, this.trustMarkType));

    this.trustmarksApi.updateTrustmarkSubject(TENANT, OPERATOR.orgId, subjectId, new TrustmarkSubject()
        .trustmarkId(this.trustmarkId)
        .subject(this.hostedEntityId)
        .revoked(true));

    assertThat(this.trustMarkSourcesOfHostedEntity())
        .as("A revoked trust mark is withdrawn from the entity configuration")
        .isEmpty();
  }

  @Test
  @DisplayName("Granting a trust mark lists the entity record of the subject as changed")
  void grantingATrustMarkListsTheEntityRecordAsChanged() {
    final long before = this.stateChangeStore.head(INSTANCE_ID);

    this.grant(false);

    assertThat(this.stateChangeStore.changes(INSTANCE_ID, before, 100))
        .as("The federation service is told to read the record of the entity that was granted the trust mark")
        .extracting(StateChangeStore.Change::key)
        .contains(StateKey.entity(this.hostedEntityUuid));
  }

  private UUID grant(final boolean revoked) {
    return this.trustmarksApi.createTrustmarkSubject(TENANT, OPERATOR.orgId, new TrustmarkSubject()
        .trustmarkId(this.trustmarkId)
        .subject(this.hostedEntityId)
        .revoked(revoked)).getTrustmarksubjectId();
  }

  /** The trust mark sources of the hosted entity, as the federation service API gives them. */
  private List<TrustMarkSourceProperty> trustMarkSourcesOfHostedEntity() throws ParseException {
    final String entityRecords = SignedJWT.parse(this.federationService()
            .get().uri("/entity_record?instanceid={id}", INSTANCE_ID).retrieve().body(String.class))
        .getJWTClaimsSet().getClaimAsString("entity_records");
    return this.jsonRegistryLoader.parseEntityRecord(entityRecords).stream()
        .filter(record -> this.hostedEntityId.equals(record.getEntityIdentifier().getValue()))
        .findFirst()
        .map(EntityRecord::getTrustMarkSource)
        .map(List::copyOf)
        .orElse(List.of());
  }

  private RestClient federationService() {
    return RestClient.create("http://localhost:%d/api/v1/federationservice".formatted(this.port));
  }

}
