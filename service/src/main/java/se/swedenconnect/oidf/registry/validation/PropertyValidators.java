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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
   * Resolves and constructs a composite PropertyValidator based on the provided configuration string and a
   * variable value resolver. The method parses the given validator configuration string, initializing and
   * combining multiple individual validators into a single aggregated validator function.
   *
   * @param validatorNameSetting the configuration string representing the validators, where individual
   *                             validators are separated by a pipe ('|') character. If null or blank, a no-op
   *                             validator is returned.
   * @param variabelResolver     the resolver used for resolving dynamic variable values in the configuration string.
   * @return a PropertyValidator that applies all resolved validation rules in sequence. If the configuration string
   *         is null or blank, a no-op validator is returned.
   */
  public PropertyValidator resolveValidator(final String validatorNameSetting,
      final VariabelValueResolver variabelResolver) {
    log.debug("Resolving validators: {}", validatorNameSetting);

    if (validatorNameSetting == null || validatorNameSetting.isBlank()) {
      log.debug("No validators found");
      return (String key, String value) -> {};
    }

    final List<PropertyValidator> validatorsList = Arrays.stream(validatorNameSetting.split("\\|"))
        .map(s -> this.propertyValidatorCreator(variabelResolver, s))
        .filter(Objects::nonNull)
        .toList();

    return (String key, String value) -> validatorsList
        .forEach(propertyValidator -> propertyValidator.validate(key, value));

  }

  /**
   * Determines if the specified validator is supported by checking if the provided validator name matches a value in
   * the {@code ValidationType} enumeration.
   *
   * @param validatorNameSetting the name of the validator to check
   * @return {@code true} if the validator is supported, {@code false} otherwise
   */
  public boolean isValidatorSupported(final String validatorNameSetting) {
    if (validatorNameSetting == null || validatorNameSetting.isBlank()) {
      return false;
    }

    try {
      final String[] split = validatorNameSetting.trim().toLowerCase().split(":");
      final String name = split[0];
      ValidationType.valueOf(name.toUpperCase());
    }
    catch (final IllegalArgumentException e) {
      return false;
    }
    return true;
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
  protected PropertyValidator propertyValidatorCreator(final VariabelValueResolver variabelResolver,
      final String validatorNameSetting) {
    log.debug("Creating validator: {}", validatorNameSetting);
    final String[] split = validatorNameSetting.trim().toLowerCase().split(":");
    final String name = split[0];
    final String conf = variabelResolver.insertTemplateValues(split.length > 1 ? split[1] : "");

    final ValidationType validationType;
    try {
      validationType = ValidationType.valueOf(name.toUpperCase());
    }
    catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown validator: " + name, e);
    }

    return switch (validationType) {

      case UUID -> this.isUUID();
      case LENGTH -> this.hasLength(conf);
      case JSON -> this.isJson();
      case JWKS -> this.jwksValidator(conf);
      case JWK -> this.isJWK();
      case ENDS_WITH -> this.validateEndsWith(conf);
      case STARTS_WITH -> this.validateStartsWith(conf);
      case CONTAINS -> this.validateContains(conf);
      case DATE -> this.validateDate();
      case BETWEEN -> this.validateBetween(conf);
      case URL -> this.validateUrl();
      case JWT -> this.isJWT();
      case MATCHES -> this.validateMatches(conf);
      case DURATION -> this.validateDuration();

      case REQUIRED -> (key, value) ->
          this.throwIf(() -> value == null || value.isBlank(),
              key, "Field is required");

      case EMAIL -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^[A-Za-z0-9+_.-]+@(.+)$"),
              key, "Invalid email format");

      case ALPHA -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^[A-Za-z]+$"),
              key, "Must contain only letters");

      case ALPHANUMERIC -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^[A-Za-z0-9]+$"),
              key, "Must contain only letters and numbers");

      case NUMBER -> (key, value) ->
          this.throwIf(() -> !value.isBlank() &&
                  !value.matches("^-?\\d*\\.?\\d+$"),
              key, "Must be a number");

      case MIN -> (String key, String value) -> this.throwIf(() ->
              !value.isBlank() && Double.parseDouble(value) < Double.parseDouble(conf),
          key, "Value has to be greater than %s".formatted(conf));

      case MAX -> (String key, String value) -> this.throwIf(() ->
              !value.isBlank() && Double.parseDouble(value) > Double.parseDouble(conf),
          key, "Value has to be less than %s".formatted(conf));

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
        throw new PropertyValidationFailException(key, value, "Invalid duration format. "
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
        throw new PropertyValidationFailException(key, value, "Invalid date format. Use YYYY-MM-DD");
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
      final Pattern pattern = Pattern.compile(regex);
      return (key, value) -> this.throwIf(
          () -> !value.isBlank() && !pattern.matcher(value).matches(),
          key, "Value does not match required pattern"
      );
    }
    catch (final PatternSyntaxException e) {
      throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
    }
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
        throw new PropertyValidationFailException(key, value,
            "Value has to be at least %d characters long".formatted(minLength));
      }
      if (length > maxLength) {
        throw new PropertyValidationFailException(key, value,
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
        throw new PropertyValidationFailException(key, value, "Value is not a valid URL: " + e.getMessage());
      }
    };
  }

  private PropertyValidator isJson() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }
      try {
        mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
      }
      catch (final Exception e) {
        throw new PropertyValidationFailException(key, value, "Value is not a valid JSON: " + e.getMessage());
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
        throw new PropertyValidationFailException(key, value, "Value is not a valid JWK: " + e.getMessage());
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
        throw new PropertyValidationFailException(key, value, "Value is not a valid JWT: " + e.getMessage());
      }
    };
  }

  private PropertyValidator jwksValidator(final String conf) {

    final boolean isPublicCheck = "public".equalsIgnoreCase(conf) || conf.isBlank();
    final boolean hasKidCheck = "kid".equalsIgnoreCase(conf) || conf.isBlank();
    final boolean hasKeysCheck = "req".equalsIgnoreCase(conf) || conf.isBlank();

    return (key, value) -> {
      try {
        if (value == null || value.isBlank()) {
          return;
        }
        final JsonNode node = mapper.readTree(value);
        final JsonNode keys = node.get("keys");

        if (hasKeysCheck && (keys == null || !keys.elements().hasNext())) {
          throw new PropertyValidationFailException(key, value, "keys element is expected");
        }

        if (!keys.isArray()) {
          throw new PropertyValidationFailException(key, value, "keys element is expected to be an array");
        }

        keys.elements().forEachRemaining(keyNode -> {

          if (hasKidCheck && !keyNode.hasNonNull("kid")) {
            throw new PropertyValidationFailException(key, value, "No kid defined in key element. This is required.");
          }

          if (!isPublicCheck) {
            return;
          }

          try {
            final JWK jwk = JWK.parse(keyNode.toString());
            if (jwk.isPrivate()) {
              throw new PropertyValidationFailException(key, value, "Keys are expected to be public. But is private. ");
            }

          }
          catch (final ParseException e) {
            throw new PropertyValidationFailException(key, value, "Unable to parse key element. " + e.getMessage());
          }

        });
      }
      catch (final JsonProcessingException e) {
        throw new PropertyValidationFailException(key, value, "Value is not a valid JSON. " + e.getMessage());
      }

    };
  }

  private void throwIf(final Supplier<Boolean> predicate, final String keyName, final String failMessage) {
    if (predicate.get()) {
      throw new PropertyValidationFailException(keyName, failMessage);
    }
  }

  /**
   * This enumeration defines various types of validation rules that can be applied to data properties. Each constant
   * represents a specific validation method or criteria used to ensure the integrity and correctness of data values.
   */
  public enum ValidationType {
    UUID,
    LENGTH,
    JSON,
    JWKS,
    JWK,
    ENDS_WITH,
    STARTS_WITH,
    CONTAINS,
    DATE,
    BETWEEN,
    URL,
    JWT,
    MATCHES,
    DURATION,
    REQUIRED,
    EMAIL,
    ALPHA,
    ALPHANUMERIC,
    NUMBER,
    MIN,
    MAX
  }

}
