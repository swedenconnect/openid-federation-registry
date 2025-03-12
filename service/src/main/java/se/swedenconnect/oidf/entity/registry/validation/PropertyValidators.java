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
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The PropertyValidators class provides utility methods for creating and resolving property validators based on
 * configuration strings. These validators enforce various constraints and checks on input key-value pairs.
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
   * Creates and returns a PropertyValidator based on the provided validator name and configuration. The
   * validatorNameSetting parameter specifies the type of validator and its configuration, typically in the format
   * "validatorName:configuration".
   *
   * @param validatorNameSetting the type and configuration of the desired validator
   * @return the created PropertyValidator instance for the specified type and configuration
   * @throws IllegalArgumentException if the validator type specified in validatorNameSetting is unknown
   */
  protected PropertyValidator propertyValidatorCreator(final String validatorNameSetting) {
    log.debug("Creating validator: {}", validatorNameSetting);
    final String[] split = validatorNameSetting.trim().toLowerCase().split(":");
    final String name = split[0];
    final String conf = split.length > 1 ? split[1] : "";

    return switch (name) {

      case "uuid" -> this.isUUID();
      case "length" -> this.hasLength(conf);
      case "json" -> this.isJson();
      case "jwks" -> this.jwksValidator(conf);
      case "jwk" -> this.isJWK();
      case "ends_with" -> this.validateEndsWith(conf);
      case "starts_with" -> this.validateStartsWith(conf);
      case "contains" -> this.validateContains(conf);
      case "date" -> this.validateDate();
      case "between" -> this.validateBetween(conf);
      case "url" -> this.validateUrl();
      case "jwt" -> this.isJWT();
      case "matches" -> this.validateMatches(conf);
      case "duration" -> this.validateDuration();

      case "required" -> (key, value) ->
          this.throwIf(() -> value == null || value.isBlank(),
              key, "Field is required");

      case "email" -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^[A-Za-z0-9+_.-]+@(.+)$"),
              key, "Invalid email format");

      case "alpha" -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^[A-Za-z]+$"),
              key, "Must contain only letters");

      case "alphanumeric" -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^[A-Za-z0-9]+$"),
              key, "Must contain only letters and numbers");

      case "number" -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^-?\\d*\\.?\\d+$"),
              key, "Must be a number");

      case "min" -> (String key, String value) -> this.throwIf(() ->
              !value.isBlank() && Double.parseDouble(value) < Double.parseDouble(conf),
          key, "Value has to be grater then %s".formatted(conf));

      case "max" -> (String key, String value) -> this.throwIf(() ->
              !value.isBlank() && Double.parseDouble(value) > Double.parseDouble(conf),
          key, "Value has to be less then %s".formatted(conf));

      default -> throw new IllegalArgumentException("Unknown validator: " + name);
    };
  }

  private PropertyValidator validateDuration() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        java.time.Duration.parse(value); // Parses ISO-8601 duration format
      }
      catch (final Exception ex) {
        throw new PropertyValidationFailException(key, "Invalid duration format. "
            + "Expected ISO-8601 duration (e.g., PT1H30M). Error: " + ex.getMessage());
      }
    };
  }

  private PropertyValidator validateEndsWith(final String suffix) {
    if (suffix == null || suffix.isBlank()) {
      throw new IllegalArgumentException("ends_with validator requires a suffix");
    }
    return (key, value) -> this.throwIf(
        () -> !value.isBlank() && !value.endsWith(suffix),
        key, "Must end with: " + suffix
    );
  }

  private PropertyValidator validateStartsWith(final String prefix) {
    if (prefix == null || prefix.isBlank()) {
      throw new IllegalArgumentException("starts_with validator requires a prefix");
    }
    return (key, value) -> this.throwIf(
        () -> !value.isBlank() && !value.startsWith(prefix),
        key, "Must start with: " + prefix
    );
  }

  private PropertyValidator validateContains(final String substring) {
    if (substring == null || substring.isBlank()) {
      throw new IllegalArgumentException("contains validator requires a substring");
    }
    return (key, value) -> this.throwIf(
        () -> !value.isBlank() && !value.contains(substring),
        key, "Must contain: " + substring
    );
  }

  private PropertyValidator validateDate() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        java.time.LocalDate.parse(value);
      }
      catch (final Exception e) {
        throw new PropertyValidationFailException(key, "Invalid date format. Use YYYY-MM-DD");
      }
    };
  }

  private PropertyValidator validateBetween(final String conf) {
    if (conf == null || !conf.contains(",")) {
      throw new IllegalArgumentException("between validator requires min,max format");
    }
    final String[] parts = conf.split(",");
    if (parts.length != 2) {
      throw new IllegalArgumentException("between validator requires exactly two values");
    }
    try {
      final double min = Double.parseDouble(parts[0]);
      final double max = Double.parseDouble(parts[1]);
      return (key, value) -> {
        if (value == null || value.isBlank()) {
          return;
        }
        final double number = Double.parseDouble(value);
        this.throwIf(
            () -> number < min || number > max,
            key, "Value must be between %s and %s".formatted(min, max)
        );
      };
    }
    catch (final NumberFormatException e) {
      throw new IllegalArgumentException("between validator requires numeric values");
    }
  }

  private PropertyValidator validateUrl() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        new URI(value).toURL();
      }
      catch (final Exception e) {
        throw new PropertyValidationFailException(key, value, "Invalid URL format");
      }
    };
  }

  private PropertyValidator isUUID() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        UUID.fromString(value);
      }
      catch (final IllegalArgumentException e) {
        throw new PropertyValidationFailException(key, value, "Invalid UUID format");
      }
    };
  }

  private PropertyValidator validateMatches(final String regex) {
    if (regex == null || regex.isBlank()) {
      throw new IllegalArgumentException("matches validator requires a regex pattern");
    }
    try {
      Pattern.compile(regex);
    }
    catch (final PatternSyntaxException e) {
      throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
    }
    return (key, value) -> this.throwIf(
        () -> !value.isBlank() && !value.matches(regex),
        key, "Value does not match required pattern"
    );
  }

  private PropertyValidator hasLength(final String conf) {
    final int[] limits = this.validateLengthConfiguration(conf);
    final int minLength = limits[0];
    final int maxLength = limits[1];

    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }

      final int length = value.length();
      if (length < minLength) {
        throw new PropertyValidationFailException(key,
            "Value has to be at least %d characters long".formatted(minLength));
      }
      if (length > maxLength) {
        throw new PropertyValidationFailException(key,
            "Value cannot be longer than %d characters".formatted(maxLength));
      }
    };
  }

  private int[] validateLengthConfiguration(final String conf) {
    if (conf == null || conf.isBlank()) {
      throw new IllegalArgumentException("Length validator requires configuration in format 'min,max'");
    }

    final String[] limits = conf.split(",");
    if (limits.length != 2) {
      throw new IllegalArgumentException(
          "Invalid length validator configuration. Expected format 'min,max', got: '%s'"
              .formatted(conf));
    }

    try {
      final int minLength = Integer.parseInt(limits[0]);
      final int maxLength = Integer.parseInt(limits[1]);

      if (minLength < 0 || maxLength < 0) {
        throw new IllegalArgumentException(
            "Length limits cannot be negative. Got min: %d, max: %d"
                .formatted(minLength, maxLength));
      }

      if (minLength > maxLength) {
        throw new IllegalArgumentException(
            "Minimum length cannot be greater than maximum length. Got min: %d, max: %d"
                .formatted(minLength, maxLength));
      }

      return new int[] { minLength, maxLength };
    }
    catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid length validator configuration. Both values must be integers, got: '%s'"
              .formatted(conf));
    }
  }

  private PropertyValidator isUrl() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
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
      if (value == null || value.isBlank()) {
        return;
      }
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
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        JWK.parse(value);
      }
      catch (final ParseException e) {
        throw new PropertyValidationFailException(key, "Value is not a valid JWK: " + e.getMessage());
      }
    };
  }

  private PropertyValidator isJWT() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        SignedJWT.parse(value);
      }
      catch (final ParseException e) {
        throw new PropertyValidationFailException(key, "Value is not a valid JWT: " + e.getMessage());
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
