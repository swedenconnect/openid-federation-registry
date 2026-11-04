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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClientResponseException;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.SubordinatesApi;
import se.swedenconnect.oidf.registry.api.TrustmarksApi;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Intermediate;
import se.swedenconnect.oidf.registry.api.model.Resolver;
import se.swedenconnect.oidf.registry.api.model.Subordinate;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.TrustAnchorIssuer;
import se.swedenconnect.oidf.registry.api.model.Trustmark;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;
import se.swedenconnect.oidf.registry.api.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.api.model.TrustmarkSubject;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.serde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.fixture.FederationAPIOperations;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for renaming an entity, which updates the references of the organization to it, and for the
 * authority hints that are exported for an entity.
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@AutoConfigureRestTestClient
class EntityRenameIT {

  private static final String TENANT = "Swedenconnect";
  private static final UUID INSTANCE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final JwtTestUtils.OrganisationType PM = JwtTestUtils.OrganisationType.PM;
  private static final JwtTestUtils.OrganisationType SKATT = JwtTestUtils.OrganisationType.SKATT;

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTestUtils jwtTestUtils;
  @Autowired
  private RestTestClient restTestClient;
  @Autowired
  private EntityRepository entityRepository;

  private final JsonRegistryLoader jsonRegistryLoader = new JsonRegistryLoader();

  private EntitiesApi entitiesApi;
  private ModulesApi modulesApi;
  private SubordinatesApi subordinatesApi;
  private TrustmarksApi trustmarksApi;

  @BeforeEach
  void setUp() {
    final ApiClient apiClient = this.apiClient(PM);
    this.entitiesApi = new EntitiesApi(apiClient);
    this.modulesApi = new ModulesApi(apiClient);
    this.subordinatesApi = new SubordinatesApi(apiClient);
    this.trustmarksApi = new TrustmarksApi(apiClient);
  }

  @Test
  @DisplayName("Renaming an intermediate renames its subordinate under the trust anchor and keeps its authority hint")
  void renamingIntermediateUpdatesSubordinateAndAuthorityHint() throws Exception {
    final String taIdentifier = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    final UUID trustAnchorId = this.createTrustAnchor(PM, taIdentifier);
    final String imIdentifier = "https://www.pm.se/oidf/im/" + UUID.randomUUID();
    final UUID imEntityId = this.createIntermediate(imIdentifier);
    final UUID subordinateId = this.createSubordinate(PM, trustAnchorId, imIdentifier);

    final String renamed = "https://www.pm.se/oidf/im/" + UUID.randomUUID();
    this.entitiesApi.updateFederationEntity(TENANT, PM.orgId, imEntityId,
        new FederationEntity().entityIdentifier(renamed));

    assertThat(this.subordinatesApi.getSubordinate(TENANT, PM.orgId, subordinateId).getEntityIdentifier())
        .isEqualTo(renamed);
    assertThat(this.exportedRecord(renamed).getAuthorityHints()).containsExactly(taIdentifier);
  }

  @Test
  @DisplayName("Renaming an entity leaves subordinates of another organization alone")
  void renamingLeavesSubordinatesOfOtherOrganization() {
    final String imIdentifier = "https://www.pm.se/oidf/im/" + UUID.randomUUID();
    final UUID imEntityId = this.createIntermediate(imIdentifier);
    final UUID otherTrustAnchorId = this.createTrustAnchor(SKATT,
        "https://registry.swedenconnect.se/oidf/" + SKATT.orgId + "/ta/" + UUID.randomUUID());
    final UUID otherSubordinateId = this.createSubordinate(SKATT, otherTrustAnchorId, imIdentifier);

    this.entitiesApi.updateFederationEntity(TENANT, PM.orgId, imEntityId,
        new FederationEntity().entityIdentifier("https://www.pm.se/oidf/im/" + UUID.randomUUID()));

    assertThat(new SubordinatesApi(this.apiClient(SKATT)).getSubordinate(TENANT, SKATT.orgId, otherSubordinateId)
        .getEntityIdentifier()).isEqualTo(imIdentifier);
  }

