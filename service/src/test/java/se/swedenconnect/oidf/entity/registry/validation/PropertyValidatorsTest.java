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

package se.swedenconnect.oidf.entity.registry.validation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PropertyValidatorsTest {
  final PropertyValidators propertyValidators = new PropertyValidators();

  @Test
  public void testResolveValidator_noValidators() {
    final PropertyValidator result = propertyValidators.resolveValidator("");
    assertNotNull(result);
    assertDoesNotThrow(() -> result.validate("key", "value"));
  }

  @Test
  public void testResolveValidator_requiredValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("req");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", ""));
    assertDoesNotThrow(() -> result.validate("key", "value"));
  }

  @Test
  public void testResolveValidator_regexValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("regex:^\\d+$");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abcd"));
    assertDoesNotThrow(() -> result.validate("key", "12345"));
  }

  @Test
  public void testResolveValidator_minValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("min:5");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "1234"));
    assertDoesNotThrow(() -> result.validate("key", "12345"));
  }

  @Test
  public void testResolveValidator_maxValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("max:5");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "123456"));
    assertDoesNotThrow(() -> result.validate("key", "123"));
  }

  @Test
  public void testResolveValidator_jsonValidator_validJson() {
    final PropertyValidator result = propertyValidators.resolveValidator("json:");
    assertDoesNotThrow(() -> result.validate("key", "{\"name\":\"value\"}"));
  }

  @Test
  public void testResolveValidator_jsonValidator_invalidJson() {
    final PropertyValidator result = propertyValidators.resolveValidator("json:");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid-json"));
  }

  @Test
  public void testResolveValidator_jwksValidator_PrivateAndPublicKeys()
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {

    final JWKSet set = new JWKSet(genKey());

    final String jwkPrivate = set.toString(false);
    final String jwkPublic = set.toString(true);

    final PropertyValidator result = propertyValidators.resolveValidator("jwks:public | jwks:kid");
    assertDoesNotThrow(() -> result.validate("key", jwkPublic));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", jwkPrivate));
  }

  private JWK genKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(Curve.P_256.toECParameterSpec());

    final KeyPair keyPair = keyGen.generateKeyPair();

    final ECKey ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
        .privateKey(keyPair.getPrivate())
        .keyID("ec-key-id") // Ange ett unikt kid (Key ID)
        .build();
    return ecKey;
  }

  @Test
  public void testResolveValidator_jwksValidator_noKid() {
    String jwkValue =
        "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"n\":\"valid-modulus\",\"e\":\"AQAB\"}]}";

    final PropertyValidator result = propertyValidators.resolveValidator("jwks");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", jwkValue));
  }

  @Test
  public void testResolveValidator_jwksValidator_invalidJwk() {

    final String invalidJwkValue = "{\"keys\":[]}";

    final PropertyValidator result = propertyValidators.resolveValidator("jwks:public");

    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", invalidJwkValue));
  }
}