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
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.mapper.EntityToDtoMapper;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.module.mapper.ModuleToDtoMapper;
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.model.Resolver;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.mapper.SubordinateMapper;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkWithSubjectsDto;
import se.swedenconnect.oidf.registry.trustmark.mapper.TrustmarkToDtoMapper;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;

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
    final FederationEntity entity = createFederationEntity();

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
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule taModule = createTaImEntity(entity, ModuleType.TRUSTANCHOR);
    entity.setTrustanchorIntermediate(taModule);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getTrustAnchor()).isNotNull();
    assertThat(dto.getTrustAnchor().getTrustAnchorId()).isEqualTo(taModule.getTaImId());
    assertThat(dto.getIntermediate()).isNull();
  }

  @Test
  void toFederationEntity_withIntermediateModule() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule imModule = createTaImEntity(entity, ModuleType.INTERMEDIATE);
    entity.setTrustanchorIntermediate(imModule);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getIntermediate()).isNotNull();
    assertThat(dto.getIntermediate().getIntermediateId()).isEqualTo(imModule.getTaImId());
    assertThat(dto.getTrustAnchor()).isNull();
  }

  @Test
  void toFederationEntity_withResolverModule() {
    final FederationEntity entity = createFederationEntity();
    final Resolver resolver = Resolver.builder()
        .resolverId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .resolveResponseDuration("PT30S")
        .trustAnchor("https://ta.example.com")
        .trustedKeys(Collections.emptyMap())
        .stepRetryDuration("PT10S")
        .build();
    entity.setResolver(resolver);

    final FederationEntityWithModulesDto dto = EntityToDtoMapper.toFederationEntity(entity, true);

    assertThat(dto.getResolver()).isNotNull();
    assertThat(dto.getResolver().getResolverId()).isEqualTo(resolver.getResolverId());
  }

  @Test
  void toFederationEntity_withTrustmarkIssuerModule() {
    final FederationEntity entity = createFederationEntity();
    final TrustMarkIssuer tmi = TrustMarkIssuer.builder()
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
    final FederationEntity entity = createHostedEntity("https://example.com", "https://example.com");

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getEntityIdentifier()).isEqualTo(entity.getIssuer());
    assertThat(dto.getAuthorityhints()).isEqualTo(entity.getAuthorityhints());
    assertThat(dto.getMetadata()).isEqualTo(entity.getMetadata());
    assertThat(dto.getEffectiveEcLocation()).isNull();
  }

  @Test
  void toDtoHosted_calculatesEcLocationWhenIssuerDiffersFromSubject() {
    final FederationEntity entity = createHostedEntity("https://telia.com/oidf", "https://sc.se");

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getEffectiveEcLocation()).isNotNull();
    assertThat(dto.getEffectiveEcLocation()).doesNotContain(".well-known/openid-federation");
    assertThat(dto.getCrit()).contains("ec_location");
  }

  @Test
  void toDtoHosted_throwsForNonHostedEntity() {
    final FederationEntity entity = createFederationEntity();

    assertThatThrownBy(() -> EntityToDtoMapper.toDtoHosted(entity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a HostedEntity");
  }

  @Test
  void toDtoHosted_withNullCrit() {
    final FederationEntity entity = createHostedEntity("https://example.com", "https://example.com");
    entity.setCrit(null);

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getCrit()).isNotNull().isEmpty();
  }

  @Test
  void toDtoHosted_parsesTrustMarkSources() {
    final FederationEntity entity = createHostedEntity("https://example.com", "https://example.com");
    entity.setTrustmarksources("[{\"trustMarkIssuer\":\"issuer1\",\"trustmarkId\":\"tm1\"}]");

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getTrustMarkSources()).hasSize(1);
    assertThat(dto.getTrustMarkSources().get(0).getTrustMarkIssuer()).isEqualTo("issuer1");
  }

  @Test
  void toDtoHosted_emptyTrustMarkSources() {
    final FederationEntity entity = createHostedEntity("https://example.com", "https://example.com");
    entity.setTrustmarksources(null);

    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);

    assertThat(dto.getTrustMarkSources()).isEmpty();
  }



  // -------------------------------------------------------------------------
  // toDto(TaImEntity) — TrustAnchor
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustAnchor() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule module = createTaImEntity(entity, ModuleType.TRUSTANCHOR);
    module.setTrustMarkIssuers(List.of("issuer1", "issuer2"));

    final TrustAnchorDto dto = ModuleToDtoMapper.toDto(module);

    assertThat(dto.getTrustAnchorId()).isEqualTo(module.getTaImId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
    assertThat(dto.getTrustMarkIssuers()).containsExactly("issuer1", "issuer2");
  }

  @Test
  void toDto_trustAnchor_withSubordinates() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule module = createTaImEntity(entity, ModuleType.TRUSTANCHOR);
    final Subordinate sub = createSubordinateEntity(module);
    module.setSubordinates(List.of(sub));

    final TrustAnchorDto dto = ModuleToDtoMapper.toDto(module);

    assertThat(dto.getSubordinates()).hasSize(1);
    assertThat(dto.getSubordinates().get(0).getSubordinateId()).isEqualTo(sub.getSubordinateId());
  }

  @Test
  void toDto_trustAnchor_throwsForIntermediate() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule module = createTaImEntity(entity, ModuleType.INTERMEDIATE);

    assertThatThrownBy(() -> ModuleToDtoMapper.toDto(module))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a TrustAnchor");
  }

  // -------------------------------------------------------------------------
  // toDtoIntermediate
  // -------------------------------------------------------------------------

  @Test
  void toDtoIntermediate() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule module = createTaImEntity(entity, ModuleType.INTERMEDIATE);

    final IntermediateDto dto = ModuleToDtoMapper.toDtoIntermediate(module);

    assertThat(dto.getIntermediateId()).isEqualTo(module.getTaImId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
  }

  @Test
  void toDtoIntermediate_withSubordinates() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule module = createTaImEntity(entity, ModuleType.INTERMEDIATE);
    final Subordinate sub = createSubordinateEntity(module);
    module.setSubordinates(List.of(sub));

    final IntermediateDto dto = ModuleToDtoMapper.toDtoIntermediate(module);

    assertThat(dto.getSubordinates()).hasSize(1);
  }

  @Test
  void toDtoIntermediate_throwsForTrustAnchor() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule module = createTaImEntity(entity, ModuleType.TRUSTANCHOR);

    assertThatThrownBy(() -> ModuleToDtoMapper.toDtoIntermediate(module))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a INTERMEDIATE");
  }

  // -------------------------------------------------------------------------
  // toDto(ResolverEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_resolver() {
    final FederationEntity entity = createFederationEntity();
    final Resolver resolver = Resolver.builder()
        .resolverId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .resolveResponseDuration("PT30S")
        .trustAnchor("https://ta.example.com")
        .trustedKeys(TestDataOperations.genJWKS().toJSONObject())
        .stepRetryDuration("PT10S")
        .build();

    final ResolverDto dto = ModuleToDtoMapper.toDto(resolver);

    assertThat(dto.getResolverId()).isEqualTo(resolver.getResolverId());
    assertThat(dto.getEntityId()).isEqualTo(entity.getEntityId());
    assertThat(dto.getActive()).isTrue();
    assertThat(dto.getResolveResponseDuration()).isEqualTo("PT30S");
    assertThat(dto.getTrustAnchor()).isEqualTo("https://ta.example.com");
    assertThat(dto.getTrustedKeys()).isEqualTo(resolver.getTrustedKeys());
    assertThat(dto.getStepRetryDuration()).isEqualTo("PT10S");
  }

  // -------------------------------------------------------------------------
  // toDto(TrustMarkEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustMark() {
    final TrustMark tmEntity = createTrustMarkEntity();

    final TrustmarkDto dto = TrustmarkToDtoMapper.toDto(tmEntity);

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
    final TrustMark tmEntity = createTrustMarkEntity();
    final TrustMarkSubject subject = TrustMarkSubject.builder()
        .trustmarksubjectId(UUID.randomUUID())
        .trustMark(tmEntity)
        .subject("https://subject.example.com")
        .revoked(false)
        .granted(OffsetDateTime.now())
        .expires(OffsetDateTime.now().plusYears(1))
        .build();
    tmEntity.setTrustmarksubjects(List.of(subject));

    final TrustmarkWithSubjectsDto dto = TrustmarkToDtoMapper.toDtoWithSubjects(tmEntity);

    assertThat(dto.getTrustmarkId()).isEqualTo(tmEntity.getTrustmarkId());
    assertThat(dto.getTrustmarkSubjects()).hasSize(1);
    assertThat(dto.getTrustmarkSubjects().getFirst().getSubject()).isEqualTo("https://subject.example.com");
  }

  @Test
  void toDtoWithSubjects_nullSubjects() {
    final TrustMark tmEntity = createTrustMarkEntity();
    tmEntity.setTrustmarksubjects(null);

    final TrustmarkWithSubjectsDto dto = TrustmarkToDtoMapper.toDtoWithSubjects(tmEntity);

    assertThat(dto.getTrustmarkSubjects()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // toDtoWithSubjectsEmpty
  // -------------------------------------------------------------------------

  @Test
  void toDtoWithSubjectsEmpty() {
    final TrustMark tmEntity = createTrustMarkEntity();

    final TrustmarkWithSubjectsDto dto = TrustmarkToDtoMapper.toDtoWithSubjectsEmpty(tmEntity);

    assertThat(dto.getTrustmarkId()).isEqualTo(tmEntity.getTrustmarkId());
    assertThat(dto.getTrustmarkSubjects()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // toDto(TrustMarkSubjectEntity)
  // -------------------------------------------------------------------------

  @Test
  void toDto_trustMarkSubject() {
    final TrustMark tmEntity = createTrustMarkEntity();
    final OffsetDateTime granted = OffsetDateTime.now();
    final OffsetDateTime expires = OffsetDateTime.now().plusYears(1);
    final TrustMarkSubject subject = TrustMarkSubject.builder()
        .trustmarksubjectId(UUID.randomUUID())
        .trustMark(tmEntity)
        .subject("https://subject.example.com")
        .revoked(true)
        .granted(granted)
        .expires(expires)
        .build();

    final TrustmarkSubjectDto dto = TrustmarkToDtoMapper.toDto(subject);

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
    final FederationEntity entity = createFederationEntity();
    final TrustMarkIssuer tmi = TrustMarkIssuer.builder()
        .trustmarkIssuerId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .trustMarkTokenValidityDuration("PT1H")
        .build();

    final TrustmarkIssuerDto dto = ModuleToDtoMapper.toDto(tmi);

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
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule taIm = createTaImEntity(entity, ModuleType.TRUSTANCHOR);
    final Subordinate sub = createSubordinateEntity(taIm);

    final SubordinateDto dto = SubordinateMapper.toDto(sub);

    assertThat(dto.getSubordinateId()).isEqualTo(sub.getSubordinateId());
    assertThat(dto.getTaImId()).isEqualTo(taIm.getTaImId());
    assertThat(dto.getJwks()).isEqualTo(sub.getJwks());
    assertThat(dto.getEntityIdentifier()).isEqualTo("https://subordinate.example.com");
    assertThat(dto.getCrit()).containsExactly("crit1", "crit2");
    assertThat(dto.getMetadataPolicyCrit()).containsExactly("mpc1", "mpc2");
    assertThat(dto.getEcLocation()).isEqualTo("https://ec.example.com");
    assertThat(dto.getEcLocationAutomaticResolve()).isFalse();
  }

  @Test
  void toDto_subordinate_withPolicy() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule taIm = createTaImEntity(entity, ModuleType.TRUSTANCHOR);
    final Subordinate sub = createSubordinateEntity(taIm);

    final SubordinateDto dto = SubordinateMapper.toDto(sub);

    assertThat(dto.getMetadataPolicy()).isEqualTo(sub.getMetadataPolicy());
  }

  @Test
  void toDto_subordinate_withNullCritFields() {
    final FederationEntity entity = createFederationEntity();
    final TrustAnchorIntermediateModule taIm = createTaImEntity(entity, ModuleType.TRUSTANCHOR);
    final Subordinate sub = createSubordinateEntity(taIm);
    sub.setCrit(null);
    sub.setMetadataPolicyCrit(null);

    final SubordinateDto dto = SubordinateMapper.toDto(sub);

    assertThat(dto.getCrit()).isNull();
    assertThat(dto.getMetadataPolicyCrit()).isNull();
  }

  // -------------------------------------------------------------------------
  // Test helpers
  // -------------------------------------------------------------------------

  private static FederationEntity createFederationEntity() {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityId(UUID.randomUUID());
    entity.setEntityType(EntityType.FEDERATION_ENTITY);
    entity.setIssuer("https://federation.example.com");
    entity.setSubject("https://federation.example.com");
    entity.setCrit(List.of("crit1"));
    entity.setAuthorityhints(List.of("https://ta.example.com"));
    return entity;
  }

  private static FederationEntity createHostedEntity(final String issuer, final String subject) {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityId(UUID.randomUUID());
    entity.setEntityType(EntityType.HOSTED_ENTITY);
    entity.setIssuer(issuer);
    entity.setSubject(subject);
    entity.setCrit(List.of("crit1"));
    entity.setAuthorityhints(List.of("https://ta.example.com"));
    entity.setMetadata(Map.of("federation_entity", Map.of("name", "test")));
    return entity;
  }

  private static TrustAnchorIntermediateModule createTaImEntity(final FederationEntity entity, final ModuleType type) {
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setTaImId(UUID.randomUUID());
    module.setModuleType(type);
    module.setEntity(entity);
    module.setActive(true);
    module.setSubordinates(Collections.emptyList());
    return module;
  }

  private static Subordinate createSubordinateEntity(final TrustAnchorIntermediateModule taIm) {
    final Subordinate sub = new Subordinate();
    sub.setSubordinateId(UUID.randomUUID());
    sub.setTaIm(taIm);
    sub.setJwks(TestDataOperations.genJWKS().toJSONObject());

    sub.setEntityidentifier("https://subordinate.example.com");
    sub.setCrit(List.of("crit1", "crit2"));
    sub.setMetadataPolicyCrit(List.of("mpc1", "mpc2"));
    sub.setEcLocation("https://ec.example.com");
    sub.setEcLocationAutomatic(false);
    sub.setMetadataPolicy(new TestDataOperations().createPolicy());
    return sub;
  }

  private static TrustMark createTrustMarkEntity() {
    final FederationEntity entity = createFederationEntity();
    final TrustMarkIssuer tmi = TrustMarkIssuer.builder()
        .trustmarkIssuerId(UUID.randomUUID())
        .entity(entity)
        .active(true)
        .trustMarkTokenValidityDuration("PT1H")
        .build();
    return TrustMark.builder()
        .trustmarkId(UUID.randomUUID())
        .trustmarkIssuer(tmi)
        .trustmarkType("https://example.com/trustmark")
        .logoUri("https://example.com/logo.png")
        .refUri("https://example.com/ref")
        .delegation("delegation-jwt")
        .build();
  }
}