  @Test
  @DisplayName("A rename to an entity identifier that is already a subordinate of the same trust anchor is rejected")
  void renamingToExistingSubordinateIsRejected() {
    final UUID trustAnchorId = this.createTrustAnchor(PM, "https://www.pm.se/oidf/ta/" + UUID.randomUUID());
    final String imIdentifier = "https://www.pm.se/oidf/im/" + UUID.randomUUID();
    final UUID imEntityId = this.createIntermediate(imIdentifier);
    this.createSubordinate(PM, trustAnchorId, imIdentifier);
    final String taken = "https://www.pm.se/oidf/im/" + UUID.randomUUID();
    this.createSubordinate(PM, trustAnchorId, taken);

    assertThatThrownBy(() -> this.entitiesApi.updateFederationEntity(TENANT, PM.orgId, imEntityId,
        new FederationEntity().entityIdentifier(taken)))
        .isInstanceOfSatisfying(RestClientResponseException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    assertThat(this.entitiesApi.getFederationEntity(TENANT, PM.orgId, imEntityId, false).getEntityIdentifier())
        .isEqualTo(imIdentifier);
  }

  @Test
  @DisplayName("Renaming a hosted entity renames its trust mark subjects in the organization only")
  void renamingHostedEntityUpdatesTrustmarkSubjectsInOrganization() {
    final String hostedIdentifier = "https://www.pm.se/oidf/hosted/" + UUID.randomUUID();
    final UUID hostedId = this.createHostedEntity(hostedIdentifier, null);
    final UUID ownSubjectId = this.createSubject(PM, this.createTrustmark(PM,
        "https://www.pm.se/oidf/tmi/" + UUID.randomUUID()).trustmarkId(), hostedIdentifier);
    final UUID otherSubjectId = this.createSubject(SKATT, this.createTrustmark(SKATT,
            "https://registry.swedenconnect.se/oidf/" + SKATT.orgId + "/tmi/" + UUID.randomUUID()).trustmarkId(),
        hostedIdentifier);

    final String renamed = "https://www.pm.se/oidf/hosted/" + UUID.randomUUID();
    this.entitiesApi.updateHostedEntity(TENANT, PM.orgId, hostedId, this.hostedEntity(renamed));

    assertThat(this.trustmarksApi.getTrustmarkSubject(TENANT, PM.orgId, ownSubjectId).getSubject())
        .isEqualTo(renamed);
    assertThat(new TrustmarksApi(this.apiClient(SKATT)).getTrustmarkSubject(TENANT, SKATT.orgId, otherSubjectId)
        .getSubject()).isEqualTo(hostedIdentifier);
  }

  @Test
  @DisplayName("Renaming a trust mark issuer updates trust mark sources and trust mark issuers of the organization")
  void renamingTrustmarkIssuerUpdatesSourcesAndTrustAnchor() {
    final String tmiIdentifier = "https://www.pm.se/oidf/tmi/" + UUID.randomUUID();
    final IssuedTrustmark issued = this.createTrustmark(PM, tmiIdentifier);
    final UUID hostedId = this.createHostedEntity("https://www.pm.se/oidf/hosted/" + UUID.randomUUID(),
        new TrustmarkSource().trustMarkIssuer(tmiIdentifier).trustmarkId(issued.trustmarkType()));
    final UUID taEntityId = UUID.randomUUID();
    this.entitiesApi.createFederationEntityWithId(TENANT, PM.orgId, taEntityId,
        new FederationEntity().entityIdentifier("https://www.pm.se/oidf/ta/" + UUID.randomUUID()));
    final UUID trustAnchorId = this.modulesApi.createTrustAnchorWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new TrustAnchor().entityId(taEntityId).active(true)
            .addTrustMarkIssuersItem(new TrustAnchorIssuer().issuer(tmiIdentifier).auto(true)))
        .getTrustAnchorId();

    final String renamed = "https://www.pm.se/oidf/tmi/" + UUID.randomUUID();
    this.entitiesApi.updateFederationEntity(TENANT, PM.orgId, issued.entityId(),
        new FederationEntity().entityIdentifier(renamed));

    assertThat(this.entitiesApi.getHostedEntity(TENANT, PM.orgId, hostedId).getTrustMarkSources())
        .singleElement()
        .satisfies(source -> {
          assertThat(source.getTrustMarkIssuer()).isEqualTo(renamed);
          assertThat(source.getTrustmarkId()).isEqualTo(issued.trustmarkType());
        });
    assertThat(this.modulesApi.getTrustAnchor(TENANT, PM.orgId, trustAnchorId).getTrustMarkIssuers())
        .singleElement()
        .satisfies(issuer -> {
          assertThat(issuer.getIssuer()).isEqualTo(renamed);
          assertThat(issuer.getAuto()).isTrue();
        });
  }

