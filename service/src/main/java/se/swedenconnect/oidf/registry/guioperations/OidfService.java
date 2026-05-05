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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.guioperations.dto.JwksLoadedDto;
import se.swedenconnect.oidf.registry.infrastructure.validation.CleanInput;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Service that makes OIDF service operation calls.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OidfService {
  final OidfServiceIntegration oidfServiceIntegration;
  final EntityConfigService entityConfigService;

  /**
   * Constructor.
   *
   * @param oidfServiceIntegration OIDF service integration
   * @param entityConfigService entity config service
   */
  public OidfService(final OidfServiceIntegration oidfServiceIntegration,
      final EntityConfigService entityConfigService) {
    this.oidfServiceIntegration = oidfServiceIntegration;
    this.entityConfigService = entityConfigService;
  }

  /**
   * Resolve entitystatement from database, if there is a ec_location on that entity it will be used. If there is not
   * loading entitystatment from standard location will be done.
   *
   * If multable entries is found they will be returned.
   *
   * @param entityID the entity ID to resolve
   * @return List of jwks
   */
  public List<JwksLoadedDto> resolveEntityStatement(final EntityID entityID) {
    final List<JwksLoadedDto> hostedJwks = this.loadFromHostedEntity(entityID);
    if (hostedJwks.isEmpty()) {
      return List.of(this.loadFromStandardLocation(entityID));
    }
    return hostedJwks;
  }

  private List<JwksLoadedDto> loadFromHostedEntity(final EntityID entityID) {
    final List<HostedEntityDto> hostedEntityDtos = this.entityConfigService.listHostedEntity(entityID.toString());
    return hostedEntityDtos.stream()
        .filter(hostedEntityDto -> hostedEntityDto.getEffectiveEcLocation() != null)
        .map(dto -> {

          final String effectiveEcLocation = dto.getEffectiveEcLocation();
          final EntityStatement entityStatement =
              this.oidfServiceIntegration.callEntityStatement(URI.create(effectiveEcLocation));
          final JwksLoadedDto jwksLoadedDto = new JwksLoadedDto();
          jwksLoadedDto.setEntityId(entityID.toString());
          jwksLoadedDto.setJwks(this.removeExpIatNbf(entityStatement));
          jwksLoadedDto.setEcLocation(effectiveEcLocation);
          return jwksLoadedDto;
        }).toList();
  }

  private JwksLoadedDto loadFromStandardLocation(final EntityID entityID) {
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(entityID.toURI())
        .path("/.well-known/openid-federation");
    final URI uri = uriBuilder.build().toUri();
    final EntityStatement entityStatement = this.oidfServiceIntegration.callEntityStatement(uri);
    final Map<String, Object> jwks = this.removeExpIatNbf(entityStatement);
    final JwksLoadedDto jwksLoadedDto = new JwksLoadedDto();
    jwksLoadedDto.setEntityId(entityID.toString());
    jwksLoadedDto.setJwks(jwks);
    jwksLoadedDto.setEcLocation(uri.toString());
    return jwksLoadedDto;
  }

  private Map<String, Object> removeExpIatNbf(final EntityStatement entityStatement) {
    final Map<String, Object> jwks = entityStatement.getClaimsSet().getJWKSet().toJSONObject();
    return CleanInput.removeExpIatNbfFromJwks(jwks);
  }


}
