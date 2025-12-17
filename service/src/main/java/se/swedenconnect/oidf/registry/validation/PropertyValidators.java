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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
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
  private final Map<String, PropertyValidator> validatorCache = new HashMap<>();
  private final Map<String, PropertyValidatorPlugin> validatorRegistry = new HashMap<>();

  /**
   * Resolves and constructs a composite PropertyValidator based on the provided configuration string and a variable
   * value resolver. The method parses the given validator configuration string, initializing and combining multiple
   * individual validators into a single aggregated validator function.
   *
   * @param validatorNameSetting the configuration string representing the validators, where individual validators
   *     are separated by a pipe ('|') character. If null or blank, a no-op validator is returned.
   * @param variabelResolver the resolver used for resolving dynamic variable values in the configuration string.
   * @return a PropertyValidator that applies all resolved validation rules in sequence. If the configuration string is
   *     null or blank, a no-op validator is returned.
   */
  public PropertyValidator resolveValidator(final String validatorNameSetting,
      final VariableValueResolver variabelResolver) {
    log.debug("Resolving validators: {}", validatorNameSetting);

    if (validatorNameSetting == null || validatorNameSetting.isBlank()) {
      log.debug("No validators found");
      return (String key, String value) -> {};
    }
    if (this.validatorCache.containsKey(validatorNameSetting)) {
      return this.validatorCache.get(validatorNameSetting);
    }

    final List<PropertyValidator> validatorsList = Arrays.stream(validatorNameSetting.split("\\| "))
        .map(s -> this.propertyValidatorCreator(variabelResolver, s))
        .filter(Objects::nonNull)
        .toList();

    final PropertyValidator validator = (String key, String value) -> validatorsList
        .forEach(propertyValidator -> propertyValidator.validate(key, value));

    this.validatorCache.put(validatorNameSetting, validator);

    return validator;
  }

  /**
   * Registers a {@link PropertyValidatorPlugin} with the validator registry.
   *
   * @param validatorPlugin the validator plugin to register
   */
  public void registerValidator(final PropertyValidatorPlugin validatorPlugin) {
    this.validatorRegistry.put(validatorPlugin.name().toUpperCase(), validatorPlugin);
  }

  /**
   * Creates a new instance of ValidationStringBuilder initialized with the provided VariabelValueResolver.
   *
   * @param variabelResolver the resolver to be used for resolving variables in validation strings
   * @return a new ValidationStringBuilder instance configured with the specified resolver
   */
  public ValidationStringBuilder builder(final VariableValueResolver variabelResolver) {
    return new ValidationStringBuilder(variabelResolver);
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
    final String[] split = validatorNameSetting.trim().split(":");
    final String name = split[0].toUpperCase();
    try {

      ValidationType.valueOf(name);
    }
    catch (final IllegalArgumentException e) {
      return this.validatorRegistry.containsKey(name);
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
  protected PropertyValidator propertyValidatorCreator(final VariableValueResolver variabelResolver,
      final String validatorNameSetting) {
    log.debug("Creating validator: {}", validatorNameSetting);
    final String[] split = validatorNameSetting.trim().split(":", 2);
    final String name = split[0];
    final String conf = variabelResolver.insertTemplateValues(split.length > 1 ? split[1] : "");

    final ValidationType validationType;
    try {
      validationType = ValidationType.valueOf(name.toUpperCase());
    }
    catch (final IllegalArgumentException e) {
      if (this.validatorRegistry.containsKey(name)) {
        return this.validatorRegistry.get(name);
      }
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
      case ENTITYID -> this.validateEntityID();
      case JWT -> this.isJWT();
      case MATCHES -> this.validateMatches(conf);
      case DURATION -> this.validateDuration();

      case REQUIRED -> (key, value) ->
          this.throwIf(() -> value == null || value.isBlank(),
              key, "Field is required");

      case EMAIL -> (key, value) ->
          this.throwIf(() -> value != null && !value.isBlank() &&
                  !value.matches("^[A-Za-z0-9+_.-]+@(.+)$"),
              key, "Invalid email format", value);

      case ALPHA -> (key, value) ->
          this.throwIf(() -> value != null && !value.isBlank() &&
                  !value.matches("^[A-Za-z]+$"),
              key, "Must contain only letters", value);

      case ALPHANUMERIC -> (key, value) ->
          this.throwIf(() -> value != null && !value.isBlank() &&
                  !value.matches("^[A-Za-z0-9]+$"),
              key, "Must contain only letters and numbers", value);

      case NUMBER -> (key, value) ->
          this.throwIf(() -> value != null && !value.isBlank() &&
                  !value.matches("^-?\\d*\\.?\\d+$"),
              key, "Must be a number", value);

      case MIN -> (String key, String value) -> this.throwIf(() -> value != null &&
              !value.isBlank() && Double.parseDouble(value) < Double.parseDouble(conf),
          key, "Value has to be greater than %s".formatted(conf), value);

      case MAX -> (String key, String value) -> this.throwIf(() -> value != null &&
              !value.isBlank() && Double.parseDouble(value) > Double.parseDouble(conf),
          key, "Value has to be less than %s".formatted(conf), value);

      default -> null;
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
        key, "Must start with: " + prefix, value
    );
  }

  private PropertyValidator validateContains(final String substring) {
    if (substring == null || substring.isBlank()) {
      throw new IllegalArgumentException("contains validator requires a substring");
    }
    return (key, value) -> this.throwIf(
        () -> !value.isBlank() && !value.contains(substring),
        key, "Must contain: " + substring, value
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

  private PropertyValidator validateEntityID() {
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return;
      }

      try {
        final URL url = URI.create(value).toURL();

        this.throwIf(() -> url.getRef() != null && !url.getRef().isBlank(),
            key, "No fragments is allowed in entityID.", url.toString());

        this.throwIf(() -> url.getQuery() != null && !url.getQuery().isBlank(),
            key, "No query parameters allowed in entityID", value);

        this.throwIf(() -> !url.getProtocol().equalsIgnoreCase("https"),
            key, "EntityId has to use https protocol", value);
      }
      catch (final IllegalArgumentException | MalformedURLException e) {
        throw new PropertyValidationFailException(key, value, "Invalid entityid format: " + e.getMessage());
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
          key, "Value does not match required pattern: " + regex, value
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
        throw new PropertyValidationFailException(key,
            "Value has to be at least %d characters long".formatted(minLength), value);
      }
      if (length > maxLength) {
        throw new PropertyValidationFailException(key,
            "Value cannot be longer than %d characters".formatted(maxLength), value);
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
        final SignedJWT jwt = SignedJWT.parse(value);
        this.throwIf(() -> jwt == null, key, "Unable to parse JWT value");
        this.throwIf(() -> jwt.getHeader().toJSONObject() == null, key, "Unable to parse JWT header");
        this.throwIf(() -> jwt.getPayload().toJSONObject() == null, key, "Unable to parse JWT payload");
        this.throwIf(() -> jwt.getSignature().decode() == null, key, "Unable to parse JWT signature");
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

        final Map<String, Object> kidDuplicateCheck = new HashMap<>();

        keys.elements().forEachRemaining(keyNode -> {

          if (hasKidCheck && !keyNode.hasNonNull("kid")) {
            throw new PropertyValidationFailException(key, value, "No kid defined in key element. This is required.");
          }

          if (!isPublicCheck) {
            return;
          }
          final String kid = keyNode.get("kid").asText();
          if (kidDuplicateCheck.put(kid, "true") != null) {
            throw new PropertyValidationFailException(key, kid, "Kid is duplicated.");
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

  private void throwIf(final Supplier<Boolean> predicate, final String keyName, final String failMessage,
      final String orgValue) {
    if (predicate.get()) {
      throw new PropertyValidationFailException(keyName, orgValue, failMessage);
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
    ENTITYID,
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

  /**
   * A builder class for constructing validation rules used in property validation logic. This class enables method
   * chaining to define various constraints and rules, which will be combined into a final validation rule string.
   */
  public class ValidationStringBuilder {

    final VariableValueResolver variabelResolver;
    private final StringBuilder builder;

    // Private constructor to enforce the builder pattern
    private ValidationStringBuilder(final VariableValueResolver variabelResolver) {
      this.builder = new StringBuilder();
      this.variabelResolver = variabelResolver;
    }

    /**
     * Static method to initialize the builder.
     *
     * @return a new instance of ValidationStringBuilder
     */
    public ValidationStringBuilder uuid() {
      this.builder.append("UUID| ");
      return this;
    }

    /**
     * Adds a length validation rule with the specified minimum and maximum lengths to the builder.
     *
     * @param min the minimum allowable length for validation
     * @param max the maximum allowable length for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder length(final int min, final int max) {
      this.builder.append("LENGTH:").append(min).append(",").append(max).append("| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder json() {
      this.builder.append("JSON| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder jwks() {
      this.builder.append("JWKS| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder jwk() {
      this.builder.append("JWK| ");
      return this;
    }

    /**
     * Adds an "ends with" validation rule to the builder using the specified suffix. This rule ensures that the value
     * being validated must end with the given suffix.
     *
     * @param suffix the suffix that the value must end with for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder endsWith(final String suffix) {
      this.builder.append("ENDS_WITH:").append(suffix).append("| ");
      return this;
    }

    /**
     * Adds a "starts with" validation rule to the builder using the specified prefix. This rule ensures that the value
     * being validated must start with the given prefix.
     *
     * @param prefix the prefix that the value must start with for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder startsWith(final String prefix) {
      this.builder.append("STARTS_WITH:").append(prefix).append("| ");
      return this;
    }

    /**
     * Adds a "contains" validation rule to the builder using the specified substring. This rule ensures that the value
     * being validated must contain the given substring.
     *
     * @param substring the substring that must be present in the value for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder contains(final String substring) {
      this.builder.append("CONTAINS:").append(substring).append("| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder date() {
      this.builder.append("DATE| ");
      return this;
    }

    /**
     * Adds a "between" validation rule to the builder using the specified minimum and maximum values. This rule ensures
     * that the value being validated falls within the specified range, inclusive.
     *
     * @param min the minimum allowable value for the validation
     * @param max the maximum allowable value for the validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder between(final int min, final int max) {
      this.builder.append("BETWEEN:").append(min).append(",").append(max).append("| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder url() {
      this.builder.append("URL| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder entityid() {
      this.builder.append("ENTITYID| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder jwt() {
      this.builder.append("JWT| ");
      return this;
    }

    /**
     * Adds a "matches" validation rule to the builder using the specified regular expression. This rule ensures that
     * the value being validated must match the given regex pattern.
     *
     * @param regex the regular expression that the value must match for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder matches(final String regex) {
      this.builder.append("MATCHES:").append(regex).append("| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder duration() {
      this.builder.append("DURATION| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder required() {
      this.builder.append("REQUIRED| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder email() {
      this.builder.append("EMAIL| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder alpha() {
      this.builder.append("ALPHA| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder alphanumeric() {
      this.builder.append("ALPHANUMERIC| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder number() {
      this.builder.append("NUMBER| ");
      return this;
    }

    /**
     * Adds a minimum value validation rule to the builder.
     *
     * @param min the minimum allowable value for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder min(final int min) {
      this.builder.append("MIN:").append(min).append("| ");
      return this;
    }

    /**
     * Adds a maximum value validation rule to the builder.
     *
     * @param max the maximum allowable value for validation
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder max(final int max) {
      this.builder.append("MAX:").append(max).append("| ");
      return this;
    }

    /**
     * Creates a validator
     *
     * @return the current instance of {@code ValidationStringBuilder} for method chaining
     */
    public ValidationStringBuilder ping() {
      this.builder.append("PING:").append("| ");
      return this;
    }

    /**
     * Builds the final validation string and trims any trailing separator.
     *
     * @return the constructed validation string
     */
    public PropertyValidator build() {
      // Remove the trailing pipe character if it exists
      if (!this.builder.isEmpty() && this.builder.charAt(this.builder.length() - 1) == '|') {
        this.builder.setLength(this.builder.length() - 2);
      }
      final String builderResult = this.builder.toString();
      this.builder.setLength(0);
      return PropertyValidators.this.resolveValidator(builderResult, this.variabelResolver);
    }
  }

}
