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

package se.swedenconnect.oidf.registry.validation;

import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.swedenconnect.oidf.registry.fixture.TestDataOperations.genKey;

public class PropertyValidatorsTest {
  final PropertyValidators propertyValidators = new PropertyValidators();

  @Test
  public void testResolveValidator_noValidators() {
    final PropertyValidator result = resolveValidator("");
    assertNotNull(result);
    assertDoesNotThrow(() -> result.validate("key", "value"));
  }

  @Test
  public void testResolveValidator_lengthValidator() {
    final PropertyValidator result = resolveValidator("length:5,10");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abcd"));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abcdefghijk"));
    assertDoesNotThrow(() -> result.validate("key", "abcde"));
  }

  @Test
  public void testResolveValidator_requiredValidator() {
    final PropertyValidator result = resolveValidator("required");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", ""));
    assertDoesNotThrow(() -> result.validate("key", "non-empty"));
  }

  @Test
  public void testResolveValidator_emailValidator() {
    final PropertyValidator result = resolveValidator("email");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid"));
    assertDoesNotThrow(() -> result.validate("key", "test@example.com"));
  }

  @Test
  public void testResolveValidator_endsWithValidator() {
    final PropertyValidator result = resolveValidator("ends_with:xyz");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc"));
    assertDoesNotThrow(() -> result.validate("key", "abcxyz"));
  }

  @Test
  public void testResolveValidator_startsWithValidator() {
    final PropertyValidator result = resolveValidator("starts_with:abc");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "xyz"));
    assertDoesNotThrow(() -> result.validate("key", "abcxyz"));
  }

  @Test
  public void testResolveValidator_containsValidator() {
    final PropertyValidator result = resolveValidator("contains:abc");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "xyz"));
    assertDoesNotThrow(() -> result.validate("key", "xyzabcxyz"));
  }

  @Test
  public void testResolveValidator_alphaValidator() {
    final PropertyValidator result = resolveValidator("alpha");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc123"));
    assertDoesNotThrow(() -> result.validate("key", "abcdef"));
  }

  @Test
  public void testResolveValidator_alphanumericValidator() {
    final PropertyValidator result = resolveValidator("alphanumeric");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc$%"));
    assertDoesNotThrow(() -> result.validate("key", "abc123"));
  }

  @Test
  public void testResolveValidator_numberValidator() {
    final PropertyValidator result = resolveValidator("number");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc"));
    assertDoesNotThrow(() -> result.validate("key", "123.45"));
  }

  @Test
  public void testResolveValidator_dateValidator() {
    final PropertyValidator result = resolveValidator("date");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid-date"));
    assertDoesNotThrow(() -> result.validate("key", "2023-01-01"));
  }

  @Test
  public void testResolveValidator_betweenValidator() {
    final PropertyValidator result = resolveValidator("between:1,10");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "0"));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "11"));
    assertDoesNotThrow(() -> result.validate("key", "5"));
  }

  @Test
  public void testResolveValidator_urlValidator() {
    final PropertyValidator result = resolveValidator("url");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid-url"));
    assertDoesNotThrow(() -> result.validate("key", "https://example.com"));
  }

  @Test
  public void testResolveValidator_matchesValidator() {
    final PropertyValidator result = resolveValidator("matches:^\\d{3}-\\d{2}-\\d{4}$");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "1234"));
    assertDoesNotThrow(() -> result.validate("key", "123-45-6789"));
  }

  @Test
  public void testResolveValidator_minValidator() {
    final PropertyValidator result = resolveValidator("min:5");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "3"));
    assertDoesNotThrow(() -> result.validate("key", "10"));
  }

  @Test
  public void testResolveValidator_maxValidator() {
    final PropertyValidator result = resolveValidator("max:10.1");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "40"));
    assertDoesNotThrow(() -> result.validate("key", "10"));
  }

  @Test
  public void testResolveValidator_jsonValidator_validJson() {
    final PropertyValidator result = resolveValidator("json:");
    assertDoesNotThrow(() -> result.validate("key", "{\"name\":\"value\"}"));
  }

  @Test
  public void testResolveValidator_jsonValidator_invalidJson() {
    final PropertyValidator result = resolveValidator("json:");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "invalid-json"));
  }

  @Test
  public void testResolveValidator_jwksValidator() {
    final String jwks =
        "{\"keys\": [{\"kty\": \"EC\",\"x5t#S256\": \"Ww-i1TD0345YYbP-kLa7fQW5A-3VuiQBm8z_rl7RbVk\",\"crv\": \"P-256\",\"kid\": \"SKv8j4XjvowMoTsY67ch6GMSL5vPqsGc5Nsk_NL-wTk\",\"x\": \"i8zRvt76etocbhjQLibFHItxgYVC1hwR10fAEzqPVJw\",\"y\": \"_RNoMz5MKfgomuOgOm53-UJkqXaIw8c1ojb1bQBFaFs\"}]}";

    final PropertyValidator result = resolveValidator("jwks");
    assertDoesNotThrow(() -> result.validate("key", jwks));
  }

  @Test
  public void testResolveValidator_jwksValidator_PrivateAndPublicKeys() {

    final JWKSet set = new JWKSet(genKey());

    final String jwkPrivate = set.toString(false);
    final String jwkPublic = set.toString(true);

    final PropertyValidator result = resolveValidator("jwks:public | jwks:kid");
    assertDoesNotThrow(() -> result.validate("key", jwkPublic));
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", jwkPrivate));
  }

  @Test
  public void testResolveValidator_emptyvalue() {
    final PropertyValidator result = resolveValidator("jwks");
    assertDoesNotThrow(() -> result.validate("key", ""));
    assertDoesNotThrow(() -> result.validate("key", null));
  }

  @Test
  public void testResolveValidator_jwksValidator_noKid() {
    String jwkValue =
        "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"n\":\"valid-modulus\",\"e\":\"AQAB\"}]}";

    final PropertyValidator result = resolveValidator("jwks");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", jwkValue));
  }

  @Test
  public void testResolveValidator_jwksValidator_invalidJwk() {
    final String invalidJwkValue = genKey().toString();
    final PropertyValidator result = resolveValidator("jwk");
    assertDoesNotThrow(() -> result.validate("key", invalidJwkValue));
  }

  @Test
  void isValidatorSupported() {
    assertTrue(this.propertyValidators.isValidatorSupported("uuid"));
    assertTrue(this.propertyValidators.isValidatorSupported("length"));
    assertTrue(this.propertyValidators.isValidatorSupported("json"));
    assertTrue(this.propertyValidators.isValidatorSupported("required"));
    assertTrue(this.propertyValidators.isValidatorSupported("email"));

    assertTrue(this.propertyValidators.isValidatorSupported("length:10,20"));
    assertTrue(this.propertyValidators.isValidatorSupported("starts_with:prefix"));

    assertFalse(this.propertyValidators.isValidatorSupported("nonexistent"));
    assertFalse(this.propertyValidators.isValidatorSupported("invalid_validator"));
    assertFalse(this.propertyValidators.isValidatorSupported(""));

    assertFalse(this.propertyValidators.isValidatorSupported(null));

  }

  public PropertyValidator resolveValidator(final String validatorString) {
    return this.propertyValidators.resolveValidator(validatorString, VariabelValueResolver.defaultResolver());
  }
}