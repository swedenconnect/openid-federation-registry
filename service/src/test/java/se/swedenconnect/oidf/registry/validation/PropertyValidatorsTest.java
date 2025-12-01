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

import static org.assertj.core.api.Assertions.*;
import static se.swedenconnect.oidf.registry.fixture.TestDataOperations.genKey;

/**
 * Unit tests for the {@link PropertyValidators} class.
 *
 * @author Per Fredrik Plars
 */
public class PropertyValidatorsTest {
  final PropertyValidators propertyValidators = new PropertyValidators();

  @Test
  public void testResolveValidator_noValidators() {
    final PropertyValidator result = resolveValidator("");
    assertThat(result).isNotNull();
    assertThatCode(() -> result.validate("key", "value")).doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_lengthValidator() {
    final PropertyValidator result = resolveValidator("length:5,10");
    assertThatThrownBy(() -> result.validate("key", "abcd"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> result.validate("key", "abcdefghijk"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "abcde"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_requiredValidator() {
    final PropertyValidator result = resolveValidator("required");
    assertThatThrownBy(() -> result.validate("key", ""))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "non-empty"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_emailValidator() {
    final PropertyValidator result = resolveValidator("email");
    assertThatThrownBy(() -> result.validate("key", "invalid"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "test@example.com"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_endsWithValidator() {
    final PropertyValidator result = resolveValidator("ends_with:xyz");
    assertThatThrownBy(() -> result.validate("key", "abc"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "abcxyz"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_startsWithValidator() {
    final PropertyValidator result = resolveValidator("starts_with:http://www.digg.se");
    assertThatThrownBy(() -> result.validate("key", "http://www.novalidate.se"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "http://www.digg.se"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_containsValidator() {
    final PropertyValidator result = resolveValidator("contains:abc");
    assertThatThrownBy(() -> result.validate("key", "xyz"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "xyzabcxyz"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_alphaValidator() {
    final PropertyValidator result = resolveValidator("alpha");
    assertThatThrownBy(() -> result.validate("key", "abc123"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "abcdef"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_alphanumericValidator() {
    final PropertyValidator result = resolveValidator("alphanumeric");
    assertThatThrownBy(() -> result.validate("key", "abc$%"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "abc123"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_numberValidator() {
    final PropertyValidator result = resolveValidator("number");
    assertThatThrownBy(() -> result.validate("key", "abc"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "123.45"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_dateValidator() {
    final PropertyValidator result = resolveValidator("date");
    assertThatThrownBy(() -> result.validate("key", "invalid-date"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "2023-01-01"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_betweenValidator() {
    final PropertyValidator result = resolveValidator("between:1,10");
    assertThatThrownBy(() -> result.validate("key", "0"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatThrownBy(() -> result.validate("key", "11"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "5"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_urlValidator() {
    final PropertyValidator result = resolveValidator("url");
    assertThatThrownBy(() -> result.validate("key", "invalid-url"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "https://example.com"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_matchesValidator() {
    final PropertyValidator result = resolveValidator("matches:^\\d{3}-\\d{2}-\\d{4}$");
    assertThatThrownBy(() -> result.validate("key", "1234"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "123-45-6789"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_minValidator() {
    final PropertyValidator result = resolveValidator("min:5");
    assertThatThrownBy(() -> result.validate("key", "3"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "10"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_maxValidator() {
    final PropertyValidator result = resolveValidator("max:10.1");
    assertThatThrownBy(() -> result.validate("key", "40"))
        .isInstanceOf(PropertyValidationFailException.class);
    assertThatCode(() -> result.validate("key", "10"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_jsonValidator_validJson() {
    final PropertyValidator result = resolveValidator("json:");
    assertThatCode(() -> result.validate("key", "{\"name\":\"value\"}"))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_jsonValidator_invalidJson() {
    final PropertyValidator result = resolveValidator("json:");
    assertThatThrownBy(() -> result.validate("key", "invalid-json"))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  public void testResolveValidator_jwksValidator() {
    final String jwks =
        "{\"keys\": [{\"kty\": \"EC\",\"x5t#S256\": \"Ww-i1TD0345YYbP-kLa7fQW5A-3VuiQBm8z_rl7RbVk\",\"crv\": \"P-256\",\"kid\": \"SKv8j4XjvowMoTsY67ch6GMSL5vPqsGc5Nsk_NL-wTk\",\"x\": \"i8zRvt76etocbhjQLibFHItxgYVC1hwR10fAEzqPVJw\",\"y\": \"_RNoMz5MKfgomuOgOm53-UJkqXaIw8c1ojb1bQBFaFs\"}]}";

    final PropertyValidator result = resolveValidator("jwks");
    assertThatCode(() -> result.validate("key", jwks))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_jwksValidator_PrivateAndPublicKeys() {

    final JWKSet set = new JWKSet(genKey());

    final String jwkPrivate = set.toString(false);
    final String jwkPublic = set.toString(true);

    final PropertyValidator result = resolveValidator("jwks:public | jwks:kid");
    assertThatCode(() -> result.validate("key", jwkPublic))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> result.validate("key", jwkPrivate))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  public void testResolveValidator_emptyvalue() {
    final PropertyValidator result = resolveValidator("jwks");
    assertThatCode(() -> result.validate("key", ""))
        .doesNotThrowAnyException();
    assertThatCode(() -> result.validate("key", null))
        .doesNotThrowAnyException();
  }

  @Test
  public void testResolveValidator_jwksValidator_noKid() {
    String jwkValue =
        "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"n\":\"valid-modulus\",\"e\":\"AQAB\"}]}";

    final PropertyValidator result = resolveValidator("jwks");
    assertThatThrownBy(() -> result.validate("key", jwkValue))
        .isInstanceOf(PropertyValidationFailException.class);
  }

  @Test
  public void testResolveValidator_jwksValidator_invalidJwk() {
    final String invalidJwkValue = genKey().toString();
    final PropertyValidator result = resolveValidator("jwk");
    assertThatCode(() -> result.validate("key", invalidJwkValue))
        .doesNotThrowAnyException();
  }

  @Test
  void isValidatorSupported() {
    assertThat(this.propertyValidators.isValidatorSupported("uuid")).isTrue();
    assertThat(this.propertyValidators.isValidatorSupported("length")).isTrue();
    assertThat(this.propertyValidators.isValidatorSupported("json")).isTrue();
    assertThat(this.propertyValidators.isValidatorSupported("required")).isTrue();
    assertThat(this.propertyValidators.isValidatorSupported("email")).isTrue();

    assertThat(this.propertyValidators.isValidatorSupported("length:10,20")).isTrue();
    assertThat(this.propertyValidators.isValidatorSupported("starts_with:prefix")).isTrue();

    assertThat(this.propertyValidators.isValidatorSupported("nonexistent")).isFalse();
    assertThat(this.propertyValidators.isValidatorSupported("invalid_validator")).isFalse();
    assertThat(this.propertyValidators.isValidatorSupported("")).isFalse();

    assertThat(this.propertyValidators.isValidatorSupported(null)).isFalse();
  }

  public PropertyValidator resolveValidator(final String validatorString) {
    return this.propertyValidators.resolveValidator(validatorString, VariableValueResolver.defaultResolver());
  }
}