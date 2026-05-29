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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
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
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OidfApiService#toSubordinates(Subordinate)}.
 *
 * @author Per Fredrik Plars
 */
@ExtendWith(MockitoExtension.class)
class OidfApiServiceToSubordinatesTest {

  @Mock
  private EntityRepository entityRepository;

  @Mock
  private SubordinateRepository subordinateRepository;

  @Mock
  private InstanceRepository instanceRepository;

  private OidfApiService service;

  private TrustAnchorIntermediateModule taIm;
  private Organization organization;

  @BeforeEach
  void setUp() throws Exception {
    final JWK signKey = TestDataOperations.genKey();
    this.service = new OidfApiService(
        signKey,
        this.subordinateRepository,
        this.entityRepository,
        "https://issuer.example.com",
        this.instanceRepository,
        Duration.ofHours(1)
    );

    this.organization = new Organization();
    this.organization.setOrgNumber("SE0123456789");

    this.taIm = new TrustAnchorIntermediateModule();
    this.taIm.setOrganization(this.organization);
  }

  @Test
  void mapsEntityIdentifierCritAndEcLocation() {
    final Subordinate sub = subordinate("https://entity.example.com", false);
    sub.setCrit(List.of("claim1", "claim2"));
    sub.setMetadataPolicyCrit(List.of("op1"));

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getEntityIdentifier().getValue()).isEqualTo("https://entity.example.com");
    assertThat(result.getCrit()).containsExactly("claim1", "claim2");
    assertThat(result.getMetadataPolicyCrit()).containsExactly("op1");
    assertThat(result.getOverrideConfigurationLocation()).isNull();
  }

  @Test
  void nullCritDefaultsToEmptyList() {
    final Subordinate sub = subordinate("https://entity.example.com", false);
    sub.setCrit(null);

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getCrit()).isNotNull().isEmpty();
  }

  @Test
  void metadataPolicyUnwrapsInnerMapWhenWrapped() {
    final Subordinate sub = subordinate("https://entity.example.com", false);
    final Map<String, Object> inner = Map.of("subject_types_supported", Map.of("value", List.of("pairwise")));
    final Map<String, Object> wrapped = new HashMap<>();
    wrapped.put("metadata_policy", inner);
    sub.setMetadataPolicy(wrapped);

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getPolicy().getPolicy()).isEqualTo(inner);
  }

  @Test
  void metadataPolicyUsedDirectlyWhenNotWrapped() {
    final Subordinate sub = subordinate("https://entity.example.com", false);
    final Map<String, Object> policy = Map.of("subject_types_supported", Map.of("value", List.of("pairwise")));
    sub.setMetadataPolicy(policy);

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getPolicy().getPolicy()).isEqualTo(policy);
  }

  @Test
  void policyIdIsSubordinateId() {
    final Subordinate sub = subordinate("https://entity.example.com", false);
    final UUID id = sub.getSubordinateId();

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result.getPolicy().getId()).isEqualTo(id.toString());
  }

  @Test
  void jwksIsParsedFromSubordinate() {
    final JWKSet jwkSet = TestDataOperations.genJWKS();
    final Subordinate sub = subordinate("https://entity.example.com", false);
    sub.setJwks(jwkSet.toJSONObject());

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result.getJwks()).isNotNull();
    assertThat(result.getJwks().getKeys()).hasSize(jwkSet.getKeys().size());
  }

  @Test
  void autoResolve_entityFound_returnsSubordinate() {
    final Subordinate sub = subordinate("https://entity.example.com", true);

    final FederationEntity hosted = new FederationEntity();
    hosted.setEntityType(EntityType.HOSTED_ENTITY);
    hosted.setIssuer("https://entity.example.com");
    hosted.setSubject("https://entity.example.com");

    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(
        eq(EntityType.HOSTED_ENTITY), eq("https://entity.example.com")))
        .thenReturn(List.of(hosted));

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getOverrideConfigurationLocation()).isNull();
  }

  @Test
  void explicitEcLocation_isSetAsVirtualEntityId() {
    final Subordinate sub = subordinate("https://entity.example.com", false);
    sub.setEcLocation("https://registry.example.com/entity-config");

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getVirtualEntityId()).isEqualTo("https://registry.example.com/entity-config");
  }

  @Test
  void autoResolve_entityFoundWithEffectiveEcLocation_setsVirtualEntityId() {
    final Subordinate sub = subordinate("https://entity.example.com", true);

    final FederationEntity hosted = new FederationEntity();
    hosted.setEntityType(EntityType.HOSTED_ENTITY);
    hosted.setIssuer("https://entity.example.com");
    hosted.setSubject("https://registry.example.com");

    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(
        eq(EntityType.HOSTED_ENTITY), eq("https://entity.example.com")))
        .thenReturn(List.of(hosted));

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNotNull();
    assertThat(result.getVirtualEntityId()).isNotNull();
  }

  @Test
  void autoResolve_entityNotFound_returnsNull() {
    final Subordinate sub = subordinate("https://entity.example.com", true);

    when(this.entityRepository.findByEntityTypeAndOptionalIssuer(eq(EntityType.HOSTED_ENTITY), any()))
        .thenReturn(List.of());

    final TrustAnchorProperties.SubordinateListingProperty result = this.service.toSubordinates(sub);

    assertThat(result).isNull();
  }

  // -------------------------------------------------------------------------

  private Subordinate subordinate(final String entityIdentifier, final boolean autoResolve) {
    final Subordinate sub = new Subordinate();
    sub.setSubordinateId(UUID.randomUUID());
    sub.setEntityidentifier(entityIdentifier);
    sub.setEcLocationAutomatic(autoResolve);
    sub.setTaIm(this.taIm);
    return sub;
  }
}