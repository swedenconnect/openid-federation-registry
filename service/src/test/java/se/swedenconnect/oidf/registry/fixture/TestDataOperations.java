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

package se.swedenconnect.oidf.registry.fixture;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.registry.ApiClient;
import se.swedenconnect.oidf.registry.api.EntitiesApi;
import se.swedenconnect.oidf.registry.api.ModulesApi;
import se.swedenconnect.oidf.registry.api.PoliciesApi;
import se.swedenconnect.oidf.registry.api.SubordinatesApi;
import se.swedenconnect.oidf.registry.api.model.EntityWithModules;
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.FederationEntityWithModules;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Policy;
import se.swedenconnect.oidf.registry.api.model.Resolver;
import se.swedenconnect.oidf.registry.api.model.Subordinate;
import se.swedenconnect.oidf.registry.api.model.TrustAnchor;
import se.swedenconnect.oidf.registry.api.model.TrustmarkIssuer;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static se.swedenconnect.oidf.registry.entity.EntityKeyType.FEDERATION_ENTITY;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class TestDataOperations {

  public static JWKSet genJWKS() {
    return new JWKSet(List.of(genKey(), genKey()));
  }

  public static JWK genKey() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
      keyGen.initialize(Curve.P_256.toECParameterSpec());

      final KeyPair keyPair = keyGen.generateKeyPair();

      return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).privateKey(keyPair.getPrivate())
          .keyID("ec-key-id" + new Random().nextInt(1000))
          .keyUse(KeyUse.SIGNATURE)
          .build();
    }
    catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Test method that creates a complete test scenario: 1. Creates a policy 2. Creates a federation entity 3. Adds a
   * trustanchor module to the federation entity 4. Creates another federation entity with relying_party metadata 5.
   * Creates a subordinate entity with subject set to the first federation entity, issuer set to the second federation
   * entity, and the created policy
   *
   * @param apiClient the OpenAPI client configured with authentication
   * @param baseUrl the base URL of the API (e.g., "https://localhost:8080")
   */
  public String createTestScenarioWithPolicyAndEntities(final ApiClient apiClient, final String baseUrl) {
    apiClient.setBasePath(baseUrl);

    final PoliciesApi policiesApi = new PoliciesApi(apiClient);
    final EntitiesApi entitiesApi = new EntitiesApi(apiClient);
    final ModulesApi modulesApi = new ModulesApi(apiClient);
    final SubordinatesApi subordinatesApi = new SubordinatesApi(apiClient);

    final List<EntityWithModules> e = entitiesApi.listEntities(FEDERATION_ENTITY.toString(), true);
    final String taEntityid = "https://www.pm.se/oidf/ta/";
    if (e.stream()
        .map(EntityWithModules::getFederationEntity)
        .map(FederationEntityWithModules::getEntityIdentifier)
        .anyMatch(s -> s.equals(taEntityid))) {
      return taEntityid;
    }

    // Step 1: Create a policy
    final Policy policyInput = Policy.builder()
        .name("Test Policy")
        .policy(Map.of("test", "policy", "version", 1))
        .build();
    final Policy createdPolicy = policiesApi.createPolicy(policyInput);
    log.info("Created policy with ID: {}", createdPolicy.getPolicyId());

    // Step 2: Create first federation entity
    final FederationEntity taFederationEntityInput = FederationEntity.builder()
        .entityIdentifier(taEntityid)
        .build();

    final FederationEntity trustAnchorEntity = entitiesApi.createFederationEntity(taFederationEntityInput);
    log.info("Created first federation entity with ID: {}", trustAnchorEntity.getEntityId());

    // Step 3: Add trustanchor module to the first federation entity
    final TrustAnchor trustAnchorInput = TrustAnchor.builder()
        .entityId(trustAnchorEntity.getEntityId())
        .active(true)
        .build();
    final TrustAnchor createdTrustAnchor = modulesApi.createTrustAnchor(trustAnchorInput);
    log.info("Created trustanchor module with ID: {} for entity: {}",
        createdTrustAnchor.getTrustAnchorId(), trustAnchorEntity.getEntityIdentifier());

    final Resolver resolverInput = Resolver.builder()
        .entityId(trustAnchorEntity.getEntityId())
        .trustAnchor(trustAnchorEntity.getEntityIdentifier())
        .trustedKeys(genJWKS().toString())
        .resolveResponseDuration("PT1H")
        .stepCachedValueThreshold(10)
        .stepRetryDuration("PT2M")
        .active(true)
        .build();
    final Resolver createdResolver = modulesApi.createResolver(resolverInput);
    log.info("Created resolver module with ID: {} for entity: {}",
        createdResolver.getResolverId(), trustAnchorEntity.getEntityIdentifier());

    final TrustmarkIssuer trustmarkIssuerInput = TrustmarkIssuer.builder()
        .entityId(trustAnchorEntity.getEntityId())
        .trustMarkTokenValidityDuration("PT1H")
        .active(true)
        .build();
    final TrustmarkIssuer createdTrustmarkIssuer = modulesApi.createTrustmarkIssuer(trustmarkIssuerInput);
    log.info("Created tmi module with ID: {} for entity: {}",
        createdTrustmarkIssuer.getTrustmarkIssuerId(), trustAnchorEntity.getEntityIdentifier());

    // Step 4: Create second federation entity with relying_party metadata
    final Map<String, Object> relyingPartyMetadata = new HashMap<>();
    relyingPartyMetadata.put("relying_party", Map.of(
        "client_name", "Test Relying Party",
        "redirect_uris", java.util.List.of("https://rp.test.se/callback")
    ));

    final HostedEntity secondFederationEntity = entitiesApi.createHostedEntity(HostedEntity.builder()
        .entityIdentifier("https://www.pm.se/oidf/relyingparty")
        .metadata(relyingPartyMetadata)
        .build());
    log.info("Created second federation entity with ID: {} and relying_party metadata",
        secondFederationEntity.getEntityId());

    // Step 5: Create subordinate entity with subject set to first federation entity,
    // issuer set to second federation entity, and the created policy

    subordinatesApi.createSubordinate(Subordinate.builder()
        .taImId(createdTrustAnchor.getTrustAnchorId())
        .entityIdentifier(secondFederationEntity.getEntityIdentifier())
        .policyId(createdPolicy.getPolicyId())
        .jwks(genJWKS().toString())
        .build());

    final HostedEntity hostedPolisen = entitiesApi.createHostedEntity(
        HostedEntity.builder()
            .entityIdentifier("https://www.polisen.se/op/sverigeid")
            .metadata(Map.of("openid_connect", "Polis polis..."))
            .build());

    subordinatesApi.createSubordinate(Subordinate.builder()
        .taImId(createdTrustAnchor.getTrustAnchorId())
        .entityIdentifier(hostedPolisen.getEntityIdentifier())
        .policyId(createdPolicy.getPolicyId())
        .ecLocationAutomaticResolve(true)
        .jwks(genJWKS().toString())
        .build());

    return taFederationEntityInput.getEntityIdentifier();
  }

}

