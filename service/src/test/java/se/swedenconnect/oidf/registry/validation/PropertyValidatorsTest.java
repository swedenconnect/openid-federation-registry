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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public void testResolveValidator_jwksValidatorBuilder() {
    final String jwks = """
        {
                  "keys": [
                    {
                      "kty": "RSA",
                      "e": "AQAB",
                      "use": "sig",
                      "kid": "a152f1cd-f25b-47fe-8b06-3aec95357ff8",
                      "x5c": [
                        "MIIC6zCCAdOgAwIBAgIJANa6D4qXtBLPMA0GCSqGSIb3DQEBCwUAMDUxEzARBgNVBAMMClNlbGZTaWduZWQxETAPBgNVBAoMCE9pZGZUZXN0MQswCQYDVQQGEwJTRTAeFw0yNTEwMTUxMTQyMTdaFw0yNzEwMTUxMTQzMTdaMDUxEzARBgNVBAMMClNlbGZTaWduZWQxETAPBgNVBAoMCE9pZGZUZXN0MQswCQYDVQQGEwJTRTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKGKheaFfpaMje2SuayUwEkFS7KBbrB6882dXYLkKBM/3/mumadQiS42Trigj/a1r9RwX9oDqTKpoOxXCyyN7qom7QyaSaMrWOGoTK4RbrofCbg5kSIToVrwpxcNR2pUuhJLga1rSVCo626pijK+o4lT9aWhdZrQVCYVFHzoDKLdOINeQAfCIfGaynwa2vcQVr/PHvUWifBlgXe0qig+S8dpIt2Ze8tG1oQgcHhF1dhK7/xiHbsX1XJ79HfEiMBZAmAK6FtITNmklOB83YmN3HCIgQmyFxKcq/NVh+JId287xI/ErEGk5MT0OQhRjsiIUjlSjYxk3uLMZIfJCpKeoTkCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAFyjB1jqHvzrCgrd3NqNnKfFOu4U/DNhvw/MoVTXA5oXedlbDhLyuIoJ1aPK04xFOg2q2aj579chsl8/HO7yTcD/6wIir3LXl3nat8QfAGbhQNLNhoDS2STweVh6WBvOujWDvqQtQ6w+YdzA5gnomJTlyHRaCxjCFj/PA4VwY2U7AtF1v2APqzhW48mL+DoY+mk6P6x0BleBiuMEDNGM2yGRHOuKu/lPvALDDTCDU5ZCtBMXjaQMVB3dDErwLSqxDRbF6ioEZ55U495y9APP5nZBb7aLB/3ZtCGWy7b9N/6IkqxqCWq8IfdSqNGX+1RXdxq9a9NspQ0hqxHVOnrJJuQ=="
                      ],
                      "alg": "RS256",
                      "n": "oYqF5oV-loyN7ZK5rJTASQVLsoFusHrzzZ1dguQoEz_f-a6Zp1CJLjZOuKCP9rWv1HBf2gOpMqmg7FcLLI3uqibtDJpJoytY4ahMrhFuuh8JuDmRIhOhWvCnFw1HalS6EkuBrWtJUKjrbqmKMr6jiVP1paF1mtBUJhUUfOgMot04g15AB8Ih8ZrKfBra9xBWv88e9RaJ8GWBd7SqKD5Lx2ki3Zl7y0bWhCBweEXV2Erv_GIduxfVcnv0d8SIwFkCYAroW0hM2aSU4HzdiY3ccIiBCbIXEpyr81WH4kh3bzvEj8SsQaTkxPQ5CFGOyIhSOVKNjGTe4sxkh8kKkp6hOQ"
                    },
                    {
                      "kty": "RSA",
                      "e": "AQAB",
                      "use": "sig",
                      "kid": "a152f1cd-f25b-47fe-8b06-3aec95357ff8",
                      "x5c": [
                        "MIIC6zCCAdOgAwIBAgIJANa6D4qXtBLPMA0GCSqGSIb3DQEBCwUAMDUxEzARBgNVBAMMClNlbGZTaWduZWQxETAPBgNVBAoMCE9pZGZUZXN0MQswCQYDVQQGEwJTRTAeFw0yNTEwMTUxMTQyMTdaFw0yNzEwMTUxMTQzMTdaMDUxEzARBgNVBAMMClNlbGZTaWduZWQxETAPBgNVBAoMCE9pZGZUZXN0MQswCQYDVQQGEwJTRTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKGKheaFfpaMje2SuayUwEkFS7KBbrB6882dXYLkKBM/3/mumadQiS42Trigj/a1r9RwX9oDqTKpoOxXCyyN7qom7QyaSaMrWOGoTK4RbrofCbg5kSIToVrwpxcNR2pUuhJLga1rSVCo626pijK+o4lT9aWhdZrQVCYVFHzoDKLdOINeQAfCIfGaynwa2vcQVr/PHvUWifBlgXe0qig+S8dpIt2Ze8tG1oQgcHhF1dhK7/xiHbsX1XJ79HfEiMBZAmAK6FtITNmklOB83YmN3HCIgQmyFxKcq/NVh+JId287xI/ErEGk5MT0OQhRjsiIUjlSjYxk3uLMZIfJCpKeoTkCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAFyjB1jqHvzrCgrd3NqNnKfFOu4U/DNhvw/MoVTXA5oXedlbDhLyuIoJ1aPK04xFOg2q2aj579chsl8/HO7yTcD/6wIir3LXl3nat8QfAGbhQNLNhoDS2STweVh6WBvOujWDvqQtQ6w+YdzA5gnomJTlyHRaCxjCFj/PA4VwY2U7AtF1v2APqzhW48mL+DoY+mk6P6x0BleBiuMEDNGM2yGRHOuKu/lPvALDDTCDU5ZCtBMXjaQMVB3dDErwLSqxDRbF6ioEZ55U495y9APP5nZBb7aLB/3ZtCGWy7b9N/6IkqxqCWq8IfdSqNGX+1RXdxq9a9NspQ0hqxHVOnrJJuQ=="
                      ],
                      "alg": "RS256",
                      "n": "oYqF5oV-loyN7ZK5rJTASQVLsoFusHrzzZ1dguQoEz_f-a6Zp1CJLjZOuKCP9rWv1HBf2gOpMqmg7FcLLI3uqibtDJpJoytY4ahMrhFuuh8JuDmRIhOhWvCnFw1HalS6EkuBrWtJUKjrbqmKMr6jiVP1paF1mtBUJhUUfOgMot04g15AB8Ih8ZrKfBra9xBWv88e9RaJ8GWBd7SqKD5Lx2ki3Zl7y0bWhCBweEXV2Erv_GIduxfVcnv0d8SIwFkCYAroW0hM2aSU4HzdiY3ccIiBCbIXEpyr81WH4kh3bzvEj8SsQaTkxPQ5CFGOyIhSOVKNjGTe4sxkh8kKkp6hOQ"
                    }
                  ]
                }""";

    final PropertyValidator jwksValidator = this.propertyValidators.builder(VariableValueResolver.defaultResolver())
        .json()
        .jwks()
        .build();

    assertThrows(PropertyValidationFailException.class, () -> jwksValidator.validate("jwks", jwks));
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
  public void testResolveValidator_entityIdValidator() {
    final PropertyValidator result = resolveValidator("entityid");
    PropertyValidationFailException ex =
        assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "abc"));
    ex = assertThrows(PropertyValidationFailException.class,
        () -> result.validate("key", "http://example.com/entityid"));
    ex = assertThrows(PropertyValidationFailException.class,
        () -> result.validate("key", "https://example.com/entityid?query=123"));
    ex = assertThrows(PropertyValidationFailException.class,
        () -> result.validate("key", "https://example.com/entityid#fragment"));
    assertDoesNotThrow(() -> result.validate("key", "https://example.com/entityid"));
    assertDoesNotThrow(() -> result.validate("key", "https://example.com/"));
    assertDoesNotThrow(() -> result.validate("key", "https://example.com"));
  }

  @Test
  public void testResolveValidator_startsWithValidator() {
    final PropertyValidator result = resolveValidator("starts_with:http://www.digg.se");
    assertThrows(PropertyValidationFailException.class, () -> result.validate("key", "http://www.novalidate.se"));
    assertDoesNotThrow(() -> result.validate("key", "http://www.digg.se"));
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
    return this.propertyValidators.resolveValidator(validatorString, VariableValueResolver.defaultResolver());
  }

  public static JWK genKey() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
      keyGen.initialize(Curve.P_256.toECParameterSpec());

      final KeyPair keyPair = keyGen.generateKeyPair();

      return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).privateKey(keyPair.getPrivate())
          .keyID("ec-key-id" + new Random().nextInt(10))
          .build();
    }
    catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }
}