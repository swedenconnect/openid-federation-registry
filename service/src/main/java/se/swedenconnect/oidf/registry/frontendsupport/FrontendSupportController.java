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
package se.swedenconnect.oidf.registry.frontendsupport;

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

import java.util.Map;

/**
 * Controller that supports frontend with different operations
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/admin/support")
public class FrontendSupportController {

  final OidfServiceIntegration oidfServiceIntegration;

  /**
   * Constructor
   *
   * @param oidfServiceIntegration OIDF integration
   */
  public FrontendSupportController(final OidfServiceIntegration oidfServiceIntegration) {
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
}
