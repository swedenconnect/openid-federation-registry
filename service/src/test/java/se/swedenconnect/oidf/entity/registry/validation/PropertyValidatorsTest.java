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

import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.swedenconnect.oidf.entity.registry.fixture.TestDataOperations.genKey;

public class PropertyValidatorsTest {
  final PropertyValidators propertyValidators = new PropertyValidators();

  @Test
  public void testResolveValidator_noValidators() {
    final PropertyValidator result = propertyValidators.resolveValidator("");
    assertNotNull(result);
    assertDoesNotThrow(() -> result.validate("key", "value"));
  }

  @Test
  public void testResolveValidator_lengthValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("length:5,10");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abcd"));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abcdefghijk"));
    assertDoesNotThrow(() -> result.validate("key", "abcde"));
  }

  @Test
  public void testResolveValidator_requiredValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("required");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", ""));
    assertDoesNotThrow(() -> result.validate("key", "non-empty"));
  }

  @Test
  public void testResolveValidator_emailValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("email");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid"));
    assertDoesNotThrow(() -> result.validate("key", "test@example.com"));
  }

  @Test
  public void testResolveValidator_endsWithValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("ends_with:xyz");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc"));
    assertDoesNotThrow(() -> result.validate("key", "abcxyz"));
  }

  @Test
  public void testResolveValidator_startsWithValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("starts_with:abc");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "xyz"));
    assertDoesNotThrow(() -> result.validate("key", "abcxyz"));
  }

  @Test
  public void testResolveValidator_containsValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("contains:abc");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "xyz"));
    assertDoesNotThrow(() -> result.validate("key", "xyzabcxyz"));
  }

  @Test
  public void testResolveValidator_alphaValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("alpha");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc123"));
    assertDoesNotThrow(() -> result.validate("key", "abcdef"));
  }

  @Test
  public void testResolveValidator_alphanumericValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("alphanumeric");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc$%"));
    assertDoesNotThrow(() -> result.validate("key", "abc123"));
  }

  @Test
  public void testResolveValidator_numberValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("number");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc"));
    assertDoesNotThrow(() -> result.validate("key", "123.45"));
  }

  @Test
  public void testResolveValidator_dateValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("date");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid-date"));
    assertDoesNotThrow(() -> result.validate("key", "2023-01-01"));
  }

  @Test
  public void testResolveValidator_betweenValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("between:1,10");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "0"));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "11"));
    assertDoesNotThrow(() -> result.validate("key", "5"));
  }

  @Test
  public void testResolveValidator_urlValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("url");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid-url"));
    assertDoesNotThrow(() -> result.validate("key", "https://example.com"));
  }

  @Test
  public void testResolveValidator_matchesValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("matches:^\\d{3}-\\d{2}-\\d{4}$");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "1234"));
    assertDoesNotThrow(() -> result.validate("key", "123-45-6789"));
  }

  @Test
  public void testResolveValidator_minValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("min:5");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "3"));
    assertDoesNotThrow(() -> result.validate("key", "10"));
  }

  @Test
  public void testResolveValidator_maxValidator() {
    final PropertyValidator result = propertyValidators.resolveValidator("max:10.1");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "40"));
    assertDoesNotThrow(() -> result.validate("key", "10"));
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
  public void testResolveValidator_jwksValidator() {
    final String jwks =
        "{\"keys\": [{\"kty\": \"EC\",\"x5t#S256\": \"Ww-i1TD0345YYbP-kLa7fQW5A-3VuiQBm8z_rl7RbVk\",\"crv\": \"P-256\",\"kid\": \"SKv8j4XjvowMoTsY67ch6GMSL5vPqsGc5Nsk_NL-wTk\",\"x\": \"i8zRvt76etocbhjQLibFHItxgYVC1hwR10fAEzqPVJw\",\"y\": \"_RNoMz5MKfgomuOgOm53-UJkqXaIw8c1ojb1bQBFaFs\"}]}";

    final PropertyValidator result = propertyValidators.resolveValidator("jwks");
    assertDoesNotThrow(() -> result.validate("key", jwks));
  }

  @Test
  public void testResolveValidator_jwksValidator_PrivateAndPublicKeys() {

    final JWKSet set = new JWKSet(genKey());

    final String jwkPrivate = set.toString(false);
    final String jwkPublic = set.toString(true);

    final PropertyValidator result = propertyValidators.resolveValidator("jwks:public | jwks:kid");
    assertDoesNotThrow(() -> result.validate("key", jwkPublic));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", jwkPrivate));
  }

  @Test
  public void testResolveValidator_emptyvalue() {
    final PropertyValidator result = propertyValidators.resolveValidator("jwks");
    assertDoesNotThrow(() -> result.validate("key", ""));
    assertDoesNotThrow(() -> result.validate("key", null));
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
    final String invalidJwkValue = genKey().toString();
    final PropertyValidator result = propertyValidators.resolveValidator("jwk");
    assertDoesNotThrow(() -> result.validate("key", invalidJwkValue));
  }

  @Test
  void isValidatorSupported() {
    assertTrue(propertyValidators.isValidatorSupported("uuid"));
    assertTrue(propertyValidators.isValidatorSupported("length"));
    assertTrue(propertyValidators.isValidatorSupported("json"));
    assertTrue(propertyValidators.isValidatorSupported("required"));
    assertTrue(propertyValidators.isValidatorSupported("email"));

    assertTrue(propertyValidators.isValidatorSupported("length:10,20"));
    assertTrue(propertyValidators.isValidatorSupported("starts_with:prefix"));

    assertFalse(propertyValidators.isValidatorSupported("nonexistent"));
    assertFalse(propertyValidators.isValidatorSupported("invalid_validator"));
    assertFalse(propertyValidators.isValidatorSupported(""));

    assertFalse(propertyValidators.isValidatorSupported(null));

  }
}