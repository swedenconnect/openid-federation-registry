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
package se.swedenconnect.oidf.registry.federationservice.service;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.federationservice.model.ModuleRecord;
import se.swedenconnect.oidf.registry.federationservice.model.TrustAnchorProperties;
import se.swedenconnect.oidf.registry.federationservice.serde.JsonRegistryLoader;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.module.model.ExternalTrustMark;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIssuer;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the trust_mark_issuers claim that {@link OidfApiService} builds for a trust anchor.
 */
@ExtendWith(MockitoExtension.class)
class OidfApiServiceTrustMarkIssuersTest {

  private static final String TA = "https://ta.example.com";
  private static final String TMI1 = "https://tmi1.example.com";
  private static final String TMI2 = "https://tmi2.example.com";
  private static final String EXTERNAL = "https://external.example.org";
  private static final String CERTIFIED_OP = "https://ta.example.com/trustmarks/certified-op";
  private static final String CERTIFIED_RP = "https://ta.example.com/trustmarks/certified-rp";

  private static final UUID ORGANIZATION_ID = UUID.randomUUID();

  @Mock
  private EntityRepository entityRepository;

  @Mock
  private SubordinateRepository subordinateRepository;

  @Mock
  private InstanceRepository instanceRepository;

  private OidfApiService service;

  @BeforeEach
  void setUp() throws Exception {
    this.service = new OidfApiService(TestDataOperations.genKey(), this.subordinateRepository,
        this.entityRepository, "https://issuer.example.com", this.instanceRepository, Duration.ofHours(1));
  }

  /** A trust mark issuer entity with the given trust marks, that the repository finds for the organization. */
  private void trustMarkIssuerInOrganization(final String entityIdentifier, final boolean active,
      final String... trustMarkTypes) {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityType(EntityType.FEDERATION_ENTITY);
    entity.setIssuer(entityIdentifier);
    entity.setSubject(entityIdentifier);
    final TrustMarkIssuer tmi = new TrustMarkIssuer();
    tmi.setActive(active);
    tmi.setEntity(entity);
    tmi.setTrustmarks(Arrays.stream(trustMarkTypes).map(type -> {
      final TrustMark trustMark = new TrustMark();
      trustMark.setTrustmarkType(type);
      return trustMark;
    }).toList());
    entity.setTrustmarkIssuer(tmi);
    when(this.entityRepository.findByOrganizationIdAndEntityKeyTypeAndIssuer(
        ORGANIZATION_ID, EntityType.FEDERATION_ENTITY, entityIdentifier)).thenReturn(java.util.Optional.of(entity));
  }

  private static TrustAnchorIssuer entry(final String issuer, final boolean auto, final String... types) {
    return new TrustAnchorIssuer(issuer, auto, List.of(types));
  }

  private static TrustAnchorIntermediateModule trustAnchor(final TrustAnchorIssuer... entries) {
    final FederationEntity entity = new FederationEntity();
    entity.setIssuer(TA);
    final Organization organization = new Organization();
    organization.setOrganizationId(ORGANIZATION_ID);
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setEntity(entity);
    module.setOrganization(organization);
    module.setTrustMarkIssuers(new ArrayList<>(List.of(entries)));
    return module;
  }

  private static ExternalTrustMark external(final String type, final String... issuers) {
    return new ExternalTrustMark(type, false, List.of(issuers));
  }

  private static ExternalTrustMark anyone(final String type) {
    return new ExternalTrustMark(type, true, List.of());
  }

  private static TrustAnchorIntermediateModule withExternal(final TrustAnchorIntermediateModule module,
      final ExternalTrustMark... trustMarks) {
    module.setExternalTrustMarks(new ArrayList<>(List.of(trustMarks)));
    return module;
  }

  private static Map<String, List<String>> asStrings(final Map<EntityID, List<EntityID>> trustMarkIssuers) {
    final Map<String, List<String>> result = new LinkedHashMap<>();
    trustMarkIssuers.forEach((type, issuers) ->
        result.put(type.getValue(), issuers.stream().map(EntityID::getValue).toList()));
    return result;
  }

