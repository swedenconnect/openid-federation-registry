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
package se.swedenconnect.oidf.registry.guioperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;

import java.net.URI;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OidfServiceTest {

  private static final String ENTITY_ID = "https://www.polisen.se/op/sverigeid";
  private static final String EC_LOCATION = "https://www.pm.se/oidf/www_polisen_se_op_sverigeid";
  private static final URI HOSTED_URI =
      URI.create(EC_LOCATION + "/.well-known/openid-federation");

  @Mock
  private OidfServiceIntegration integration;
  @Mock
  private EntityConfigService entityConfigService;

  private OidfService service;
  private EntityStatement statement;

  @BeforeEach
  void setUp() {
    this.service = new OidfService(this.integration, this.entityConfigService);
    this.statement = mock(EntityStatement.class);
  }

  @Test
  @DisplayName("Hosted entity with ec_location - entity statement is loaded from the ec_location")
  void hostedEntityWithEcLocationUsesEcLocation() {
    final HostedEntityDto hosted = new HostedEntityDto();
    hosted.setEffectiveEcLocation(EC_LOCATION);
    when(this.entityConfigService.listHostedEntity(ENTITY_ID)).thenReturn(List.of(hosted));
    when(this.integration.callEntityStatementAndVerifyJwks(HOSTED_URI)).thenReturn(this.statement);

    assertThat(this.service.loadEntityStatement(new EntityID(ENTITY_ID))).isSameAs(this.statement);

    verify(this.integration, never()).entityConfigurationOnStandardLocation(new EntityID(ENTITY_ID));
  }

  @Test
  @DisplayName("Ec_location with trailing slash - no double slash in the loaded URI")
  void trailingSlashInEcLocation() {
    final HostedEntityDto hosted = new HostedEntityDto();
    hosted.setEffectiveEcLocation(EC_LOCATION + "/");
    when(this.entityConfigService.listHostedEntity(ENTITY_ID)).thenReturn(List.of(hosted));
    when(this.integration.callEntityStatementAndVerifyJwks(HOSTED_URI)).thenReturn(this.statement);

    assertThat(this.service.loadEntityStatement(new EntityID(ENTITY_ID))).isSameAs(this.statement);
  }

  @Test
  @DisplayName("Hosted entity without ec_location - standard location is used")
  void hostedEntityWithoutEcLocationUsesStandardLocation() {
    when(this.entityConfigService.listHostedEntity(ENTITY_ID)).thenReturn(List.of(new HostedEntityDto()));
    when(this.integration.entityConfigurationOnStandardLocation(new EntityID(ENTITY_ID))).thenReturn(this.statement);

    assertThat(this.service.loadEntityStatement(new EntityID(ENTITY_ID))).isSameAs(this.statement);
  }

  @Test
  @DisplayName("Entity that is not hosted - standard location is used")
  void notHostedUsesStandardLocation() {
    when(this.entityConfigService.listHostedEntity(ENTITY_ID)).thenReturn(List.of());
    when(this.integration.entityConfigurationOnStandardLocation(new EntityID(ENTITY_ID))).thenReturn(this.statement);

    assertThat(this.service.loadEntityStatement(new EntityID(ENTITY_ID))).isSameAs(this.statement);
  }
}
