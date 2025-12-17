/*
 * Copyright 2025 Sweden Connect
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
import se.swedenconnect.oidf.registry.api.model.FederationEntity;
import se.swedenconnect.oidf.registry.api.model.HostedEntity;
import se.swedenconnect.oidf.registry.api.model.Policy;
import se.swedenconnect.oidf.registry.api.model.Resolver;
import se.swedenconnect.oidf.registry.api.model.SubordinateEntity;
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
import java.util.UUID;

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
   * @return a map containing the created entities and policy IDs
   */
  public Map<String, UUID> createTestScenarioWithPolicyAndEntities(final ApiClient apiClient, final String baseUrl) {
    apiClient.setBasePath(baseUrl);

    final PoliciesApi policiesApi = new PoliciesApi(apiClient);
    final EntitiesApi entitiesApi = new EntitiesApi(apiClient);
    final ModulesApi modulesApi = new ModulesApi(apiClient);

    final Map<String, UUID> result = new HashMap<>();

    // Step 1: Create a policy
    final Policy policyInput = Policy.builder()
        .name("Test Policy")
        .policy(Map.of("test", "policy", "version", 1))
        .build();
    final Policy createdPolicy = policiesApi.createPolicy(policyInput);
    result.put("policyId", createdPolicy.getPolicyId());
    log.info("Created policy with ID: {}", createdPolicy.getPolicyId());

    // Step 2: Create first federation entity
    final FederationEntity firstFederationEntityInput = FederationEntity.builder()
        .issuer("https://www.pm.se/oidf/ta")
        .metadata(Map.of("federation_entity", Map.of("organization_name", "Test Organization 1")))
        .build();
    final FederationEntity firstFederationEntity = entitiesApi.createFederationEntity(firstFederationEntityInput);
    result.put("firstFederationEntityId", firstFederationEntity.getEntityId());
    log.info("Created first federation entity with ID: {}", firstFederationEntity.getEntityId());

    // Step 3: Add trustanchor module to the first federation entity
    final TrustAnchor trustAnchorInput = TrustAnchor.builder()
        .entityId(firstFederationEntity.getEntityId())
        .active(true)
        .build();
    final TrustAnchor createdTrustAnchor = modulesApi.createTrustAnchor(trustAnchorInput);
    result.put("trustAnchorModuleId", createdTrustAnchor.getTrustAnchorId());
    log.info("Created trustanchor module with ID: {} for entity: {}",
        createdTrustAnchor.getTrustAnchorId(), firstFederationEntity.getIssuer());

    final Resolver resolverInput = Resolver.builder()
        .entityId(firstFederationEntity.getEntityId())
        .trustAnchor(firstFederationEntity.getIssuer())
        .trustedKeys(genJWKS().toString())
        .resolveResponseDuration("PT1H")
        .stepCachedValueThreshold(10)
        .stepRetryDuration("PT2M")
        .active(true)
        .build();
    final Resolver createdResolver = modulesApi.createResolver(resolverInput);
    result.put("resolveModuleId", createdResolver.getResolverId());
    log.info("Created resolver module with ID: {} for entity: {}",
        createdResolver.getResolverId(), firstFederationEntity.getIssuer());

    final TrustmarkIssuer trustmarkIssuerInput = TrustmarkIssuer.builder()
        .entityId(firstFederationEntity.getEntityId())
        .trustMarkTokenValidityDuration("PT1H")
        .active(true)
        .build();
    final TrustmarkIssuer createdTrustmarkIssuer = modulesApi.createTrustmarkIssuer(trustmarkIssuerInput);
    result.put("tmiId", createdTrustmarkIssuer.getTrustmarkIssuerId());
    log.info("Created tmi module with ID: {} for entity: {}",
        createdTrustmarkIssuer.getTrustmarkIssuerId(), firstFederationEntity.getIssuer());

    // Step 4: Create second federation entity with relying_party metadata
    final Map<String, Object> relyingPartyMetadata = new HashMap<>();
    relyingPartyMetadata.put("relying_party", Map.of(
        "client_name", "Test Relying Party",
        "redirect_uris", java.util.List.of("https://rp.test.se/callback")
    ));
    final FederationEntity secondFederationEntityInput = FederationEntity.builder()
        .issuer("https://www.pm.se/oidf/relyingparty")
        .metadata(relyingPartyMetadata)
        .build();
    final FederationEntity secondFederationEntity = entitiesApi.createFederationEntity(secondFederationEntityInput);
    result.put("secondFederationEntityId", secondFederationEntity.getEntityId());
    log.info("Created second federation entity with ID: {} and relying_party metadata",
        secondFederationEntity.getEntityId());

    // Step 5: Create subordinate entity with subject set to first federation entity,
    // issuer set to second federation entity, and the created policy
    final SubordinateEntity subordinateEntityInput = SubordinateEntity.builder()
        .subject(firstFederationEntity.getIssuer())
        .issuer(secondFederationEntity.getIssuer())
        .policyId(createdPolicy.getPolicyId())
        .jwks(genJWKS().toString())
        .build();

    final SubordinateEntity createdSubordinateEntity = entitiesApi.createSubordinateEntity(subordinateEntityInput);
    result.put("subordinateEntityId", createdSubordinateEntity.getEntityId());

    final HostedEntity hostedEntityEntityInput = HostedEntity.builder()
        .issuer("https://www.polisen.se/op/swerigeid")
        .build();

    final HostedEntity createdHostedEntity = entitiesApi.createHostedEntity(hostedEntityEntityInput);
    result.put("createdHostedEntityId", createdHostedEntity.getEntityId());

    log.info("Created subordinate entity with ID: {}, subject: {}, issuer: {}, policyId: {}",
        createdSubordinateEntity.getEntityId(),
        createdSubordinateEntity.getSubject(),
        createdSubordinateEntity.getIssuer(),
        createdPolicy.getPolicyId());

    return result;
  }

}