  @Test
  @DisplayName("Auto includes every trust mark that the issuer has")
  void autoIncludesAllTrustMarksOfTheIssuer() {
    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP, CERTIFIED_RP);

    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(entry(TMI1, true)));

    assertThat(asStrings(result.getTrustMarkIssuers()))
        .containsOnly(Map.entry(CERTIFIED_OP, List.of(TMI1)), Map.entry(CERTIFIED_RP, List.of(TMI1)));
  }

  @Test
  @DisplayName("Auto follows the issuer: a trust mark added later is part of the next export")
  void autoFollowsTheCurrentTrustMarks() {
    final TrustAnchorIntermediateModule module = trustAnchor(entry(TMI1, true));
    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP);
    assertThat(asStrings(this.service.toTaIm(module).getTrustMarkIssuers())).containsOnlyKeys(CERTIFIED_OP);

    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP, CERTIFIED_RP);
    assertThat(asStrings(this.service.toTaIm(module).getTrustMarkIssuers()))
        .containsOnlyKeys(CERTIFIED_OP, CERTIFIED_RP);
  }

  @Test
  @DisplayName("External trust marks give the issuers outside the organization for each trust mark type")
  void externalTrustMarksAreExportedAsListed() {
    final TrustAnchorProperties result = this.service.toTaIm(withExternal(trustAnchor(),
        external(CERTIFIED_OP, EXTERNAL, "https://other.example.org"), external(CERTIFIED_RP, EXTERNAL)));

    assertThat(asStrings(result.getTrustMarkIssuers())).containsOnly(
        Map.entry(CERTIFIED_OP, List.of(EXTERNAL, "https://other.example.org")),
        Map.entry(CERTIFIED_RP, List.of(EXTERNAL)));
  }

  @Test
  @DisplayName("A trust mark type that anyone may issue is exported with an empty list of issuers")
  void allowAllIsExportedAsAnEmptyList() {
    final TrustAnchorProperties result = this.service.toTaIm(withExternal(trustAnchor(), anyone(CERTIFIED_OP)));

    assertThat(asStrings(result.getTrustMarkIssuers())).containsOnly(Map.entry(CERTIFIED_OP, List.of()));
  }

  @Test
  @DisplayName("Allow all wins over issuers that are listed for the same trust mark type")
  void allowAllWinsOverListedIssuers() {
    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP, CERTIFIED_RP);

    final TrustAnchorProperties result = this.service.toTaIm(
        withExternal(trustAnchor(entry(TMI1, true)), anyone(CERTIFIED_OP), external(CERTIFIED_RP, EXTERNAL)));

    assertThat(asStrings(result.getTrustMarkIssuers())).containsOnly(
        Map.entry(CERTIFIED_OP, List.of()), Map.entry(CERTIFIED_RP, List.of(TMI1, EXTERNAL)));
  }

  @Test
  @DisplayName("Auto entries, listed entries and external trust marks are merged per trust mark type")
  void entriesAreMergedPerTrustMarkType() {
    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP, CERTIFIED_RP);

    final TrustAnchorProperties result = this.service.toTaIm(
        withExternal(trustAnchor(entry(TMI1, true), entry(TMI2, false, CERTIFIED_OP)), external(CERTIFIED_OP, EXTERNAL)));

    assertThat(asStrings(result.getTrustMarkIssuers()))
        .containsOnly(Map.entry(CERTIFIED_OP, List.of(TMI1, TMI2, EXTERNAL)), Map.entry(CERTIFIED_RP, List.of(TMI1)));
  }

  @Test
  @DisplayName("A listed entry may name only some of the trust marks of a trust mark issuer of the organization")
  void explicitEntryForOwnIssuerIsNotExpanded() {
    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(entry(TMI1, false, CERTIFIED_RP)));

    assertThat(asStrings(result.getTrustMarkIssuers())).containsOnly(Map.entry(CERTIFIED_RP, List.of(TMI1)));
  }

  @Test
  @DisplayName("An auto entry is left out when the issuer is gone, inactive, or has no trust marks")
  void autoEntryIsLeftOutWhenTheIssuerCannotBeUsed() {
    // TMI1 is not found at all
    when(this.entityRepository.findByOrganizationIdAndEntityKeyTypeAndIssuer(
        ORGANIZATION_ID, EntityType.FEDERATION_ENTITY, TMI1)).thenReturn(java.util.Optional.empty());
    trustMarkIssuerInOrganization(TMI2, false, CERTIFIED_OP);
    trustMarkIssuerInOrganization("https://empty.example.com", true);

    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(
        entry(TMI1, true), entry(TMI2, true), entry("https://empty.example.com", true)));

    assertThat(result.getTrustMarkIssuers()).isNull();
  }

  @Test
  @DisplayName("A trust mark issuer of another organization is not found, so auto leaves it out")
  void autoIsLimitedToTheSameOrganization() {
    // Nothing is stubbed for ORGANIZATION_ID, the repository does not find the issuer there.
    when(this.entityRepository.findByOrganizationIdAndEntityKeyTypeAndIssuer(
        ORGANIZATION_ID, EntityType.FEDERATION_ENTITY, TMI1)).thenReturn(java.util.Optional.empty());

    assertThat(this.service.toTaIm(trustAnchor(entry(TMI1, true))).getTrustMarkIssuers()).isNull();
  }

  @Test
  @DisplayName("No duplicate issuers or trust mark types in the claim")
  void noDuplicates() {
    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP, CERTIFIED_OP);

    final TrustAnchorProperties result = this.service.toTaIm(withExternal(
        trustAnchor(entry(TMI1, true), entry(TMI1, false, CERTIFIED_OP)), external(CERTIFIED_OP, EXTERNAL, EXTERNAL)));

    assertThat(asStrings(result.getTrustMarkIssuers()))
        .containsOnly(Map.entry(CERTIFIED_OP, List.of(TMI1, EXTERNAL)));
  }

  @Test
  @DisplayName("The claim is serialized as trust-mark-issuers with the trust mark type as key")
  void serializesAsTypeToIssuers() {
    trustMarkIssuerInOrganization(TMI1, true, CERTIFIED_OP);
    final ModuleRecord record = new ModuleRecord();
    record.setTrustAnchors(List.of(this.service.toTaIm(withExternal(trustAnchor(entry(TMI1, true)), anyone(CERTIFIED_RP)))));

    final String json = new JsonRegistryLoader().toJson(record);

    assertThat(json).contains("\"trust-mark-issuers\":{\"" + CERTIFIED_OP + "\":[\"" + TMI1 + "\"],\""
        + CERTIFIED_RP + "\":[]}");
  }

  @Test
  @DisplayName("Nothing on the module gives no claim")
  void noIssuersGivesNoClaim() {
    assertThat(this.service.toTaIm(trustAnchor()).getTrustMarkIssuers()).isNull();
    assertThat(this.service.toTaIm(withExternal(trustAnchor())).getTrustMarkIssuers()).isNull();
  }
}
