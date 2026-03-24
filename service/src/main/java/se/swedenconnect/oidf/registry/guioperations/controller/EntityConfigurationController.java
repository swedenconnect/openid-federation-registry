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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import se.swedenconnect.oidf.registry.guioperations.OidfServiceIntegration;
import se.swedenconnect.oidf.registry.guioperations.dto.EntityConfigurationPingDto;

import java.util.Map;

/**
 * Controller that handle operations on the entitystatmend for a specific entityid
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/registry/v1/entityconfiguration")
public class EntityConfigurationController {

  final OidfServiceIntegration oidfServiceIntegration;

  /**
   * Constructor
   *
   * @param oidfServiceIntegration OIDF integration
   */
  public EntityConfigurationController(final OidfServiceIntegration oidfServiceIntegration) {
    this.oidfServiceIntegration = oidfServiceIntegration;
  }

  /**
   * Endpoint to load JWKS from entityconfiguration
   *
   * @param entityId EntityId that will be used to resolve JWKS
   * @return JWKS
   */
  @PostMapping(path = "/jwks")
  public Map<String, Object> loadJwksFromEntityConfiguration(
      @RequestBody final String entityId) {
    log.debug("Start loading jwks from entitystatement {}", entityId);
    try {
      final EntityStatement entityConfiguration =
          this.oidfServiceIntegration.entityConfiguration(EntityID.parse(entityId));
      final EntityStatementClaimsSet claimsSet = entityConfiguration.getClaimsSet();
      if (claimsSet == null) {
        throw new IllegalArgumentException("Entity configuration is missing claims");
      }

      if (claimsSet.getJWKSet() == null) {
        throw new IllegalArgumentException("Entity configuration is missing jwks");
      }
      return claimsSet.getJWKSet().toJSONObject();
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
   * Endpoint that controls if entityconfiguration is accessible
   *
   * @param entityId EntityId that will be used to resolve JWKS
   * @return EntityConfigurationPingDto That contains OK or an error description
   */
  @PostMapping(path = "/ping")
  public EntityConfigurationPingDto pingEntityConfiguration(
      @RequestBody final String entityId) {
    log.debug("Start loading entity configuration for entityid: {}", entityId);
    final EntityConfigurationPingDto response = new EntityConfigurationPingDto();
    try {
      final EntityStatement entityConfiguration =
          this.oidfServiceIntegration.entityConfiguration(EntityID.parse(entityId));
      final EntityStatementClaimsSet claimsSet = entityConfiguration.getClaimsSet();
      if (claimsSet == null) {
        throw new IllegalArgumentException("Entity configuration is missing claims");
      }
      response.setEntityConfigurationAccessible(true);
      return response;
    }
    catch (final ParseException e) {
      response.setErrorMessage("Invalid entity ID: " + entityId);
      response.setEntityConfigurationAccessible(false);
      return response;

    }
    catch (final HttpClientErrorException | HttpServerErrorException e) {
      log.info("Error loading jwks from entitystatement {}", entityId, e);

      response.setErrorMessage("Unable to get entity configuration, "
          + "target server gave httpStatus:" + e.getStatusCode());
      response.setEntityConfigurationAccessible(false);
      return response;
    }
    catch (final SecurityException | ResourceAccessException e) {
      log.info("Error loading entity configuration {}", entityId, e);
      response.setErrorMessage("Unable to get entity configuration.");
      response.setEntityConfigurationAccessible(false);
      return response;
    }

  }

}
