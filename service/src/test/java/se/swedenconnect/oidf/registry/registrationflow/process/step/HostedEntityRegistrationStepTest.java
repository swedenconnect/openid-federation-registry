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

package se.swedenconnect.oidf.registry.registrationflow.process.step;

import com.nimbusds.jose.jwk.JWKSet;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.guioperations.OidfServiceIntegration;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.HostedEntityRegistrationStep;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSourceDto;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostedEntityRegistrationStep}.
 *
 * @author Per Fredrik Plars
 */
@ExtendWith(MockitoExtension.class)
class HostedEntityRegistrationStepTest {

  private static final String ENTITY_ID = "https://www.polisen.se/op/sverigeid";
  private static final String ISSUER_ID = "https://www.pm.se/oidf/tmi";
  private static final String TRUST_MARK_TYPE = "https://id.swedenconnect.se/tm-prop/public-body";
  private static final UUID TAIM_ID = UUID.randomUUID();
  private static final UUID HOSTED_ENTITY_ID = UUID.randomUUID();

  @Mock
  private EntityConfigService entityConfigService;
  @Mock
  private TaImRepository taImRepository;
  @Mock
  private InstancePlacementService instancePlacementService;
  @Mock
  private OidfServiceIntegration oidfServiceIntegration;
  @Mock
  private TrustMarkRepository trustMarkRepository;

  private HostedEntityRegistrationStep step;

  @BeforeEach
  void setUp() {
    this.step = new HostedEntityRegistrationStep(this.entityConfigService, this.taImRepository,
        this.instancePlacementService, this.oidfServiceIntegration, this.trustMarkRepository);
  }

  @Test
  @DisplayName("A registration that requests no trust marks keeps the trust mark sources of the hosted entity")
  void updateWithoutRequestedTrustMarksKeepsTheTrustMarkSources() {
    final HostedEntityDto stored = this.storedHostedEntity();
    when(this.entityConfigService.listHostedEntity(any(OrganizationRecord.class), eq(ENTITY_ID)))
        .thenReturn(List.of(stored));
    this.taImOfOrganization();

    this.step.execute(this.context(), null);

    final ArgumentCaptor<HostedEntityDto> written = ArgumentCaptor.forClass(HostedEntityDto.class);
    verify(this.entityConfigService).updateHostedEntity(any(OrganizationRecord.class), eq(HOSTED_ENTITY_ID),
        written.capture());
    assertThat(written.getValue().getTrustMarkSources())
        .extracting(TrustmarkSourceDto::getTrustMarkIssuer, TrustmarkSourceDto::getTrustmarkId)
        .containsExactly(org.assertj.core.groups.Tuple.tuple(ISSUER_ID, TRUST_MARK_TYPE));
  }

  /** The step has the keys it needs and the federation JWKS already, so no service node is called. */
  private ProcessContext context() {
    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.ENTITY_ID, ENTITY_ID);
    ctx.put(ContextKey.TAIM_ID, TAIM_ID);
    ctx.put(ContextKey.ORG, new OrganizationRecord("5520001263", "Polisen", "https://www.pm.se/oidf", "swedenconnect"));
    ctx.put(ContextKey.REQUEST_METADATA, new JSONObject(Map.of("openid_relying_party", Map.of())));
    ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, new JWKSet());
    return ctx;
  }

  /** What is hosted now: an entity that holds one trust mark, granted by an earlier registration. */
  private HostedEntityDto storedHostedEntity() {
    final TrustmarkSourceDto source = new TrustmarkSourceDto();
    source.setTrustMarkIssuer(ISSUER_ID);
    source.setTrustmarkId(TRUST_MARK_TYPE);

    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityId(HOSTED_ENTITY_ID);
    dto.setEntityIdentifier(ENTITY_ID);
    dto.setMetadata(Map.of("openid_relying_party", Map.of()));
    dto.setTrustMarkSources(new ArrayList<>(List.of(source)));
    return dto;
  }

  private void taImOfOrganization() {
    final Instance instance = new Instance();
    instance.setInstanceId(UUID.randomUUID());
    final Organization organization = new Organization();
    organization.setOrganizationId(UUID.randomUUID());
    organization.setOrgNumber("5520001263");
    organization.setInstance(instance);

    final FederationEntity entity = new FederationEntity();
    entity.setOrganization(organization);
    final TrustAnchorIntermediateModule taIm = new TrustAnchorIntermediateModule();
    taIm.setOrganization(organization);
    taIm.setEntity(entity);

    when(this.taImRepository.findById(TAIM_ID)).thenReturn(Optional.of(taIm));
    when(this.instancePlacementService.resolveEntityPrefixForPlacedOrg(organization))
        .thenReturn(Optional.of("https://www.pm.se/oidf"));
  }

}