  @Test
  @DisplayName("Stored authority hints of a hosted entity are saved and exported together with the calculated ones")
  void storedAuthorityHintsAreExportedWithCalculated() throws Exception {
    final String taIdentifier = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    final UUID trustAnchorId = this.createTrustAnchor(PM, taIdentifier);
    final String hostedIdentifier = "https://www.pm.se/oidf/hosted/" + UUID.randomUUID();
    final String external = "https://external.example.se/oidf/ta";
    final UUID hostedId = UUID.randomUUID();
    this.entitiesApi.createHostedEntityWithId(TENANT, PM.orgId, hostedId,
        this.hostedEntity(hostedIdentifier).authorityhints(List.of(taIdentifier, external)));
    this.createSubordinate(PM, trustAnchorId, hostedIdentifier);

    assertThat(this.entitiesApi.getHostedEntity(TENANT, PM.orgId, hostedId).getAuthorityhints())
        .containsExactly(taIdentifier, external);
    assertThat(this.exportedRecord(hostedIdentifier).getAuthorityHints())
        .containsExactly(taIdentifier, external);
  }

  @Test
  @DisplayName("Stored authority hints of a federation entity are exported")
  void storedAuthorityHintsOfFederationEntityAreExported() throws Exception {
    final String imIdentifier = "https://www.pm.se/oidf/im/" + UUID.randomUUID();
    final UUID imEntityId = this.createIntermediate(imIdentifier);
    final String external = "https://external.example.se/oidf/ta";

    this.entitiesApi.updateFederationEntity(TENANT, PM.orgId, imEntityId,
        new FederationEntity().entityIdentifier(imIdentifier).authorityhints(List.of(external)));

    assertThat(this.exportedRecord(imIdentifier).getAuthorityHints()).containsExactly(external);
  }

  @Test
  @DisplayName("Renaming a trust anchor renames the trust anchor of the resolvers of the organization only")
  void renamingTrustAnchorUpdatesResolversInOrganization() {
    final String taIdentifier = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    final UUID taEntityId = UUID.randomUUID();
    this.entitiesApi.createFederationEntityWithId(TENANT, PM.orgId, taEntityId,
        new FederationEntity().entityIdentifier(taIdentifier));
    final UUID ownResolverId = this.createResolver(PM, "https://www.pm.se/oidf/resolver/" + UUID.randomUUID(),
        taIdentifier);
    final UUID otherResolverId = this.createResolver(SKATT,
        "https://registry.swedenconnect.se/oidf/" + SKATT.orgId + "/resolver/" + UUID.randomUUID(), taIdentifier);

    final String renamed = "https://www.pm.se/oidf/ta/" + UUID.randomUUID();
    this.entitiesApi.updateFederationEntity(TENANT, PM.orgId, taEntityId,
        new FederationEntity().entityIdentifier(renamed));

    assertThat(this.modulesApi.getResolver(TENANT, PM.orgId, ownResolverId).getTrustAnchor()).isEqualTo(renamed);
    assertThat(new ModulesApi(this.apiClient(SKATT)).getResolver(TENANT, SKATT.orgId, otherResolverId)
        .getTrustAnchor()).isEqualTo(taIdentifier);
  }

