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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The PropertyValidators class provides utility methods for creating and resolving
 * property validators based on configuration strings. These validators enforce
 * various constraints and checks on input key-value pairs.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class PropertyValidators {

  public static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Resolves and creates a composite {@link PropertyValidator} from a given configuration string. Each validator is
   * created based on the provided string and executed sequentially.
   *
   * @param validatorNameSetting a string containing validator definitions separated by the pipe ('|') character. If
   *     the string is null or blank, a no-operation validator is returned.
   * @return a {@link PropertyValidator} that applies all the resolved validators. If no validators are resolved, a
   *     no-operation validator is returned.
   */
  public PropertyValidator resolveValidator(final String validatorNameSetting) {
    log.debug("Resolving validators: {}", validatorNameSetting);

    if (validatorNameSetting == null || validatorNameSetting.isBlank()) {
      log.debug("No validators found");
      return (String key, String value) -> {};
    }

    final List<PropertyValidator> validatorsList = Arrays.stream(validatorNameSetting.split("\\|"))
        .map(this::propertyValidatorCreator)
        .filter(Objects::nonNull)
        .toList();

    return (String key, String value) -> validatorsList
        .forEach(propertyValidator -> propertyValidator.validate(key, value));

  }

  /**
   * Creates and returns a PropertyValidator based on the provided validator name and configuration.
   * The validatorNameSetting parameter specifies the type of validator and its configuration,
   * typically in the format "validatorName:configuration".
   *
   * @param validatorNameSetting the type and configuration of the desired validator
   * @return the created PropertyValidator instance for the specified type and configuration
   * @throws IllegalArgumentException if the validator type specified in validatorNameSetting is unknown
   */
  protected PropertyValidator propertyValidatorCreator(final String validatorNameSetting) {
    log.debug("Creating validator: {}", validatorNameSetting);
    final String[] split = validatorNameSetting.trim().split(":");
    final String name = split[0];
    final String conf = split.length > 1 ? split[1] : "";

    return switch (name) {
      case "req" -> (String key, String value) -> {
        this.throwIf(() -> value == null || value.isBlank(), key, "Required validation failed");
      };
      case "regex" -> (String key, String value) -> this.throwIf(() ->
          !value.isBlank() && !value.matches(conf), key, "Regex validation failed");
      case "min" -> (String key, String value) -> this.throwIf(() ->
              !value.isBlank() && value.length() < Integer.parseInt(conf),
          key, "Value has to be grater then %s".formatted(conf));
      case "max" -> (String key, String value) -> this.throwIf(() ->
              !value.isBlank() && value.length() >= Integer.parseInt(conf),
          key, "Value has to be less then %s".formatted(conf));
      case "json" -> this.isJson();
      case "jwks" -> this.jwksValidator(conf);
      case "url" -> this.isUrl();
      case "jwk" -> this.isJWK();

      default -> throw new IllegalArgumentException("Unknown validator: " + name);
    };
  }

  private PropertyValidator isUrl() {
    return (key, value) -> {
      try {
        new URI(value).toURL();
      }
      catch (final Exception e) {
        throw new PropertyValidationFailException(key, "Value is not a valid URL: " + e.getMessage());
      }
    };
  }

  private PropertyValidator isJson() {
    return (key, value) -> {
      try {
        mapper.readTree(value);
      }
      catch (final Exception e) {
        throw new PropertyValidationFailException(key, "Value is not a valid JSON: " + e.getMessage());
      }
    };
  }

  private PropertyValidator isJWK() {
    return (key, value) -> {
      try {
        JWK.parse(value);
      }
      catch (final ParseException e) {
        throw new PropertyValidationFailException(key, "Value is not a valid JWK: " + e.getMessage());
      }
    };
  }

  private PropertyValidator jwksValidator(final String conf) {

    final boolean isPublicCheck = "public".equalsIgnoreCase(conf) || conf.isBlank();
    final boolean hasKidCheck = "kid".equalsIgnoreCase(conf) || conf.isBlank();
    final boolean hasKeysCheck = "req".equalsIgnoreCase(conf) || conf.isBlank();

    return (key, value) -> {
      try {
        final JsonNode node = mapper.readTree(value);

        final JsonNode keys = node.get("keys");
        if (!keys.isArray()) {
          throw new PropertyValidationFailException(key, "keys element is expected to be an array");
        }

        if (hasKeysCheck && !keys.elements().hasNext()) {
          throw new PropertyValidationFailException(key, "keys element is expected");
        }

        keys.elements().forEachRemaining(keyNode -> {

          if (hasKidCheck && !keyNode.hasNonNull("kid")) {
            throw new PropertyValidationFailException(key, "No kid defined in key element. This is required.");
          }

          if (!isPublicCheck) {
            return;
          }

          try {
            final JWK jwk = JWK.parse(keyNode.toString());
            if (jwk.isPrivate()) {
              throw new PropertyValidationFailException(key, "Keys are expected to be public. But is private. ");
            }

          }
          catch (final ParseException e) {
            throw new PropertyValidationFailException(key, "Unable to parse key element");
          }

        });
      }
      catch (final JsonProcessingException e) {
        throw new PropertyValidationFailException(key, "Value is not a valid JSON. " + e.getMessage());
      }

    };
  }

  private void throwIf(final Supplier<Boolean> predicate, final String keyName, final String failMessage) {
    if (predicate.get()) {
      throw new PropertyValidationFailException(keyName, failMessage);
    }
  }

}
