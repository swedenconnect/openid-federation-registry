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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.lang.reflect.Field;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 * Creating default test data
 *
 * @author Per Fredrik Plars
 */
public class OptionsTestData {

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @ToString
  public static class TrustAnchorTestData extends OptionsTestDataProvider {
    @Builder.Default
    UUID id = UUID.randomUUID();

    UUID entityId;
    @Builder.Default
    String active = "true";
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @ToString
  public static class HostedEntityTestData extends OptionsTestDataProvider {
    @Builder.Default
    UUID id = UUID.randomUUID();
    @Builder.Default
    UUID policyId = null;
    @Builder.Default
    String subject = "http://www.swedenconnect.se/test";
    @Builder.Default
    String issuer = "http://www.swedenconnect.se/test";
    String metadata;

  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @ToString
  public static class SubordinateEntityTestData extends OptionsTestDataProvider {
    @Builder.Default
    UUID id = UUID.randomUUID();
    @Builder.Default
    UUID policyId = null;
    @Builder.Default
    String jwks = genJwks();
    @Builder.Default
    String subject = "http://www.swedenconnect.se/subject";
    @Builder.Default
    String issuer = "http://www.swedenconnect.se/issuer";
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @ToString
  public static class PolicyTestData extends OptionsTestDataProvider {
    @Builder.Default
    UUID id = UUID.randomUUID();
    @Builder.Default
    String name = "Default Policy";
    @Builder.Default
    String policy = "{\n"
        + "    \"openid_provider\": {\n"
        + "      \"id_token_signing_alg_values_supported\":\n"
        + "        {\"subset_of\": [\"RS256\", \"RS384\", \"RS512\"]},\n"
        + "      \"op_policy_uri\": {\n"
        + "        \"regexp\":\n"
        + "          \"^https:\\/\\/[\\\\w-]+\\\\.example\\\\.com\\/[\\\\w-]+\\\\.html\"}\n"
        + "    },\n"
        + "    \"oauth_client\": {\n"
        + "      \"grant_types\": {\n"
        + "        \"one_of\": [\"authorization_code\", \"client_credentials\"]\n"
        + "      }\n"
        + "    }\n"
        + "  }";
  }

  public static abstract class OptionsTestDataProvider {
    public Function<Values, String> testData() {
      return values -> createFieldMap(this).get(values.getKey());
    }

    public void create() {

    }

    public void update() {

    }

    public void delete() {

    }

    public void load() {

    }

    public abstract UUID getId();
  }

  public static String genJwks() {
    final JWKSet jwkSet = new JWKSet(List.of(genKey(), genKey()));
    return jwkSet.toString();
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

  private static final Map<String, Map<String, String>> cache = new HashMap<>();
  public static Map<String, String> createFieldMap(Object obj) {
    Map<String, String> fieldMap = cache.get(obj.getClass().getName());
    if (fieldMap != null) {
      return fieldMap;
    }
    fieldMap = new HashMap<>();
    final Class<?> clazz = obj.getClass();
    for (Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true);
      try {
        final String fieldName = field.getName();
        final Object fieldValue = field.get(obj);
        fieldMap.put(toSnakeCase(fieldName), Optional.ofNullable(fieldValue).map(Object::toString).orElse(null));
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException("Reflection error while accessing field: " + field.getName(), e);
      }
    }
    return Collections.unmodifiableMap(fieldMap);
  }

  public static <T> T instantiateAndFill(Class<T> clazz, Map<String, Object> fieldMap) {
    try {
      T instance = clazz.getDeclaredConstructor().newInstance();

      for (Map.Entry<String, Object> entry : fieldMap.entrySet()) {
        String fieldName = toCamelCase(entry.getKey());
        String fieldValue = entry.getValue().toString();
        if (fieldValue == null || fieldValue.isBlank()) {
          continue;
        }
        try {
          Field field = clazz.getDeclaredField(fieldName);
          field.setAccessible(true);
          field.set(instance, castValues(field.getType(), fieldValue));
        }
        catch (NoSuchFieldException e) {
          System.out.println("Field '" + fieldName + "' does not exist in class " + clazz.getSimpleName());
        }
      }

      return instance;
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to create or populate object", e);
    }
  }

  private static Object castValues(Class classType, String value) {
    return switch (classType.getSimpleName()) {
      case "UUID" -> UUID.fromString(value);
      default -> value;
    };
  }

  private static String toCamelCase(String snakeCase) {
    StringBuilder camelCase = new StringBuilder();
    boolean toUpperCase = false;

    for (char c : snakeCase.toCharArray()) {
      if (c == '_') {
        toUpperCase = true;
      }
      else {
        if (toUpperCase) {
          camelCase.append(Character.toUpperCase(c));
          toUpperCase = false;
        }
        else {
          camelCase.append(c);
        }
      }
    }

    return camelCase.toString();
  }

  private static String toSnakeCase(String camelCase) {
    StringBuilder snakeCase = new StringBuilder();

    for (char c : camelCase.toCharArray()) {
      if (Character.isUpperCase(c)) {
        snakeCase.append('_');
        snakeCase.append(Character.toLowerCase(c));
      }
      else {
        snakeCase.append(c);
      }
    }
    return snakeCase.toString();
  }

}