  @Test
  @DisplayName("An empty authority hint is refused")
  void emptyAuthorityHintIsRefused() {
    for (final String empty : Arrays.asList(null, "", " ")) {
      final List<String> hints = Arrays.asList("https://external.example.se/oidf/ta", empty);
      assertThatThrownBy(() -> this.entitiesApi.createHostedEntityWithId(TENANT, PM.orgId, UUID.randomUUID(),
          this.hostedEntity("https://www.pm.se/oidf/hosted/" + UUID.randomUUID()).authorityhints(hints)))
          .isInstanceOfSatisfying(RestClientResponseException.class,
              e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
      assertThatThrownBy(() -> this.entitiesApi.createFederationEntityWithId(TENANT, PM.orgId, UUID.randomUUID(),
          new FederationEntity().entityIdentifier("https://www.pm.se/oidf/im/" + UUID.randomUUID())
              .authorityhints(hints)))
          .isInstanceOfSatisfying(RestClientResponseException.class,
              e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }
  }

  @Test
  @DisplayName("An empty authority hint already stored is left out of the export")
  void storedEmptyAuthorityHintIsLeftOutOfExport() throws Exception {
    final String hostedIdentifier = "https://www.pm.se/oidf/hosted/" + UUID.randomUUID();
    final UUID hostedId = this.createHostedEntity(hostedIdentifier, null);
    final String external = "https://external.example.se/oidf/ta";
    final se.swedenconnect.oidf.registry.entity.model.FederationEntity stored =
        this.entityRepository.findById(hostedId).orElseThrow();
    stored.setAuthorityhints(Arrays.asList(external, null, ""));
    this.entityRepository.save(stored);

    assertThat(this.exportedRecord(hostedIdentifier).getAuthorityHints()).containsExactly(external);
  }

  private EntityRecord exportedRecord(final String entityIdentifier) throws Exception {
    final String json = new FederationAPIOperations(this.restTestClient).callEntity(INSTANCE_ID)
        .getJWTClaimsSet().getClaimAsString("entity_records");
    return this.jsonRegistryLoader.parseEntityRecord(json).stream()
        .filter(record -> new EntityID(entityIdentifier).equals(record.getEntityIdentifier()))
        .findFirst()
        .orElseThrow();
  }

  private UUID createTrustAnchor(final JwtTestUtils.OrganisationType organisation, final String entityIdentifier) {
    final ApiClient client = this.apiClient(organisation);
    final UUID entityId = UUID.randomUUID();
    new EntitiesApi(client).createFederationEntityWithId(TENANT, organisation.orgId, entityId,
        new FederationEntity().entityIdentifier(entityIdentifier));
    return new ModulesApi(client).createTrustAnchorWithId(TENANT, organisation.orgId, UUID.randomUUID(),
        new TrustAnchor().entityId(entityId).active(true)).getTrustAnchorId();
  }

  private UUID createResolver(final JwtTestUtils.OrganisationType organisation, final String entityIdentifier,
      final String trustAnchor) {
    final ApiClient client = this.apiClient(organisation);
    final UUID entityId = UUID.randomUUID();
    new EntitiesApi(client).createFederationEntityWithId(TENANT, organisation.orgId, entityId,
        new FederationEntity().entityIdentifier(entityIdentifier));
    return new ModulesApi(client).createResolverWithId(TENANT, organisation.orgId, UUID.randomUUID(),
        new Resolver()
            .entityId(entityId)
            .active(true)
            .trustAnchor(trustAnchor)
            .trustedKeys(TestDataOperations.genJWKS().toJSONObject())
            .stepCachedValueThreshold(10)
            .resolveResponseDuration("PT1H"))
        .getResolverId();
  }

  private UUID createIntermediate(final String entityIdentifier) {
    final UUID entityId = UUID.randomUUID();
    this.entitiesApi.createFederationEntityWithId(TENANT, PM.orgId, entityId,
        new FederationEntity().entityIdentifier(entityIdentifier));
    this.modulesApi.createIntermediateWithId(TENANT, PM.orgId, UUID.randomUUID(),
        new Intermediate().entityId(entityId).active(true));
    return entityId;
  }

  private UUID createSubordinate(final JwtTestUtils.OrganisationType organisation, final UUID taImId,
      final String entityIdentifier) {
    return new SubordinatesApi(this.apiClient(organisation)).createSubordinate(TENANT, organisation.orgId,
        new Subordinate()
            .taImId(taImId)
            .entityIdentifier(entityIdentifier)
            .jwks(TestDataOperations.genJWKS().toJSONObject())
            .ecLocationAutomaticResolve(false)
            .ecLocation(entityIdentifier + "/.well-known/openid-federation"))
        .getSubordinateId();
  }

  private HostedEntity hostedEntity(final String entityIdentifier) {
    return new HostedEntity().entityIdentifier(entityIdentifier)
        .metadata(Map.of("hosted_entity", Map.of("name", "Hosted")));
  }

  private UUID createHostedEntity(final String entityIdentifier, final TrustmarkSource source) {
    final UUID hostedId = UUID.randomUUID();
    final HostedEntity hosted = this.hostedEntity(entityIdentifier);
    if (source != null) {
      hosted.addTrustMarkSourcesItem(source);
    }
    this.entitiesApi.createHostedEntityWithId(TENANT, PM.orgId, hostedId, hosted);
    return hostedId;
  }

  private IssuedTrustmark createTrustmark(final JwtTestUtils.OrganisationType organisation,
      final String issuerEntityIdentifier) {
    final ApiClient client = this.apiClient(organisation);
    final UUID entityId = UUID.randomUUID();
    new EntitiesApi(client).createFederationEntityWithId(TENANT, organisation.orgId, entityId,
        new FederationEntity().entityIdentifier(issuerEntityIdentifier));
    final UUID issuerId = new ModulesApi(client).createTrustmarkIssuerWithId(TENANT, organisation.orgId,
        UUID.randomUUID(), new TrustmarkIssuer().entityId(entityId).active(true)).getTrustmarkIssuerId();
    final UUID trustmarkId = UUID.randomUUID();
    final String trustmarkType = "https://www.pm.se/oidf/loa/" + UUID.randomUUID();
    new TrustmarksApi(client).createTrustmarkWithId(TENANT, organisation.orgId, trustmarkId,
        new Trustmark().trustmarkissuerId(issuerId).trustmarkType(trustmarkType));
    return new IssuedTrustmark(entityId, trustmarkType, trustmarkId);
  }

  private UUID createSubject(final JwtTestUtils.OrganisationType organisation, final UUID trustmarkId,
      final String subject) {
    return new TrustmarksApi(this.apiClient(organisation)).createTrustmarkSubject(TENANT, organisation.orgId,
        new TrustmarkSubject().trustmarkId(trustmarkId).subject(subject).revoked(false)).getTrustmarksubjectId();
  }

  private ApiClient apiClient(final JwtTestUtils.OrganisationType organisation) {
    final ApiClient client = new ApiClient();
    client.setBasePath("http://localhost:" + this.port);
    client.setBearerToken(this.jwtTestUtils.createJwt(organisation));
    return client;
  }

  private record IssuedTrustmark(UUID entityId, String trustmarkType, UUID trustmarkId) {
  }
}
