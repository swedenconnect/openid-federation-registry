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
package se.swedenconnect.oidf.registry.guioperations.controller;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.guioperations.JwksKeysCacheService;
import se.swedenconnect.oidf.registry.guioperations.OidfService;
import se.swedenconnect.oidf.registry.guioperations.OidfServiceIntegration;
import se.swedenconnect.oidf.registry.guioperations.dto.EntityConfigurationViewDto;
import se.swedenconnect.oidf.registry.guioperations.dto.JwksLoadedDto;
import se.swedenconnect.oidf.registry.guioperations.dto.JwksPayloadDto;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;

import java.util.List;

/**
 * Controller that handle operations on the entitystatmend for a specific entityid
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/registry/v1/entityconfiguration")
@Tag(name = "EntityConfigurationController", description = "Loading entity configurations")
public class EntityConfigurationController {

  final OidfServiceIntegration oidfServiceIntegration;
  final OidfService oidfService;
  final JwksKeysCacheService jwksKeysCacheService;

  /**
   * Constructor
   *
   * @param oidfServiceIntegration OIDF integration
   * @param oidfService OIDF service
   * @param jwksKeysCacheService cache service for signing keys
   */
  public EntityConfigurationController(final OidfServiceIntegration oidfServiceIntegration,
      final OidfService oidfService,
      final JwksKeysCacheService jwksKeysCacheService) {
    this.oidfServiceIntegration = oidfServiceIntegration;
    this.oidfService = oidfService;
    this.jwksKeysCacheService = jwksKeysCacheService;
  }

  /**
   * Returns available signing keys for the given entity type. Federation entities receive keys from the
   * {@code federation} JWKS claim; hosted entities receive keys from the {@code hosted} claim.
   *
   * @param type               FEDERATION_ENTITY or HOSTED_ENTITY
   * @param organizationRecord the authenticated organisation used to resolve the correct instance
   * @return list of available signing keys
   */
  /**
   * Returns the available signing key names for the given entity type, sourced from the {@code name} claim of the
   * oidf-service {@code /jwks} JWT.
   *
   * @param type FEDERATION_ENTITY or HOSTED_ENTITY
   * @param organizationRecord the authenticated organisation used to resolve the correct instance
   * @return ordered list of signing key names
   */
  @GetMapping(path = "/signing-keys")
  public List<String> listSigningKeys(
      @RequestParam("type") final EntityType type,
      @Parameter(hidden = true) final OrganizationRecord organizationRecord) {
    return this.jwksKeysCacheService.getPayload(organizationRecord)
        .map(payload -> this.namesForType(type, payload))
        .orElse(List.of());
  }

  private List<String> namesForType(final EntityType type, final JwksPayloadDto payload) {
    return switch (type) {
      case FEDERATION_ENTITY -> payload.names().federation();
      case HOSTED_ENTITY -> payload.names().hosted();
      default -> List.of();
    };
  }

  /**
   * Endpoint to load JWKS from entityconfiguration
   *
   * @param entityId EntityId that will be used to resolve JWKS
   * @return JWKS
   */
  @PostMapping(path = "/jwks")
  public List<JwksLoadedDto> loadJwksFromEntityConfiguration(
      @RequestBody final String entityId) {
    log.debug("Start loading jwks from entitystatement {}", entityId);
    try {
      final List<JwksLoadedDto> jwks = this.oidfService.loadJwks(EntityID.parse(entityId));

      if (jwks.isEmpty()) {
        throw new IllegalArgumentException("There is no jwks for entity id " + entityId);
      }

      return jwks;
    }
    catch (final ParseException e) {
      throw new IllegalArgumentException("Invalid entity ID: " + entityId, e);

    }
    catch (final HttpClientErrorException | HttpServerErrorException e) {
      log.info("Error loading jwks from entitystatement {}", entityId, e);
      throw new IllegalArgumentException("Unable to get entity configuration, "
          + "target server gave httpStatus:" + e.getStatusCode(), e);

    }
    catch (final SecurityException | ResourceAccessException e) {
      log.info("Error loading jwks from entitystatement {}", entityId);
      throw new IllegalArgumentException("Unable to get entity configuration.", e);
    }

  }

  /**
   * Fetches an entity configuration and returns its decoded header and payload as JSON.
   * The JWT signature is intentionally omitted.
   *
   * @param entityId EntityId whose entity configuration will be fetched
   * @return decoded header and payload, or 400 on error
   */
  @PostMapping(path = "/view")
  public ResponseEntity<EntityConfigurationViewDto> viewEntityConfiguration(
      @RequestBody final String entityId) {
    log.debug("Fetching entity configuration for view: {}", entityId);
    try {
      final EntityStatement statement =
          this.oidfServiceIntegration.entityConfigurationOnStandardLocation(EntityID.parse(entityId));
      final com.nimbusds.jwt.SignedJWT signedJWT = statement.getSignedStatement();

      final EntityConfigurationViewDto dto = new EntityConfigurationViewDto();
      dto.setHeader(signedJWT.getHeader().toJSONObject());
      dto.setPayload(signedJWT.getPayload().toJSONObject());
      return ResponseEntity.ok(dto);
    }
    catch (final com.nimbusds.oauth2.sdk.ParseException e) {
      log.info("Invalid entity ID for view: {}", entityId);
      return ResponseEntity.badRequest().build();
    }
    catch (final HttpClientErrorException | HttpServerErrorException e) {
      log.info("HTTP error fetching entity configuration for view {}: {}", entityId, e.getStatusCode());
      return ResponseEntity.badRequest().build();
    }
    catch (final SecurityException | ResourceAccessException | IllegalArgumentException | IllegalStateException e) {
      log.info("Error fetching entity configuration for view {}", entityId, e);
      return ResponseEntity.badRequest().build();
    }
  }

}
