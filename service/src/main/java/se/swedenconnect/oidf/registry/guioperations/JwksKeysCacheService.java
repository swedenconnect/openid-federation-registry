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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.guioperations.dto.JwksPayloadDto;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance cache for signing keys fetched from the oidf-service {@code /jwks} endpoint. Keys are cached per
 * instance base URL. Each call attempts a fresh fetch; on failure the cached result for that instance is returned.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class JwksKeysCacheService {

  private final OidfServiceIntegration oidfServiceIntegration;
  private final InstancePlacementService instancePlacementService;
  private final Map<URI, JwksPayloadDto> cache = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   * @param oidfServiceIntegration integration for fetching JWKS from oidf-service
   * @param instancePlacementService used to resolve which instance an organisation belongs to
   */
  public JwksKeysCacheService(final OidfServiceIntegration oidfServiceIntegration,
      final InstancePlacementService instancePlacementService) {
    this.oidfServiceIntegration = oidfServiceIntegration;
    this.instancePlacementService = instancePlacementService;
  }

  /**
   * Returns the full {@link JwksPayloadDto} for the instance that {@code organizationRecord} belongs to,
   * including federation keys, hosted keys, and key name aliases.
   *
   * @param organizationRecord the organisation whose instance to query
   * @return the payload DTO, or empty if the instance cannot be resolved or the cache is cold after a failure
   */
  public Optional<JwksPayloadDto> getPayload(final OrganizationRecord organizationRecord) {
    return Optional.ofNullable(this.refresh(organizationRecord));
  }

  /**
   * Returns the federation signing keys for the instance that {@code organizationRecord} belongs to.
   *
   * @param organizationRecord the organisation whose instance to query
   * @return list of federation signing keys, empty if the instance cannot be resolved or fetching fails
   */
  public List<JWK> getFederationKeys(final OrganizationRecord organizationRecord) {
    final JwksPayloadDto payload = this.refresh(organizationRecord);
    if (payload == null) {
      return List.of();
    }
    final JWKSet jwkSet = payload.federation();
    return jwkSet != null ? jwkSet.getKeys() : List.of();
  }

  /**
   * Returns the hosted signing keys for the instance that {@code organizationRecord} belongs to.
   *
   * @param organizationRecord the organisation whose instance to query
   * @return list of hosted signing keys, empty if the instance cannot be resolved or fetching fails
   */
  public List<JWK> getHostedKeys(final OrganizationRecord organizationRecord) {
    final JwksPayloadDto payload = this.refresh(organizationRecord);
    if (payload == null) {
      return List.of();
    }
    final JWKSet jwkSet = payload.hosted();
    return jwkSet != null ? jwkSet.getKeys() : List.of();
  }

  private JwksPayloadDto refresh(final OrganizationRecord organizationRecord) {
    final Optional<URI> baseUrl = this.instancePlacementService.resolveBaseUrl(organizationRecord);
    if (baseUrl.isEmpty()) {
      log.warn("No instance found for org '{}', skipping signing key fetch",
          organizationRecord.orgNumber());
      return null;
    }
    try {
      final JwksPayloadDto fresh =
          this.oidfServiceIntegration.fetchServiceKeys(EntityID.parse(baseUrl.get().toString()));
      this.cache.put(baseUrl.get(), fresh);
    }
    catch (final Exception e) {
      log.error("Failed to fetch JWKS from {} for org '{}', returning cached result",
          baseUrl.get(), organizationRecord.orgNumber(), e);
    }
    return this.cache.get(baseUrl.get());
  }
}