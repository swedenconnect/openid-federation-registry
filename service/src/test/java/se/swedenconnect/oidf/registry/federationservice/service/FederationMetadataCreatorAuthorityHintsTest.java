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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the authority hints of an entity record.
 *
 * @author Per Fredrik Plars
 */
@ExtendWith(MockitoExtension.class)
class FederationMetadataCreatorAuthorityHintsTest {

  private static final UUID INSTANCE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final String ENTITY_ID = "https://www.polisen.se/op/sverigeid";
  private static final String INTERMEDIATE_ID = "https://www.pm.se/oidf/im";

  @Mock
  private SubordinateRepository subordinateRepository;

  private FederationMetadataCreator creator;

  private Instance instance;

  @BeforeEach
  void setUp() {
    this.creator = new FederationMetadataCreator(this.subordinateRepository, "hosted");
    this.instance = new Instance();
    this.instance.setInstanceId(INSTANCE_ID);
  }

  @Test
  void superiorInAnotherOrganizationOfTheInstanceIsAnAuthorityHint() {
    final FederationEntity hostedEntity = this.hostedEntity(this.organization());
    final Subordinate subordinate = this.subordinate(this.organization());

    when(this.subordinateRepository.findByInstanceIdAndEntityidentifier(INSTANCE_ID, ENTITY_ID))
        .thenReturn(List.of(subordinate));

    final EntityRecord record = this.creator.createEntityResponse(hostedEntity);

    assertThat(record.getAuthorityHints()).containsExactly(INTERMEDIATE_ID);
  }

  @Test
  void storedHintsFollowTheSuperiorsWithoutDuplicatesOrBlanks() {
    final Organization organization = this.organization();
    final FederationEntity hostedEntity = this.hostedEntity(organization);
    hostedEntity.setAuthorityhints(
        new ArrayList<>(Arrays.asList(INTERMEDIATE_ID, "https://www.pm.se/oidf/ta", null, "  ")));

    when(this.subordinateRepository.findByInstanceIdAndEntityidentifier(INSTANCE_ID, ENTITY_ID))
        .thenReturn(List.of(this.subordinate(organization)));

    final EntityRecord record = this.creator.createEntityResponse(hostedEntity);

    assertThat(record.getAuthorityHints()).containsExactly(INTERMEDIATE_ID, "https://www.pm.se/oidf/ta");
  }

  @Test
  void noSuperiorAndNoStoredHintsGivesNoAuthorityHints() {
    final FederationEntity hostedEntity = this.hostedEntity(this.organization());

    when(this.subordinateRepository.findByInstanceIdAndEntityidentifier(INSTANCE_ID, ENTITY_ID))
        .thenReturn(List.of());

    final EntityRecord record = this.creator.createEntityResponse(hostedEntity);

    assertThat(record.getAuthorityHints()).isNull();
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
    entity.setSubject("https://www.pm.se/oidf");
    entity.setMetadata(Map.of("openid_relying_party", Map.of()));
    entity.setOrganization(organization);
    return entity;
  }

  /** A subordinate under an Intermediate of the given organization, pointing at the hosted entity. */
  private Subordinate subordinate(final Organization organization) {
    final FederationEntity intermediateEntity = new FederationEntity();
    intermediateEntity.setEntityType(EntityType.FEDERATION_ENTITY);
    intermediateEntity.setIssuer(INTERMEDIATE_ID);
    intermediateEntity.setSubject(INTERMEDIATE_ID);
    intermediateEntity.setOrganization(organization);

    final TrustAnchorIntermediateModule intermediate = new TrustAnchorIntermediateModule();
    intermediate.setOrganization(organization);
    intermediate.setEntity(intermediateEntity);

    final Subordinate subordinate = new Subordinate();
    subordinate.setEntityidentifier(ENTITY_ID);
    subordinate.setTaIm(intermediate);
    return subordinate;
  }

}
