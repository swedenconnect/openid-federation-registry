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

package se.swedenconnect.oidf.registry.dto;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.entity.ResolverEntity;
import se.swedenconnect.oidf.registry.entity.SubordinateEntity;
import se.swedenconnect.oidf.registry.entity.TaImEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.entity.TrustmarkIssuerEntity;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityToDtoMapperTest {

  // -------------------------------------------------------------------------
  // toFederationEntity
  // -------------------------------------------------------------------------

  @Test
  void toFederationEntity_withoutModules() {
    final EntityEntity entity = createFederationEntity();

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, false);

    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getEntityIdentifier()).isEqualTo(entity.getIssuer());
    assertThat(dto.getAuthorityhints()).isEqualTo(entity.getAuthorityhints());
    assertThat(dto.getCrit()).isEqualTo(entity.getCrit());
    assertThat(dto.getTrustAnchor()).isNull();
    assertThat(dto.getIntermediate()).isNull();
    assertThat(dto.getResolver()).isNull();
    assertThat(dto.getTrustmarkIssuer()).isNull();
  }

  @Test
  void toFederationEntity_withTrustAnchorModule() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity taModule = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);
    entity.setTrustanchorIntermediate(taModule);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getTrustAnchor()).isNotNull();
    assertThat(dto.getTrustAnchor().getTrustAnchorId()).isEqualTo(taModule.getTaImId());
    assertThat(dto.getIntermediate()).isNull();
  }

  @Test
  void toFederationEntity_withIntermediateModule() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity imModule = createTaImEntity(entity, TaImEntity.Type.INTERMEDIATE);
    entity.setTrustanchorIntermediate(imModule);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getIntermediate()).isNotNull();
    assertThat(dto.getIntermediate().getIntermediateId()).isEqualTo(imModule.getTaImId());
    assertThat(dto.getTrustAnchor()).isNull();
  }

  @Test
  void toFederationEntity_withResolverModule() {
    final EntityEntity entity = createFederationEntity();
    final ResolverEntity resolver = ResolverEntity.builder()
        .resolverId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .resolveResponseDuration("PT30S")
        .trustAnchor("https://ta.example.com")
        .trustedKeys("{}")
        .stepRetryDuration("PT10S")
        .build();
    entity.setResolver(resolver);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getResolver()).isNotNull();
    assertThat(dto.getResolver().getResolverId()).isEqualTo(resolver.getResolverId());
  }

  @Test
  void toFederationEntity_withTrustmarkIssuerModule() {
    final EntityEntity entity = createFederationEntity();
    final TrustmarkIssuerEntity tmi = TrustmarkIssuerEntity.builder()
        .trustmarkIssuerId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .trustMarkTokenValidityDuration("PT1H")
        .build();
    entity.setTrustmarkIssuer(tmi);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getTrustmarkIssuer()).isNotNull();
    assertThat(dto.getTrustmarkIssuer().getTrustmarkIssuerId()).isEqualTo(tmi.getTrustmarkIssuerId());
  }

  // -------------------------------------------------------------------------
  // toDtoHosted
  // -------------------------------------------------------------------------

  @Test
  void toDtoHosted_mapsAllFields() {
    final EntityEntity entity = createHostedEntity("https://example.com", "https://example.com");

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getEntityIdentifier()).isEqualTo(entity.getIssuer());
    assertThat(dto.getAuthorityhints()).isEqualTo(entity.getAuthorityhints());
    assertThat(dto.getMetadata()).isEqualTo(entity.getMetadata());
    assertThat(dto.getEffectiveEcLocation()).isNull();
  }

  @Test
  void toDtoHosted_calculatesEcLocationWhenIssuerDiffersFromSubject() {
    final EntityEntity entity = createHostedEntity("https://telia.com/oidf", "https://sc.se");

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getEffectiveEcLocation()).isNotNull();
    assertThat(dto.getEffectiveEcLocation()).contains(".well-known/openid-federation");
    assertThat(dto.getCrit()).contains("ec_location");
  }

  @Test
  void toDtoHosted_throwsForNonHostedEntity() {
    final EntityEntity entity = createFederationEntity();

    assertThatThrownBy(() -> EntityToDtoMapper.toDtoHosted(entity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a HostedEntity");
  }

  @Test
  void toDtoHosted_withNullCrit() {
    final EntityEntity entity = createHostedEntity("https://example.com", "https://example.com");
    entity.setCrit(null);

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getCrit()).isNotNull().isEmpty();
  }

  @Test
  void toDtoHosted_parsesTrustMarkSources() {
    final EntityEntity entity = createHostedEntity("https://example.com", "https://example.com");
    entity.setTrustmarksources("[{\"trustMarkIssuer\":\"issuer1\",\"trustmarkId\":\"tm1\"}]");

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getTrustMarkSources()).hasSize(1);
    assertThat(dto.getTrustMarkSources().get(0).getTrustMarkIssuer()).isEqualTo("issuer1");
  }

  @Test
  void toDtoHosted_emptyTrustMarkSources() {
    final EntityEntity entity = createHostedEntity("https://example.com", "https://example.com");
    entity.setTrustmarksources(null);

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getTrustMarkSources()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // toDto(PolicyEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_policy() {
    final PolicyEntity policyEntity = new PolicyEntity();
    policyEntity.setPolicyId(UUID.randomUUID());
    policyEntity.setName("test-policy");
    policyEntity.setPolicy(Map.of("key", "value"));

    final PolicyDto dto = EntityToDtoMapper.toDto(policyEntity);

    assertThat(dto.getPolicyId()).isEqualTo(policyEntity.getPolicyId());
    assertThat(dto.getName()).isEqualTo("test-policy");
    assertThat(dto.getPolicy()).containsEntry("key", "value");
  }

  // -------------------------------------------------------------------------
  // toDto(TaImEntity) — TrustAnchor
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustAnchor() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity module = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);
    module.setTrustMarkIssuers(List.of("issuer1", "issuer2"));

    final TrustAnchorDto dto = EntityToDtoMapper.toDto(module);

    assertThat(dto.getTrustAnchorId()).isEqualTo(module.getTaImId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
    assertThat(dto.getTrustMarkIssuers()).containsExactly("issuer1", "issuer2");
  }

  @Test
  void toDto_trustAnchor_withSubordinates() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity module = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);
    final SubordinateEntity sub = createSubordinateEntity(module);
    module.setSubordinates(List.of(sub));

    final TrustAnchorDto dto = EntityToDtoMapper.toDto(module);

    assertThat(dto.getSubordinates()).hasSize(1);
    assertThat(dto.getSubordinates().get(0).getSubordinateId()).isEqualTo(sub.getSubordinateId());
  }

  @Test
  void toDto_trustAnchor_throwsForIntermediate() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity module = createTaImEntity(entity, TaImEntity.Type.INTERMEDIATE);

    assertThatThrownBy(() -> EntityToDtoMapper.toDto(module))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a TrustAnchor");
  }

  // -------------------------------------------------------------------------
  // toDtoIntermediate
  // -------------------------------------------------------------------------

  @Test
  void toDtoIntermediate() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity module = createTaImEntity(entity, TaImEntity.Type.INTERMEDIATE);

    final IntermediateDto dto = EntityToDtoMapper.toDtoIntermediate(module);

    assertThat(dto.getIntermediateId()).isEqualTo(module.getTaImId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
  }

  @Test
  void toDtoIntermediate_withSubordinates() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity module = createTaImEntity(entity, TaImEntity.Type.INTERMEDIATE);
    final SubordinateEntity sub = createSubordinateEntity(module);
    module.setSubordinates(List.of(sub));

    final IntermediateDto dto = EntityToDtoMapper.toDtoIntermediate(module);

    assertThat(dto.getSubordinates()).hasSize(1);
  }

  @Test
  void toDtoIntermediate_throwsForTrustAnchor() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity module = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);

    assertThatThrownBy(() -> EntityToDtoMapper.toDtoIntermediate(module))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a INTERMEDIATE");
  }

  // -------------------------------------------------------------------------
  // toDto(ResolverEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_resolver() {
    final EntityEntity entity = createFederationEntity();
    final ResolverEntity resolver = ResolverEntity.builder()
        .resolverId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .resolveResponseDuration("PT30S")
        .trustAnchor("https://ta.example.com")
        .trustedKeys("{\"keys\":[]}")
        .stepRetryDuration("PT10S")
        .build();

    final ResolverDto dto = EntityToDtoMapper.toDto(resolver);

    assertThat(dto.getResolverId()).isEqualTo(resolver.getResolverId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
    assertThat(dto.getResolveResponseDuration()).isEqualTo("PT30S");
    assertThat(dto.getTrustAnchor()).isEqualTo("https://ta.example.com");
    assertThat(dto.getTrustedKeys()).isEqualTo("{\"keys\":[]}");
    assertThat(dto.getStepRetryDuration()).isEqualTo("PT10S");
  }

  // -------------------------------------------------------------------------
  // toDto(TrustMarkEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustMark() {
    final TrustMarkEntity tmEntity = createTrustMarkEntity();

    final TrustmarkDto dto = EntityToDtoMapper.toDto(tmEntity);

    assertThat(dto.getTrustmarkId()).isEqualTo(tmEntity.getTrustmarkId());
    assertThat(dto.getTrustmarkissuerId()).isEqualTo(tmEntity.getTrustmarkIssuer().getTrustmarkIssuerId());
    assertThat(dto.getTrustmarkType()).isEqualTo("https://example.com/trustmark");
    assertThat(dto.getLogoUri()).isEqualTo("https://example.com/logo.png");
    assertThat(dto.getRefUri()).isEqualTo("https://example.com/ref");
    assertThat(dto.getDelegation()).isEqualTo("delegation-jwt");
  }

  // -------------------------------------------------------------------------
  // toDtoWithSubjects
  // -------------------------------------------------------------------------

  @Test
  void toDtoWithSubjects() {
    final TrustMarkEntity tmEntity = createTrustMarkEntity();
    final TrustMarkSubjectEntity subject = TrustMarkSubjectEntity.builder()
        .trustmarksubjectId(UUID.randomUUID())
        .trustMark(tmEntity)
        .subject("https://subject.example.com")
        .revoked(false)
        .granted(OffsetDateTime.now())
        .expires(OffsetDateTime.now().plusYears(1))
        .build();
    tmEntity.setTrustmarksubjects(List.of(subject));

    final TrustmarkWithSubjectsDto dto = EntityToDtoMapper.toDtoWithSubjects(tmEntity);

    assertThat(dto.getTrustmarkId()).isEqualTo(tmEntity.getTrustmarkId());
    assertThat(dto.getTrustmarkSubjects()).hasSize(1);
    assertThat(dto.getTrustmarkSubjects().getFirst().getSubject()).isEqualTo("https://subject.example.com");
  }

  @Test
  void toDtoWithSubjects_nullSubjects() {
    final TrustMarkEntity tmEntity = createTrustMarkEntity();
    tmEntity.setTrustmarksubjects(null);

    final TrustmarkWithSubjectsDto dto = EntityToDtoMapper.toDtoWithSubjects(tmEntity);

    assertThat(dto.getTrustmarkSubjects()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // toDtoWithSubjectsEmpty
  // -------------------------------------------------------------------------

  @Test
  void toDtoWithSubjectsEmpty() {
    final TrustMarkEntity tmEntity = createTrustMarkEntity();

    final TrustmarkWithSubjectsDto dto = EntityToDtoMapper.toDtoWithSubjectsEmpty(tmEntity);

    assertThat(dto.getTrustmarkId()).isEqualTo(tmEntity.getTrustmarkId());
    assertThat(dto.getTrustmarkSubjects()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // toDto(TrustMarkSubjectEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustMarkSubject() {
    final TrustMarkEntity tmEntity = createTrustMarkEntity();
    final OffsetDateTime granted = OffsetDateTime.now();
    final OffsetDateTime expires = OffsetDateTime.now().plusYears(1);
    final TrustMarkSubjectEntity subject = TrustMarkSubjectEntity.builder()
        .trustmarksubjectId(UUID.randomUUID())
        .trustMark(tmEntity)
        .subject("https://subject.example.com")
        .revoked(true)
        .granted(granted)
        .expires(expires)
        .build();

    final TrustmarkSubjectDto dto = EntityToDtoMapper.toDto(subject);

    assertThat(dto.getTrustmarksubjectId()).isEqualTo(subject.getTrustmarksubjectId());
    assertThat(dto.getTrustmarkId()).isEqualTo(tmEntity.getTrustmarkId());
    assertThat(dto.getSubject()).isEqualTo("https://subject.example.com");
    assertThat(dto.getRevoked()).isTrue();
    assertThat(dto.getGranted()).isEqualTo(granted);
    assertThat(dto.getExpires()).isEqualTo(expires);
  }

  // -------------------------------------------------------------------------
  // toDto(TrustmarkIssuerEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustmarkIssuer() {
    final EntityEntity entity = createFederationEntity();
    final TrustmarkIssuerEntity tmi = TrustmarkIssuerEntity.builder()
        .trustmarkIssuerId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .trustMarkTokenValidityDuration("PT1H")
        .build();

    final TrustmarkIssuerDto dto = EntityToDtoMapper.toDto(tmi);

    assertThat(dto.getTrustmarkIssuerId()).isEqualTo(tmi.getTrustmarkIssuerId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
    assertThat(dto.getTrustMarkTokenValidityDuration()).isEqualTo("PT1H");
  }

  // -------------------------------------------------------------------------
  // toDto(SubordinateEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_subordinate() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity taIm = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);
    final SubordinateEntity sub = createSubordinateEntity(taIm);

    final SubordinateDto dto = EntityToDtoMapper.toDto(sub);

    assertThat(dto.getSubordinateId()).isEqualTo(sub.getSubordinateId());
    assertThat(dto.getTaImId()).isEqualTo(taIm.getTaImId());
    assertThat(dto.getJwks()).isEqualTo("{\"keys\":[]}");
    assertThat(dto.getEntityIdentifier()).isEqualTo("https://subordinate.example.com");
    assertThat(dto.getCrit()).containsExactly("crit1", "crit2");
    assertThat(dto.getMetadataPolicyCrit()).containsExactly("mpc1", "mpc2");
    assertThat(dto.getEcLocation()).isEqualTo("https://ec.example.com");
    assertThat(dto.isEcLocationAutomaticResolve()).isFalse();
  }

  @Test
  void toDto_subordinate_withPolicy() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity taIm = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);
    final SubordinateEntity sub = createSubordinateEntity(taIm);
    final PolicyEntity policy = new PolicyEntity();
    policy.setPolicyId(UUID.randomUUID());
    policy.setPolicy(Map.of("rule", "allow"));
    sub.setPolicy(policy);

    final SubordinateDto dto = EntityToDtoMapper.toDto(sub);

    assertThat(dto.getPolicyId()).isEqualTo(policy.getPolicyId());
    assertThat(dto.getPolicy()).containsEntry("rule", "allow");
  }

  @Test
  void toDto_subordinate_withNullCritFields() {
    final EntityEntity entity = createFederationEntity();
    final TaImEntity taIm = createTaImEntity(entity, TaImEntity.Type.TRUSTANCHOR);
    final SubordinateEntity sub = createSubordinateEntity(taIm);
    sub.setCrit(null);
    sub.setMetadataPolicyCrit(null);

    final SubordinateDto dto = EntityToDtoMapper.toDto(sub);

    assertThat(dto.getCrit()).isNull();
    assertThat(dto.getMetadataPolicyCrit()).isNull();
  }

  // -------------------------------------------------------------------------
  // Test helpers
  // -------------------------------------------------------------------------

  private static EntityEntity createFederationEntity() {
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(UUID.randomUUID());
    entity.setEntityType(EntityKeyType.FEDERATION_ENTITY);
    entity.setIssuer("https://federation.example.com");
    entity.setSubject("https://federation.example.com");
    entity.setCrit(List.of("crit1"));
    entity.setAuthorityhints(List.of("https://ta.example.com"));
    return entity;
  }

  private static EntityEntity createHostedEntity(final String issuer, final String subject) {
    final EntityEntity entity = new EntityEntity();
    entity.setEntityId(UUID.randomUUID());
    entity.setEntityType(EntityKeyType.HOSTED_ENTITY);
    entity.setIssuer(issuer);
    entity.setSubject(subject);
    entity.setCrit(List.of("crit1"));
    entity.setAuthorityhints(List.of("https://ta.example.com"));
    entity.setMetadata(Map.of("federation_entity", Map.of("name", "test")));
    return entity;
  }

  private static TaImEntity createTaImEntity(final EntityEntity entity, final TaImEntity.Type type) {
    final TaImEntity module = new TaImEntity();
    module.setTaImId(UUID.randomUUID());
    module.setModuleType(type);
    module.setEntity(entity);
    module.setActive(true);
    module.setSubordinates(Collections.emptyList());
    return module;
  }

  private static SubordinateEntity createSubordinateEntity(final TaImEntity taIm) {
    final SubordinateEntity sub = new SubordinateEntity();
    sub.setSubordinateId(UUID.randomUUID());
    sub.setTaIm(taIm);
    sub.setJwks("{\"keys\":[]}");
    sub.setEntityidentifier("https://subordinate.example.com");
    sub.setCrit("crit1,crit2");
    sub.setMetadataPolicyCrit("mpc1,mpc2");
    sub.setEcLocation("https://ec.example.com");
    sub.setEcLocationAutomatic(false);
    return sub;
  }

  private static TrustMarkEntity createTrustMarkEntity() {
    final EntityEntity entity = createFederationEntity();
    final TrustmarkIssuerEntity tmi = TrustmarkIssuerEntity.builder()
        .trustmarkIssuerId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .trustMarkTokenValidityDuration("PT1H")
        .build();
    return TrustMarkEntity.builder()
        .trustmarkId(UUID.randomUUID())
        .trustmarkIssuer(tmi)
        .trustmarkType("https://example.com/trustmark")
        .logoUri("https://example.com/logo.png")
        .refUri("https://example.com/ref")
        .delegation("delegation-jwt")
        .build();
  }
}
