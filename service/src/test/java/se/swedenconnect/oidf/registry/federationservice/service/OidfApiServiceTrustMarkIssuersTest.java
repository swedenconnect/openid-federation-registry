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
import se.swedenconnect.oidf.registry.federationservice.model.TrustAnchorProperties;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
  private static final String CERTIFIED_OP = "https://ta.example.com/trustmarks/certified-op";
  private static final String CERTIFIED_RP = "https://ta.example.com/trustmarks/certified-rp";

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

  private static FederationEntity trustMarkIssuerEntity(final String entityIdentifier, final boolean active,
      final String... trustMarkTypes) {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityType(EntityType.FEDERATION_ENTITY);
    entity.setIssuer(entityIdentifier);
    entity.setSubject(entityIdentifier);
    final TrustMarkIssuer tmi = new TrustMarkIssuer();
    tmi.setActive(active);
    tmi.setEntity(entity);
    tmi.setTrustmarks(java.util.Arrays.stream(trustMarkTypes).map(type -> {
      final TrustMark trustMark = new TrustMark();
      trustMark.setTrustmarkType(type);
      return trustMark;
    }).toList());
    entity.setTrustmarkIssuer(tmi);
    return entity;
  }

  private static TrustAnchorIntermediateModule trustAnchor(final List<String> trustMarkIssuers) {
    final FederationEntity entity = new FederationEntity();
    entity.setIssuer(TA);
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setEntity(entity);
    module.setTrustMarkIssuers(trustMarkIssuers);
    return module;
  }

  private static Map<String, List<String>> asStrings(final Map<EntityID, List<EntityID>> trustMarkIssuers) {
    final Map<String, List<String>> result = new java.util.LinkedHashMap<>();
    trustMarkIssuers.forEach((type, issuers) ->
        result.put(type.getValue(), issuers.stream().map(EntityID::getValue).toList()));
    return result;
  }

  @Test
  @DisplayName("Each trust mark type maps to the issuers that have a trust mark of that type")
  void mapsTrustMarkTypesToIssuers() {
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI1))
        .thenReturn(List.of(trustMarkIssuerEntity(TMI1, true, CERTIFIED_OP, CERTIFIED_RP)));
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI2))
        .thenReturn(List.of(trustMarkIssuerEntity(TMI2, true, CERTIFIED_OP)));

    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(List.of(TMI1, TMI2)));

    assertThat(asStrings(result.getTrustMarkIssuers()))
        .containsEntry(CERTIFIED_OP, List.of(TMI1, TMI2))
        .containsEntry(CERTIFIED_RP, List.of(TMI1))
        .hasSize(2);
  }

  @Test
  @DisplayName("An issuer that is not a trust mark issuer in this registry is left out")
  void externalIssuerIsLeftOut() {
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI1))
        .thenReturn(List.of(trustMarkIssuerEntity(TMI1, true, CERTIFIED_OP)));
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI2))
        .thenReturn(List.of());

    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(List.of(TMI1, TMI2)));

    assertThat(asStrings(result.getTrustMarkIssuers())).containsOnly(Map.entry(CERTIFIED_OP, List.of(TMI1)));
  }

  @Test
  @DisplayName("An inactive trust mark issuer is left out")
  void inactiveIssuerIsLeftOut() {
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI1))
        .thenReturn(List.of(trustMarkIssuerEntity(TMI1, false, CERTIFIED_OP)));

    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(List.of(TMI1)));

    assertThat(result.getTrustMarkIssuers()).isNull();
  }

  @Test
  @DisplayName("A repeated issuer and a repeated trust mark type give no duplicates")
  void noDuplicates() {
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI1))
        .thenReturn(List.of(trustMarkIssuerEntity(TMI1, true, CERTIFIED_OP, CERTIFIED_OP)));

    final TrustAnchorProperties result = this.service.toTaIm(trustAnchor(List.of(TMI1, TMI1)));

    assertThat(asStrings(result.getTrustMarkIssuers())).containsOnly(Map.entry(CERTIFIED_OP, List.of(TMI1)));
  }

  @Test
  @DisplayName("The claim is serialized as trust-mark-issuers with the trust mark type as key")
  void serializesAsTypeToIssuers() {
    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(EntityType.FEDERATION_ENTITY, TMI1))
        .thenReturn(List.of(trustMarkIssuerEntity(TMI1, true, CERTIFIED_OP)));
    final se.swedenconnect.oidf.registry.federationservice.model.ModuleRecord record =
        new se.swedenconnect.oidf.registry.federationservice.model.ModuleRecord();
    record.setTrustAnchors(List.of(this.service.toTaIm(trustAnchor(List.of(TMI1)))));

    final String json = new se.swedenconnect.oidf.registry.federationservice.serde.JsonRegistryLoader()
        .toJson(record);

    assertThat(json).contains("\"trust-mark-issuers\":{\"" + CERTIFIED_OP + "\":[\"" + TMI1 + "\"]}");
  }

  @Test
  @DisplayName("No trust mark issuers on the module gives no claim")
  void noIssuersGivesNoClaim() {
    assertThat(this.service.toTaIm(trustAnchor(null)).getTrustMarkIssuers()).isNull();
    assertThat(this.service.toTaIm(trustAnchor(List.of())).getTrustMarkIssuers()).isNull();
  }
}
