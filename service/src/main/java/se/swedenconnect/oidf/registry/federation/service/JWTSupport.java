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

package se.swedenconnect.oidf.registry.federation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

import static se.swedenconnect.oidf.registry.infrastructure.validation.PropertyValidators.mapper;

/**
 * A utility class for generating and signing JSON Web Tokens (JWTs) using a specified JSON Web Key (JWK). This class
 * enables custom JWT claim generation and signing operations for secure token generation.
 *
 * @author Per Fredrik Plars
 */
public class JWTSupport {
  final JWK signKey;
  final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Constructs an instance of JWTSupport with the specified signing key.
   *
   * @param signKey the JSON Web Key (JWK) used for signing JWTs
   */
  public JWTSupport(final JWK signKey) {
    this.signKey = signKey;
  }

  private JWTClaimsSet.Builder defaultClaimSet() {
    return new JWTClaimsSet.Builder()
        .issueTime(new Date())
        .jwtID(UUID.randomUUID().toString());
  }

  /**
   * Provides a {@link JWSSigner} implementation based on the type of key used in the {@code signKey}.
   *
   * @return a {@link JWSSigner} instance suitable for the key type (RSA or EC) present in {@code signKey}.
   * @throws JOSEException if the key type is unsupported or an error occurs during the signer creation.
   */
  private JWSSigner getSigner() throws JOSEException {
    final String keyType = this.signKey.getKeyType().getValue();
    if (keyType.equals(KeyType.RSA.getValue())) {
      return new RSASSASigner(this.signKey.toRSAKey());
    }

    if (keyType.equals(KeyType.EC.getValue())) {
      return new ECDSASigner(this.signKey.toECKey());
    }
    throw new JOSEException("Unsupported key type: " + keyType);
  }

  /**
   * Signs a JWT with the provided claims and returns the signed JWT.
   *
   * @param jwtTypeName the type of the JWT to be signed. This is used to set the type in the header, with
   *     underscores replaced by dashes.
   * @param claimDecorator a function to customize the default claims of the JWT. It takes a default
   *     JWTClaimsSet.Builder and returns a modified JWTClaimsSet.Builder.
   * @return the signed JWT as a {@code SignedJWT} object.
   * @throws RuntimeException if an error occurs during signing, such as an unsupported key type or issues with the
   *     signing operation.
   */
  public SignedJWT signJWT(final String jwtTypeName,
      final Function<JWTClaimsSet.Builder, JWTClaimsSet.Builder> claimDecorator) {
    try {
      final JWSSigner signer = this.getSigner();
      final JWSAlgorithm alg = signer.supportedJWSAlgorithms().stream().findFirst().orElseThrow();
      final JWSHeader header = new JWSHeader.Builder(alg)
          .type(new JOSEObjectType(jwtTypeName.replace('_', '-') + "+jwt"))
          .keyID(this.signKey.getKeyID())
          .build();
      final SignedJWT jwt = new SignedJWT(header, claimDecorator.apply(this.defaultClaimSet()).build());
      jwt.sign(signer);
      return jwt;
    }
    catch (final JOSEException e) {
      throw new RuntimeException("Unable to sign JWT", e);
    }
  }

  /**
   * Converts JWT string to pretty JSON format.
   *
   * @param jwtString the JWT string
   * @return the pretty JSON format
   */
  public String toPrettyJson(final String jwtString) {
    try {
      final SignedJWT signedJWT = SignedJWT.parse(jwtString);

      final Map<String, Object> plainJwt = new TreeMap<>();
      plainJwt.put("header", signedJWT.getHeader().toJSONObject());
      plainJwt.put("payload", signedJWT.getJWTClaimsSet().toJSONObject());
      plainJwt.put("signature", signedJWT.getSignature().toString());

      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(plainJwt);
    }
    catch (final Exception e) {
      throw new RuntimeException("Error formatting JWT", e);
    }
  }

}
