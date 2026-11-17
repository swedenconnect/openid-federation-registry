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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.SubordinatesApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Subordinate;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.serde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.fixture.FederationAPIOperations;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@code cross_organization_authority_hints} on the exported entity records. An entity of one organization is
 * registered as a subordinate under a Trust Anchor of another organization of the same instance, and the entity record
 * exported for the instance must then carry that Trust Anchor as an authority hint — but only on an instance that has
 * the flag turned on.
 * <p>
 * The whole {@code instances} list is declared inline because a collection binds from the highest-precedence property
 * source alone: adding a single indexed property on top of {@code application.yml} would leave the other instance
 * properties unbound. Instance {@code 123e4567-…} (Swedenconnect) has the flag on, {@code 223e4567-…} (ENA) has it
 * off, so both halves share one application context.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "openid.federation.registry.instances[0].instance_id=123e4567-e89b-12d3-a456-426614174000",
        "openid.federation.registry.instances[0].name=Swedenconnect",
        "openid.federation.registry.instances[0].base_url=https://registry.swedenconnect.se/oidf",
        "openid.federation.registry.instances[0].function_groups[0]=swedenconnect",
        "openid.federation.registry.instances[0].operator_organizations[0]=5520001263",
        "openid.federation.registry.instances[0].cross_organization_authority_hints=true",
        "openid.federation.registry.instances[1].instance_id=223e4567-e89b-12d3-a456-426614174001",
        "openid.federation.registry.instances[1].name=ENA",
        "openid.federation.registry.instances[1].base_url=https://registry.ena.se/oidf",
        "openid.federation.registry.instances[1].function_groups[0]=swedenconnect",
        "openid.federation.registry.instances[1].operator_organizations[0]=5520001263"
    })
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class CrossOrganizationAuthorityHintsIT {

  private static final UUID CROSS_ORGANIZATION_INSTANCE = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final UUID OWN_ORGANIZATION_INSTANCE = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
  private static final String CROSS_ORGANIZATION_TENANT = "Swedenconnect";
  private static final String OWN_ORGANIZATION_TENANT = "ENA";
  private static final String CROSS_ORGANIZATION_BASE_URL = "https://registry.swedenconnect.se/oidf";
  private static final String OWN_ORGANIZATION_BASE_URL = "https://registry.ena.se/oidf";

  /** The organization that owns the Trust Anchor the subordinates are registered under. */
  private static final JwtTestUtils.OrganisationType SUPERIOR = JwtTestUtils.OrganisationType.PM;
  /** The organization that owns the entity registered under the other organization's Trust Anchor. */
  private static final JwtTestUtils.OrganisationType SUBORDINATE = JwtTestUtils.OrganisationType.AF;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private FederationAPIOperations federationAPIOperations;

  private final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader();

  /** The entity identifiers of one tenant's fixture. */
  private record Scenario(String trustAnchor, String entityOfAnotherOrganization, String entityOfTheSameOrganization) {
  }

  @Test
  @DisplayName("With cross_organization_authority_hints on, a Trust Anchor in another organization of the instance "
      + "is an authority hint of the entity")
  void superiorInAnotherOrganizationIsAnAuthorityHint() throws Exception {
    final Scenario scenario = this.setUpScenario(CROSS_ORGANIZATION_TENANT, CROSS_ORGANIZATION_BASE_URL);

    final Map<String, EntityRecord> records = this.entityRecords(CROSS_ORGANIZATION_INSTANCE);

    assertThat(records.get(scenario.entityOfAnotherOrganization()).getAuthorityHints())
        .as("the Trust Anchor of the other organization must be an authority hint")
        .containsExactly(scenario.trustAnchor());
    assertThat(records.get(scenario.entityOfTheSameOrganization()).getAuthorityHints())
        .as("the entity of the Trust Anchor's own organization keeps its hint")
        .containsExactly(scenario.trustAnchor());
  }

  @Test
  @DisplayName("Without the flag, a Trust Anchor in another organization of the instance is not an authority hint")
  void superiorInAnotherOrganizationIsNoAuthorityHintWithoutTheFlag() throws Exception {
    final Scenario scenario = this.setUpScenario(OWN_ORGANIZATION_TENANT, OWN_ORGANIZATION_BASE_URL);

    final Map<String, EntityRecord> records = this.entityRecords(OWN_ORGANIZATION_INSTANCE);

    assertThat(records.get(scenario.entityOfAnotherOrganization()).getAuthorityHints())
        .as("the lookup stays within the entity's own organization")
        .isNull();
    assertThat(records.get(scenario.entityOfTheSameOrganization()).getAuthorityHints())
        .as("the entity of the Trust Anchor's own organization keeps its hint")
        .containsExactly(scenario.trustAnchor());
  }

  /**
   * Registers, on the given tenant, a Trust Anchor of the {@code SUPERIOR} organization with two subordinates: a
   * hosted entity of its own organization and one of the {@code SUBORDINATE} organization. Every entity identifier
   * must start with its organization's entity prefix, {@code <base url>/<org number>}.
   */
  private Scenario setUpScenario(final String tenant, final String baseUrl) {
    final String suffix = UUID.randomUUID().toString();
    final String superiorPrefix = baseUrl + "/" + SUPERIOR.orgId;
    final String subordinatePrefix = baseUrl + "/" + SUBORDINATE.orgId;
    final ApiClient superiorClient = this.apiClient(SUPERIOR);
    final EntitiesApi superiorEntities = new EntitiesApi(superiorClient);

    final FederationEntity trustAnchorEntity = superiorEntities.createFederationEntity(tenant, SUPERIOR.orgId,
        FederationEntity.builder().entityIdentifier(superiorPrefix + "/ta/" + suffix).build());
    final TrustAnchor trustAnchor = new ModulesApi(superiorClient).createTrustAnchor(tenant, SUPERIOR.orgId,
        TrustAnchor.builder().entityId(trustAnchorEntity.getEntityId()).active(true).build());

    final HostedEntity ownOrganizationEntity = superiorEntities.createHostedEntity(tenant, SUPERIOR.orgId,
        HostedEntity.builder()
            .entityIdentifier(superiorPrefix + "/rp/" + suffix)
            .metadata(Map.of("openid_relying_party", Map.of("client_name", "Superior RP")))
            .build());
    final HostedEntity otherOrganizationEntity = new EntitiesApi(this.apiClient(SUBORDINATE))
        .createHostedEntity(tenant, SUBORDINATE.orgId, HostedEntity.builder()
            .entityIdentifier(subordinatePrefix + "/rp/" + suffix)
            .metadata(Map.of("openid_relying_party", Map.of("client_name", "Subordinate RP")))
            .build());

    final SubordinatesApi subordinates = new SubordinatesApi(superiorClient);
    for (final String entityIdentifier :
        List.of(ownOrganizationEntity.getEntityIdentifier(), otherOrganizationEntity.getEntityIdentifier())) {
      subordinates.createSubordinate(tenant, SUPERIOR.orgId, Subordinate.builder()
          .taImId(trustAnchor.getTrustAnchorId())
          .entityIdentifier(entityIdentifier)
          .entityTypes(List.of("openid_relying_party"))
          .jwks(TestDataOperations.genJWKS().toJSONObject())
          .build());
    }

    return new Scenario(trustAnchorEntity.getEntityIdentifier(), otherOrganizationEntity.getEntityIdentifier(),
        ownOrganizationEntity.getEntityIdentifier());
  }

  /** The entity records exported for an instance, by entity identifier. */
  private Map<String, EntityRecord> entityRecords(final UUID instanceId) throws Exception {
    final SignedJWT signedJWT = this.federationAPIOperations.callEntity(instanceId);
    final String entityRecordJson = signedJWT.getJWTClaimsSet().getClaimAsString("entity_records");
    return this.jsonRegistryLoader.parseEntityRecord(entityRecordJson).stream()
        .collect(Collectors.toMap(record -> record.getEntityIdentifier().toString(), record -> record));
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType organization) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(organization));
    return client;
  }
}
