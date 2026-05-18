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
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.mapper.DtoToEntityMapper;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.fixture.TestDataOperations;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.module.mapper.DtoToModuleMapper;
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.model.Resolver;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.mapper.DtoToSubordinateMapper;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSourceDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.trustmark.mapper.DtoToTrustmarkMapper;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoToEntityMapperTest {

  private static final UUID ID = UUID.randomUUID();

  // -------------------------------------------------------------------------
  // toEntity — FederationEntityDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_federationEntityDto() {
    final FederationEntityDto dto = new FederationEntityDto();
    dto.setEntityIdentifier("https://federation.example.com");
    dto.setCrit(List.of("crit1"));
    dto.setAuthorityhints(List.of("https://ta.example.com"));
    final Organization org = createOrganization();

    final FederationEntity entity = DtoToEntityMapper.toEntity(ID, dto, EntityType.FEDERATION_ENTITY, org);

    assertThat(entity.getEntityId()).isEqualTo(ID);
    assertThat(entity.getEntityType()).isEqualTo(EntityType.FEDERATION_ENTITY);
    assertThat(entity.getOrganization()).isEqualTo(org);
    assertThat(entity.getIssuer()).isEqualTo("https://federation.example.com");
    assertThat(entity.getSubject()).isEqualTo("https://federation.example.com");
    assertThat(entity.getCrit()).containsExactly("crit1");
    assertThat(entity.getAuthorityhints()).containsExactly("https://ta.example.com");
  }

  // -------------------------------------------------------------------------
  // toEntity — HostedEntityDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_hostedEntityDto() {
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityIdentifier("https://hosted.example.com");
    dto.setMetadata(Map.of("federation_entity", Map.of("name", "test")));
    final Organization org = createOrganization();

    final FederationEntity entity = DtoToEntityMapper.toEntity(ID, dto, EntityType.HOSTED_ENTITY, org);

    assertThat(entity.getEntityId()).isEqualTo(ID);
    assertThat(entity.getEntityType()).isEqualTo(EntityType.HOSTED_ENTITY);
    assertThat(entity.getOrganization()).isEqualTo(org);
    assertThat(entity.getIssuer()).isEqualTo("https://hosted.example.com");
    assertThat(entity.getSubject()).isEqualTo("https://hosted.example.com");
    assertThat(entity.getMetadata()).containsKey("federation_entity");
  }

  @Test
  void toEntity_hostedEntityDto_withTrustMarkSources() {
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityIdentifier("https://hosted.example.com");
    final TrustmarkSourceDto source = new TrustmarkSourceDto();
    source.setTrustMarkIssuer("issuer1");
    source.setTrustmarkId("tm1");
    dto.setTrustMarkSources(List.of(source));
    final Organization org = createOrganization();

    final FederationEntity entity = DtoToEntityMapper.toEntity(ID, dto, EntityType.HOSTED_ENTITY, org);

    assertThat(entity.getTrustmarksources()).contains("issuer1");
    assertThat(entity.getTrustmarksources()).contains("tm1");
  }




  // -------------------------------------------------------------------------
  // toEntity — TrustAnchorDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_trustAnchorDto() {
    final TrustAnchorDto dto = new TrustAnchorDto();
    dto.setActive(true);
    dto.setTrustMarkIssuers(List.of("issuer1"));
    final FederationEntity entityEntity = createEntityEntity();
    final Organization org = createOrganization();

    final TrustAnchorIntermediateModule module = DtoToModuleMapper.toEntity(ID, dto, entityEntity, org);

    assertThat(module.getTaImId()).isEqualTo(ID);
    assertThat(module.getModuleType()).isEqualTo(ModuleType.TRUSTANCHOR);
    assertThat(module.getEntity()).isEqualTo(entityEntity);
    assertThat(module.getOrganization()).isEqualTo(org);
    assertThat(module.getActive()).isTrue();
    assertThat(module.getTrustMarkIssuers()).containsExactly("issuer1");
  }

  // -------------------------------------------------------------------------
  // toEntity — IntermediateDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_intermediateDto() {
    final IntermediateDto dto = new IntermediateDto();
    dto.setActive(true);
    final FederationEntity entityEntity = createEntityEntity();
    final Organization org = createOrganization();

    final TrustAnchorIntermediateModule module = DtoToModuleMapper.toEntity(ID, dto, entityEntity, org);

    assertThat(module.getTaImId()).isEqualTo(ID);
    assertThat(module.getModuleType()).isEqualTo(ModuleType.INTERMEDIATE);
    assertThat(module.getEntity()).isEqualTo(entityEntity);
    assertThat(module.getActive()).isTrue();
  }

  // -------------------------------------------------------------------------
  // toEntity — ResolverDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_resolverDto() {
    final ResolverDto dto = new ResolverDto();
    dto.setActive(true);
    dto.setResolveResponseDuration("PT30S");
    dto.setTrustAnchor("https://ta.example.com");
    dto.setTrustedKeys(TestDataOperations.genJWKS().toJSONObject());
    dto.setStepRetryDuration("PT10S");
    dto.setStepCachedValueThreshold(5);
    final FederationEntity entityEntity = createEntityEntity();

    final Resolver entity = DtoToModuleMapper.toEntity(ID, dto, entityEntity);

    assertThat(entity.getResolverId()).isEqualTo(ID);
    assertThat(entity.getEntity()).isEqualTo(entityEntity);
    assertThat(entity.getActive()).isTrue();
    assertThat(entity.getResolveResponseDuration()).isEqualTo("PT30S");
    assertThat(entity.getTrustAnchor()).isEqualTo("https://ta.example.com");
    assertThat(entity.getTrustedKeys()).isEqualTo(dto.getTrustedKeys());
    assertThat(entity.getStepRetryDuration()).isEqualTo("PT10S");
    assertThat(entity.getStepCachedValueThreshold()).isEqualTo(5);
  }

  // -------------------------------------------------------------------------
  // toEntity — TrustmarkDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_trustmarkDto() {
    final TrustmarkDto dto = new TrustmarkDto();
    dto.setTrustmarkType("https://example.com/trustmark");
    dto.setLogoUri("https://example.com/logo.png");
    dto.setRefUri("https://example.com/ref");
    dto.setDelegation("delegation-jwt");
    final TrustMarkIssuer issuer = TrustMarkIssuer.builder()
        .trustmarkIssuerId(UUID.randomUUID())
        .build();

    final TrustMark entity = DtoToTrustmarkMapper.toEntity(ID, dto, issuer);

    assertThat(entity.getTrustmarkId()).isEqualTo(ID);
    assertThat(entity.getTrustmarkIssuer()).isEqualTo(issuer);
    assertThat(entity.getTrustmarkType()).isEqualTo("https://example.com/trustmark");
    assertThat(entity.getLogoUri()).isEqualTo("https://example.com/logo.png");
    assertThat(entity.getRefUri()).isEqualTo("https://example.com/ref");
    assertThat(entity.getDelegation()).isEqualTo("delegation-jwt");
  }

  // -------------------------------------------------------------------------
  // toEntity — TrustmarkSubjectDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_trustmarkSubjectDto() {
    final OffsetDateTime granted = OffsetDateTime.now();
    final OffsetDateTime expires = OffsetDateTime.now().plusYears(1);
    final TrustmarkSubjectDto dto = new TrustmarkSubjectDto();
    dto.setSubject("https://subject.example.com");
    dto.setRevoked(false);
    dto.setGranted(granted);
    dto.setExpires(expires);
    final TrustMark trustMark = TrustMark.builder()
        .trustmarkId(UUID.randomUUID())
        .build();

    final TrustMarkSubject entity = DtoToTrustmarkMapper.toEntity(ID, dto, trustMark);

    assertThat(entity.getTrustmarksubjectId()).isEqualTo(ID);
    assertThat(entity.getTrustMark()).isEqualTo(trustMark);
    assertThat(entity.getSubject()).isEqualTo("https://subject.example.com");
    assertThat(entity.getRevoked()).isFalse();
    assertThat(entity.getGranted()).isEqualTo(granted);
    assertThat(entity.getExpires()).isEqualTo(expires);
  }

  // -------------------------------------------------------------------------
  // toEntity — TrustmarkIssuerDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_trustmarkIssuerDto() {
    final TrustmarkIssuerDto dto = new TrustmarkIssuerDto();
    dto.setActive(true);
    dto.setTrustMarkTokenValidityDuration("PT1H");
    final FederationEntity entityEntity = createEntityEntity();

    final TrustMarkIssuer entity = DtoToModuleMapper.toEntity(ID, dto, entityEntity);

    assertThat(entity.getTrustmarkIssuerId()).isEqualTo(ID);
    assertThat(entity.getEntity()).isEqualTo(entityEntity);
    assertThat(entity.getActive()).isTrue();
    assertThat(entity.getTrustMarkTokenValidityDuration()).isEqualTo("PT1H");
  }

  // -------------------------------------------------------------------------
  // toEntity — SubordinateDto
  // -------------------------------------------------------------------------

  @Test
  void toEntity_subordinateDto() {
    final SubordinateDto dto = new SubordinateDto();
    dto.setJwks(TestDataOperations.genJWKS().toJSONObject());

    dto.setEntityIdentifier("https://subordinate.example.com");
    dto.setCrit(List.of("crit1", "crit2"));
    dto.setMetadataPolicyCrit(List.of("mpc1", "mpc2"));
    dto.setEcLocation("https://ec.example.com");
    dto.setEcLocationAutomaticResolve(false);
    final TrustAnchorIntermediateModule taIm = new TrustAnchorIntermediateModule();
    taIm.setTaImId(UUID.randomUUID());

    final Subordinate entity = DtoToSubordinateMapper.toEntity(ID, dto, taIm);

    assertThat(entity.getSubordinateId()).isEqualTo(ID);
    assertThat(entity.getTaIm()).isEqualTo(taIm);
    assertThat(entity.getJwks()).isEqualTo(dto.getJwks());
    assertThat(entity.getEntityidentifier()).isEqualTo("https://subordinate.example.com");
    assertThat(entity.getCrit()).isEqualTo(List.of("crit1", "crit2"));
    assertThat(entity.getMetadataPolicyCrit()).isEqualTo(List.of("mpc1", "mpc2"));
    assertThat(entity.getEcLocation()).isEqualTo("https://ec.example.com");
    assertThat(entity.isEcLocationAutomatic()).isFalse();
  }

  @Test
  void toEntity_subordinateDto_withNullCrit() {
    final SubordinateDto dto = new SubordinateDto();
    dto.setJwks(TestDataOperations.genJWKS().toJSONObject());
    dto.setEntityIdentifier("https://subordinate.example.com");
    dto.setCrit(null);
    dto.setMetadataPolicyCrit(null);
    final TrustAnchorIntermediateModule taIm = new TrustAnchorIntermediateModule();
    taIm.setTaImId(UUID.randomUUID());

    final Subordinate entity = DtoToSubordinateMapper.toEntity(ID, dto, taIm);

    assertThat(entity.getCrit()).isNull();
    assertThat(entity.getMetadataPolicyCrit()).isNull();
  }

  // -------------------------------------------------------------------------
  // updateEntity — FederationEntityDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_federationEntityDto() {
    final FederationEntity entity = new FederationEntity();
    entity.setIssuer("old-issuer");
    final FederationEntityDto dto = new FederationEntityDto();
    dto.setEntityIdentifier("https://new.example.com");
    dto.setCrit(List.of("new-crit"));
    dto.setAuthorityhints(List.of("https://new-ta.example.com"));

    DtoToEntityMapper.updateEntity(entity, dto);

    assertThat(entity.getIssuer()).isEqualTo("https://new.example.com");
    assertThat(entity.getSubject()).isEqualTo("https://new.example.com");
    assertThat(entity.getCrit()).containsExactly("new-crit");
    assertThat(entity.getAuthorityhints()).containsExactly("https://new-ta.example.com");
  }

  // -------------------------------------------------------------------------
  // updateEntity — HostedEntityDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_hostedEntityDto() {
    final FederationEntity entity = new FederationEntity();
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityIdentifier("https://updated.example.com");
    dto.setMetadata(Map.of("key", "updated"));
    final TrustmarkSourceDto source = new TrustmarkSourceDto();
    source.setTrustMarkIssuer("issuer1");
    source.setTrustmarkId("tm1");
    dto.setTrustMarkSources(List.of(source));

    DtoToEntityMapper.updateEntity(entity, dto);

    assertThat(entity.getIssuer()).isEqualTo("https://updated.example.com");
    assertThat(entity.getSubject()).isEqualTo("https://updated.example.com");
    assertThat(entity.getMetadata()).containsEntry("key", "updated");
    assertThat(entity.getTrustmarksources()).contains("issuer1");
  }

  @Test
  void updateEntity_hostedEntityDto_withNullTrustMarkSources() {
    final FederationEntity entity = new FederationEntity();
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityIdentifier("https://example.com");
    dto.setTrustMarkSources(null);

    DtoToEntityMapper.updateEntity(entity, dto);

    assertThat(entity.getTrustmarksources()).isNull();
  }



  // -------------------------------------------------------------------------
  // updateIntermediate — TrustAnchorDto
  // -------------------------------------------------------------------------

  @Test
  void updateIntermediate_trustAnchorDto() {
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setActive(false);
    final TrustAnchorDto dto = new TrustAnchorDto();
    dto.setActive(true);
    dto.setTrustMarkIssuers(List.of("issuer1", "issuer2"));

    DtoToModuleMapper.updateIntermediate(module, dto);

    assertThat(module.getActive()).isTrue();
    assertThat(module.getTrustMarkIssuers()).containsExactly("issuer1", "issuer2");
  }

  // -------------------------------------------------------------------------
  // updateIntermediate — IntermediateDto
  // -------------------------------------------------------------------------

  @Test
  void updateIntermediate_intermediateDto() {
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setActive(false);
    final IntermediateDto dto = new IntermediateDto();
    dto.setActive(true);

    DtoToModuleMapper.updateIntermediate(module, dto);

    assertThat(module.getActive()).isTrue();
  }

  // -------------------------------------------------------------------------
  // updateEntity — ResolverDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_resolverDto() {
    final Resolver entity = Resolver.builder().build();
    final ResolverDto dto = new ResolverDto();
    dto.setActive(true);
    dto.setResolveResponseDuration("PT60S");
    dto.setTrustAnchor("https://new-ta.example.com");
    dto.setTrustedKeys(TestDataOperations.genJWKS().toJSONObject());

    dto.setStepRetryDuration("PT5S");

    DtoToModuleMapper.updateEntity(entity, dto);

    assertThat(entity.getActive()).isTrue();
    assertThat(entity.getResolveResponseDuration()).isEqualTo("PT60S");
    assertThat(entity.getTrustAnchor()).isEqualTo("https://new-ta.example.com");
    assertThat(entity.getTrustedKeys()).isEqualTo(dto.getTrustedKeys());
    assertThat(entity.getStepRetryDuration()).isEqualTo("PT5S");
  }

  // -------------------------------------------------------------------------
  // updateEntity — TrustmarkDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_trustmarkDto() {
    final TrustMark entity = TrustMark.builder().build();
    final TrustmarkDto dto = new TrustmarkDto();
    dto.setTrustmarkType("https://example.com/updated-type");
    dto.setLogoUri("https://example.com/new-logo.png");
    dto.setRefUri("https://example.com/new-ref");
    dto.setDelegation("new-delegation");

    DtoToTrustmarkMapper.updateEntity(entity, dto);

    assertThat(entity.getTrustmarkType()).isEqualTo("https://example.com/updated-type");
    assertThat(entity.getLogoUri()).isEqualTo("https://example.com/new-logo.png");
    assertThat(entity.getRefUri()).isEqualTo("https://example.com/new-ref");
    assertThat(entity.getDelegation()).isEqualTo("new-delegation");
  }

  // -------------------------------------------------------------------------
  // updateEntity — TrustmarkSubjectDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_trustmarkSubjectDto() {
    final TrustMarkSubject entity = TrustMarkSubject.builder().build();
    final OffsetDateTime granted = OffsetDateTime.now();
    final OffsetDateTime expires = OffsetDateTime.now().plusYears(1);
    final TrustmarkSubjectDto dto = new TrustmarkSubjectDto();
    dto.setSubject("https://updated-subject.example.com");
    dto.setRevoked(true);
    dto.setGranted(granted);
    dto.setExpires(expires);

    DtoToTrustmarkMapper.updateEntity(entity, dto);

    assertThat(entity.getSubject()).isEqualTo("https://updated-subject.example.com");
    assertThat(entity.getRevoked()).isTrue();
    assertThat(entity.getGranted()).isEqualTo(granted);
    assertThat(entity.getExpires()).isEqualTo(expires);
  }

  // -------------------------------------------------------------------------
  // updateEntity — TrustmarkIssuerDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_trustmarkIssuerDto() {
    final TrustMarkIssuer entity = TrustMarkIssuer.builder().build();
    final TrustmarkIssuerDto dto = new TrustmarkIssuerDto();
    dto.setActive(false);
    dto.setTrustMarkTokenValidityDuration("PT2H");

    DtoToModuleMapper.updateEntity(entity, dto);

    assertThat(entity.getActive()).isFalse();
    assertThat(entity.getTrustMarkTokenValidityDuration()).isEqualTo("PT2H");
  }

  // -------------------------------------------------------------------------
  // updateEntity — SubordinateDto
  // -------------------------------------------------------------------------

  @Test
  void updateEntity_subordinateDto() {
    final Subordinate entity = new Subordinate();
    final SubordinateDto dto = new SubordinateDto();

    dto.setJwks(TestDataOperations.genJWKS().toJSONObject());

    dto.setEntityIdentifier("https://updated-sub.example.com");
    dto.setCrit(List.of("new-crit"));
    dto.setMetadataPolicyCrit(List.of("new-mpc"));
    dto.setEcLocation("https://new-ec.example.com");
    dto.setEcLocationAutomaticResolve(true);

    DtoToSubordinateMapper.updateEntity(entity, dto);

    assertThat(entity.getJwks()).isEqualTo(dto.getJwks());
    assertThat(entity.getEntityidentifier()).isEqualTo("https://updated-sub.example.com");
    assertThat(entity.getCrit()).isEqualTo(List.of("new-crit"));
    assertThat(entity.getMetadataPolicyCrit()).isEqualTo(List.of("new-mpc"));
    assertThat(entity.getEcLocation()).isEqualTo("https://new-ec.example.com");
    assertThat(entity.isEcLocationAutomatic()).isTrue();
  }

  @Test
  void updateEntity_subordinateDto_clearsNullCrit() {
    final Subordinate entity = new Subordinate();
    entity.setCrit(List.of("old-crit"));
    entity.setMetadataPolicyCrit(List.of("old-mpc"));
    final SubordinateDto dto = new SubordinateDto();
    dto.setJwks(TestDataOperations.genJWKS().toJSONObject());
    dto.setEntityIdentifier("https://sub.example.com");
    dto.setCrit(null);
    dto.setMetadataPolicyCrit(null);

    DtoToSubordinateMapper.updateEntity(entity, dto);

    assertThat(entity.getCrit()).isEmpty();
    assertThat(entity.getMetadataPolicyCrit()).isEmpty();
  }

  @Test
  void updateEntity_subordinateDto_clearsEmptyCrit() {
    final Subordinate entity = new Subordinate();
    entity.setCrit(List.of("old-crit"));
    entity.setMetadataPolicyCrit(List.of("old-mpc"));
    final SubordinateDto dto = new SubordinateDto();
    dto.setJwks(TestDataOperations.genJWKS().toJSONObject());
    dto.setEntityIdentifier("https://sub.example.com");
    dto.setCrit(Collections.emptyList());
    dto.setMetadataPolicyCrit(Collections.emptyList());

    DtoToSubordinateMapper.updateEntity(entity, dto);

    assertThat(entity.getCrit()).isEmpty();
    assertThat(entity.getMetadataPolicyCrit()).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Test helpers
  // -------------------------------------------------------------------------

  private static Organization createOrganization() {
    final Organization org = new Organization();
    org.setOrganizationId(UUID.randomUUID());
    org.setOrgNumber("555555-5555");
    org.setOrgName("Test Organization");
    return org;
  }

  private static FederationEntity createEntityEntity() {
    final FederationEntity entity = new FederationEntity();
    entity.setEntityId(UUID.randomUUID());
    entity.setEntityType(EntityType.FEDERATION_ENTITY);
    entity.setIssuer("https://entity.example.com");
    entity.setSubject("https://entity.example.com");
    return entity;
  }
}
