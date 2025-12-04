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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
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

  private final TestRestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  final JwtTestUtils jwtTestUtils = new JwtTestUtils();

  public TestDataOperations(final TestRestTemplate restTemplate, final ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  public static JWK genKey() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
      keyGen.initialize(Curve.P_256.toECParameterSpec());

      final KeyPair keyPair = keyGen.generateKeyPair();

      return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).privateKey(keyPair.getPrivate())
          .keyID("ec-key-id" + new Random().nextInt(100))
          .build();
    }
    catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  public UUID createPolicies(JwtTestUtils.OrganisationType organisationType) {
    return null;
  }

  public UUID updatePolicies(
      final JwtTestUtils.OrganisationType organizationType,
      final UUID policyId,
      final Map<String, String> dataToUpdate) throws JsonProcessingException {
    return null;
  }
}

