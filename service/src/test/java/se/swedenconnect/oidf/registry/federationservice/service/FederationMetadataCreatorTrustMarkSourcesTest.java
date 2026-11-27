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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkSourceProperty;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkSubjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the trust mark sources of an entity record.
 *
 * @author Per Fredrik Plars
 */
@ExtendWith(MockitoExtension.class)
class FederationMetadataCreatorTrustMarkSourcesTest {

  private static final UUID INSTANCE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final String ENTITY_ID = "https://www.polisen.se/op/sverigeid";
  private static final String ISSUER_ID = "https://www.pm.se/oidf/tmi";
  private static final String TRUST_MARK_TYPE = "https://id.swedenconnect.se/tm-prop/public-body";

  @Mock
  private SubordinateRepository subordinateRepository;
  @Mock
  private TrustMarkSubjectRepository trustMarkSubjectRepository;

  private Instance instance;

  @BeforeEach
  void setUp() {
    this.instance = new Instance();
    this.instance.setInstanceId(INSTANCE_ID);
  }

  /** A creator where the given instances resolve trust marks across every organization of the instance. */
  private FederationMetadataCreator creator(final Set<UUID> crossOrganizationTrustMarkInstances) {
    return new FederationMetadataCreator(this.subordinateRepository, "hosted", Set.of(),
        this.trustMarkSubjectRepository, crossOrganizationTrustMarkInstances);
  }

  @Test
  @DisplayName("A granted trust mark is a trust mark source of the hosted entity")
  void grantedTrustMarkIsATrustMarkSource() {
    final Organization organization = this.organization();
    final FederationEntity hostedEntity = this.hostedEntity(organization);

    when(this.trustMarkSubjectRepository.findByOrganizationIdAndSubject(organization.getOrganizationId(), ENTITY_ID))
        .thenReturn(List.of(this.granted(TRUST_MARK_TYPE, false)));

    final EntityRecord record = this.creator(Set.of()).createEntityResponse(hostedEntity);

    assertThat(record.getTrustMarkSource())
        .extracting(source -> source.issuer().getValue(), TrustMarkSourceProperty::trustMarkType)
        .containsExactly(org.assertj.core.groups.Tuple.tuple(ISSUER_ID, TRUST_MARK_TYPE));
  }

  @Test
  @DisplayName("The stored sources are kept and the granted trust marks follow them, without duplicates")
  void storedSourcesAreKeptAndGrantedOnesFollowWithoutDuplicates() {
    final Organization organization = this.organization();
    final FederationEntity hostedEntity = this.hostedEntity(organization);
    hostedEntity.setTrustmarksources("""
        [{"trustMarkIssuer":"https://www.external.se/tmi","trustmarkId":"https://www.external.se/tm/loa3"},
         {"trustMarkIssuer":"%s","trustmarkId":"%s"}]""".formatted(ISSUER_ID, TRUST_MARK_TYPE));

    when(this.trustMarkSubjectRepository.findByOrganizationIdAndSubject(organization.getOrganizationId(), ENTITY_ID))
        .thenReturn(List.of(this.granted(TRUST_MARK_TYPE, false), this.granted("https://www.pm.se/oidf/tm/other",
            false)));

    final EntityRecord record = this.creator(Set.of()).createEntityResponse(hostedEntity);

    assertThat(record.getTrustMarkSource())
        .extracting(TrustMarkSourceProperty::trustMarkType)
        .containsExactly("https://www.external.se/tm/loa3", TRUST_MARK_TYPE, "https://www.pm.se/oidf/tm/other");
  }

  @Test
  @DisplayName("A revoked trust mark is no trust mark source")
  void revokedTrustMarkIsNoTrustMarkSource() {
    final Organization organization = this.organization();
    final FederationEntity hostedEntity = this.hostedEntity(organization);

    when(this.trustMarkSubjectRepository.findByOrganizationIdAndSubject(organization.getOrganizationId(), ENTITY_ID))
        .thenReturn(List.of(this.granted(TRUST_MARK_TYPE, true)));

    final EntityRecord record = this.creator(Set.of()).createEntityResponse(hostedEntity);

    assertThat(record.getTrustMarkSource()).isNull();
  }

  @Test
  @DisplayName("A trust mark granted by an issuer of another organization is picked up when the instance allows it")
  void grantedByAnotherOrganizationOfTheInstanceIsASourceWhenEnabled() {
    final FederationEntity hostedEntity = this.hostedEntity(this.organization());

    when(this.trustMarkSubjectRepository.findByInstanceIdAndSubject(INSTANCE_ID, ENTITY_ID))
        .thenReturn(List.of(this.granted(TRUST_MARK_TYPE, false)));

    final EntityRecord record = this.creator(Set.of(INSTANCE_ID)).createEntityResponse(hostedEntity);

    assertThat(record.getTrustMarkSource())
        .extracting(TrustMarkSourceProperty::trustMarkType)
        .containsExactly(TRUST_MARK_TYPE);
    verify(this.trustMarkSubjectRepository, never()).findByOrganizationIdAndSubject(any(), any());
  }

  @Test
  @DisplayName("Another instance being enabled does not widen this one")
  void anotherInstanceBeingEnabledDoesNotWidenThisOne() {
    final Organization organization = this.organization();
    final FederationEntity hostedEntity = this.hostedEntity(organization);

    when(this.trustMarkSubjectRepository.findByOrganizationIdAndSubject(organization.getOrganizationId(), ENTITY_ID))
        .thenReturn(List.of());

    final EntityRecord record = this.creator(Set.of(UUID.randomUUID())).createEntityResponse(hostedEntity);

    assertThat(record.getTrustMarkSource()).isNull();
    verify(this.trustMarkSubjectRepository, never()).findByInstanceIdAndSubject(any(), any());
  }

  /** An organization of the instance, each call a different one. */
  private Organization organization() {
    final Organization organization = new Organization();
    organization.setOrganizationId(UUID.randomUUID());
    organization.setInstance(this.instance);
    return organization;
  }

  private FederationEntity hostedEntity(final Organization organization) {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityType(EntityType.HOSTED_ENTITY);
    entity.setIssuer(ENTITY_ID);
    entity.setSubject(ENTITY_ID);
    entity.setMetadata(Map.of("openid_relying_party", Map.of()));
    entity.setOrganization(organization);
    return entity;
  }

  /** A subject entry on a trust mark of the issuer, for the hosted entity. */
  private TrustMarkSubject granted(final String trustMarkType, final boolean revoked) {
    final FederationEntity issuerEntity = new FederationEntity();
    issuerEntity.setEntityType(EntityType.FEDERATION_ENTITY);
    issuerEntity.setIssuer(ISSUER_ID);
    issuerEntity.setSubject(ISSUER_ID);

    final TrustMarkIssuer issuer = new TrustMarkIssuer();
    issuer.setEntity(issuerEntity);

    final TrustMark trustMark = new TrustMark();
    trustMark.setTrustmarkId(UUID.randomUUID());
    trustMark.setTrustmarkType(trustMarkType);
    trustMark.setTrustmarkIssuer(issuer);
    trustMark.setTrustmarksubjects(new ArrayList<>());

    final TrustMarkSubject subject = new TrustMarkSubject();
    subject.setTrustmarksubjectId(UUID.randomUUID());
    subject.setSubject(ENTITY_ID);
    subject.setRevoked(revoked);
    subject.setTrustMark(trustMark);
    return subject;
  }

}
