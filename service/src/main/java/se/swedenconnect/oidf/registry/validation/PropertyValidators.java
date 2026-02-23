/*
 * Copyright 2026 Sweden Connect
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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static final JsonMapper mapper = new JsonMapper();
  private final Map<String, PropertyValidatorPlugin> validatorRegistry = new HashMap<>();

  /**
   * Creates a new instance of ValidationBuilder initialized with the provided VariableValueResolver.
   *
   * @param variabelResolver the resolver to be used for resolving variables in validation strings
   * @return a new ValidationBuilder instance configured with the specified resolver
   */
  public ValidationBuilder builder(final VariableValueResolver variabelResolver) {
    return new ValidationBuilder(variabelResolver);
  }

  /**
   * Registers a {@link PropertyValidatorPlugin} with the validator registry.
   *
   * @param validatorPlugin the validator plugin to register
   */
  public void registerValidator(final PropertyValidatorPlugin validatorPlugin) {
    this.validatorRegistry.put(validatorPlugin.name().toUpperCase(), validatorPlugin);
  }

  // ---------------------------------------------------------------------------
  // No-arg validators — each directly implements PropertyValidator.validate()
  // and can be used as a method reference (this::validateXxx)
  // ---------------------------------------------------------------------------

  private ValidationStatus validateDuration(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "duration");
    }
    try {
      java.time.Duration.parse(value);
    }
    catch (final Exception ex) {
      throw new PropertyValidationFailException(key, value, "Invalid duration format. "
          + "Expected ISO-8601 duration (e.g., PT1H30M). Error: " + ex.getMessage());
    }
    return ValidationStatusImpl.ok(key, "duration");
  }

  private ValidationStatus validateDate(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "date");
    }
    try {
      java.time.LocalDate.parse(value);
    }
    catch (final Exception e) {
      throw new PropertyValidationFailException(key, value, "Invalid date format. Use YYYY-MM-DD");
    }
    return ValidationStatusImpl.ok(key, "date(YYYY-MM-DD)");
  }

  private ValidationStatus validateUrl(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "url");
    }
    try {
      new URI(value).toURL();
    }
    catch (final Exception e) {
      throw new PropertyValidationFailException(key, value, "Invalid URL format");
    }
    return ValidationStatusImpl.ok(key, "url");
  }

  private ValidationStatus validateEntityID(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "entityid");
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
    return ValidationStatusImpl.ok(key, "entityid");
  }

  private ValidationStatus validateUUID(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "uuid");
    }
    try {
      UUID.fromString(value);
    }
    catch (final IllegalArgumentException e) {
      throw new PropertyValidationFailException(key, value, "Invalid UUID format");
    }
    return ValidationStatusImpl.ok(key, "uuid");
  }

  private ValidationStatus validateJson(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "json");
    }
    try {
      mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
    }
    catch (final Exception e) {
      throw new PropertyValidationFailException(key, value, "Value is not a valid JSON: " + e.getMessage());
    }
    return ValidationStatusImpl.ok(key, "json");
  }

  private ValidationStatus validateJWK(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "jwk");
    }
    try {
      JWK.parse(value);
    }
    catch (final ParseException e) {
      throw new PropertyValidationFailException(key, value, "Value is not a valid JWK: " + e.getMessage());
    }
    return ValidationStatusImpl.ok(key, "jwk");
  }

  private ValidationStatus validateJWT(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "jwt");
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
    return ValidationStatusImpl.ok(key, "jwt");
  }

  /**
   * Validates that the value is a valid OIDF (OpenID Federation) policy JSON structure using MetadataPolicy.parse()
   * from Nimbus OIDC SDK.
   */
  private ValidationStatus validateOidfPolicy(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, "oidfpolicy");
    }
    final JsonNode node = mapper.readTree(value);
    if (node.isEmpty()) {
      return ValidationStatusImpl.noEval(key, "oidfpolicy");
    }

    final String rootNodeName = "metadata_policy";
    final JsonNode metadataPolicyNode = node.get(rootNodeName);
    if (metadataPolicyNode == null) {
      throw new PropertyValidationFailException(key, value,
          "Expected a start node of type: " + rootNodeName);
    }
    metadataPolicyNode.propertyStream().forEach(s -> {
      try {
        MetadataPolicy.parse(s.getValue().toPrettyString());
      }
      catch (com.nimbusds.oauth2.sdk.ParseException | PolicyViolationException e) {
        throw new PropertyValidationFailException(
            key + "." + rootNodeName + "." + s.getKey() + "."
                + s.getValue().propertyNames().stream().findFirst().orElse("<novalue>"),
            value,
            "Invalid OIDF metadata policy structure: " + e.getMessage());
      }
    });
    return ValidationStatusImpl.ok(key, "oidfpolicy");
  }

  private ValidationStatus validateRequired(final String key, final String value) {
    this.throwIf(() -> value == null || value.isBlank(), key, "Field is required");
    return ValidationStatusImpl.ok(key, "required");
  }

  private ValidationStatus validateEmail(final String key, final String value) {
    this.throwIf(() -> value != null && !value.isBlank() && !value.matches("^[A-Za-z0-9+_.-]+@(.+)$"),
        key, "Invalid email format", value);
    return ValidationStatusImpl.ok(key, "email");
  }

  private ValidationStatus validateAlpha(final String key, final String value) {
    this.throwIf(() -> value != null && !value.isBlank() && !value.matches("^[A-Za-z]+$"),
        key, "Must contain only letters", value);
    return ValidationStatusImpl.ok(key, "alpha");
  }

  private ValidationStatus validateAlphanumeric(final String key, final String value) {
    this.throwIf(() -> value != null && !value.isBlank() && !value.matches("^[A-Za-z0-9]+$"),
        key, "Must contain only letters and numbers", value);
    return ValidationStatusImpl.ok(key, "alphanumeric");
  }

  private ValidationStatus validateNumber(final String key, final String value) {
    this.throwIf(() -> value != null && !value.isBlank() && !value.matches("^-?\\d*\\.?\\d+$"),
        key, "Must be a number", value);
    return ValidationStatusImpl.ok(key, "number");
  }

  // ---------------------------------------------------------------------------
  // Parameterized validators — factory methods that close over configuration
  // ---------------------------------------------------------------------------

  private PropertyValidator validateEndsWith(final String suffix) {
    if (suffix == null || suffix.isBlank()) {
      throw new IllegalArgumentException("ends_with validator requires a suffix");
    }
    return (key, value) -> {
      this.throwIf(() -> !value.isBlank() && !value.endsWith(suffix), key, "Must end with: " + suffix);
      return ValidationStatusImpl.ok(key, "endsWith(%s)".formatted(suffix));
    };
  }

  private PropertyValidator validateStartsWith(final String prefix) {
    if (prefix == null || prefix.isBlank()) {
      throw new IllegalArgumentException("starts_with validator requires a prefix");
    }
    return (key, value) -> {
      this.throwIf(() -> !value.isBlank() && !value.startsWith(prefix), key,
          "Must start with: " + prefix, value);
      return ValidationStatusImpl.ok(key, "startsWith(%s)".formatted(prefix));
    };
  }

  private PropertyValidator validateContains(final String substring) {
    if (substring == null || substring.isBlank()) {
      throw new IllegalArgumentException("contains validator requires a substring");
    }
    return (key, value) -> {
      this.throwIf(() -> !value.isBlank() && !value.contains(substring), key, "Must contain: " + substring, value);
      return ValidationStatusImpl.ok(key, "contains(%s)".formatted(substring));

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
          return ValidationStatusImpl.noEval(key, "between");
        }
        final double number = Double.parseDouble(value);
        this.throwIf(() -> number < min || number > max,
            key, "Value must be between %s and %s".formatted(min, max));
        return ValidationStatusImpl.ok(key, "between(%s,%s)".formatted(min,max));


      };
    }
    catch (final NumberFormatException e) {
      throw new IllegalArgumentException("between validator requires numeric values");
    }
  }

  private PropertyValidator validateMatches(final String regex) {
    if (regex == null || regex.isBlank()) {
      throw new IllegalArgumentException("matches validator requires a regex pattern");
    }
    try {
      final Pattern pattern = Pattern.compile(regex);
      return (key, value) -> {
        this.throwIf(() -> !value.isBlank() && !pattern.matcher(value).matches(),
            key, "Value does not match required pattern: " + regex, value);
        return ValidationStatusImpl.ok(key, "matches(%s)".formatted(regex));
      };
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
        return ValidationStatusImpl.noEval(key, "length");
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
      return ValidationStatusImpl.ok(key, "length(%s)".formatted(conf));

    };
  }

  private PropertyValidator validateJwks(final String conf) {
    final boolean isPublicCheck = "public".equalsIgnoreCase(conf) || conf.isBlank();
    final boolean hasKidCheck = "kid".equalsIgnoreCase(conf) || conf.isBlank();
    final boolean hasKeysCheck = "req".equalsIgnoreCase(conf) || conf.isBlank();
    return (key, value) -> {
      if (value == null || value.isBlank()) {
        return ValidationStatusImpl.noEval(key, "jwks");
      }
      try {
        final JsonNode node = mapper.readTree(value);
        final JsonNode keys = node.get("keys");
        if (hasKeysCheck && (keys == null || !keys.iterator().hasNext())) {
          throw new PropertyValidationFailException(key, value, "keys element is expected");
        }
        if (!keys.isArray()) {
          throw new PropertyValidationFailException(key, value, "keys element is expected to be an array");
        }
        final Map<String, Object> kidDuplicateCheck = new HashMap<>();
        keys.iterator().forEachRemaining(keyNode -> {
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
      catch (final JacksonException e) {
        throw new PropertyValidationFailException(key, value, "Value is not a valid JSON. " + e.getMessage());
      }
      return ValidationStatusImpl.ok(key, "jwks(%s)".formatted(conf));
    };
  }

  private PropertyValidator validateMin(final String conf) {
    return (key, value) -> {
      this.throwIf(() -> value != null && !value.isBlank() && Double.parseDouble(value) < Double.parseDouble(conf),
          key, "Value has to be greater than %s".formatted(conf), value);
      return ValidationStatusImpl.ok(key, "min(%s)".formatted(conf));
    };
  }

  private PropertyValidator validateMax(final String conf) {
    return (key, value) -> {
      this.throwIf(() -> value != null && !value.isBlank() && Double.parseDouble(value) > Double.parseDouble(conf),
          key, "Value has to be less than %s".formatted(conf), value);
      return ValidationStatusImpl.ok(key, "max(%s)".formatted(conf));
    };
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

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
            "Length limits cannot be negative. Got min: %d, max: %d".formatted(minLength, maxLength));
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

  // ---------------------------------------------------------------------------
  // Builder
  // ---------------------------------------------------------------------------

  /**
   * A builder class for constructing a composite {@link PropertyValidator} by chaining validation rules. Each method
   * adds a validator directly to an internal list; {@link #build()} composes them without any string round-trip.
   */
  public class ValidationBuilder {

    final VariableValueResolver variabelResolver;
    private final List<PropertyValidator> validators = new ArrayList<>();

    private ValidationBuilder(final VariableValueResolver variabelResolver) {
      this.variabelResolver = variabelResolver;
    }

    private void addValidator(final PropertyValidator validator) {
      this.validators.add(validator);
    }


    /**
     * Adds a UUID validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder uuid() {
      this.addValidator(PropertyValidators.this::validateUUID);
      return this;
    }

    /**
     * Adds a length validation rule with the specified minimum and maximum lengths.
     *
     * @param min the minimum allowable length
     * @param max the maximum allowable length
     * @return this builder
     */
    public ValidationBuilder length(final int min, final int max) {
      this.addValidator(PropertyValidators.this.hasLength(min + "," + max));
      return this;
    }

    /**
     * Adds a JSON validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder json() {
      this.addValidator(PropertyValidators.this::validateJson);
      return this;
    }

    /**
     * Adds a JWKS (JSON Web Key Set) validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder jwks() {
      this.addValidator(PropertyValidators.this.validateJwks(""));
      return this;
    }

    /**
     * Adds a JWK (JSON Web Key) validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder jwk() {
      this.addValidator(PropertyValidators.this::validateJWK);
      return this;
    }

    /**
     * Adds an "ends with" validation rule using the specified suffix.
     *
     * @param suffix the required suffix
     * @return this builder
     */
    public ValidationBuilder endsWith(final String suffix) {
      this.addValidator(PropertyValidators.this.validateEndsWith(this.variabelResolver.insertTemplateValues(suffix)));
      return this;
    }

    /**
     * Adds a "starts with" validation rule using the specified prefix.
     *
     * @param prefix the required prefix
     * @return this builder
     */
    public ValidationBuilder startsWith(final String prefix) {
      this.addValidator(PropertyValidators.this.validateStartsWith(this.variabelResolver.insertTemplateValues(prefix)));
      return this;
    }

    /**
     * Adds a "contains" validation rule using the specified substring.
     *
     * @param substring the required substring
     * @return this builder
     */
    public ValidationBuilder contains(final String substring) {
      this.addValidator(PropertyValidators.this.validateContains(
          this.variabelResolver.insertTemplateValues(substring)));
      return this;
    }

    /**
     * Adds a date validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder date() {
      this.addValidator(PropertyValidators.this::validateDate);
      return this;
    }

    /**
     * Adds a "between" validation rule enforcing an inclusive numeric range.
     *
     * @param min the minimum allowable value
     * @param max the maximum allowable value
     * @return this builder
     */
    public ValidationBuilder between(final int min, final int max) {
      this.addValidator(PropertyValidators.this.validateBetween(min + "," + max));
      return this;
    }

    /**
     * Adds a URL validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder url() {
      this.addValidator(PropertyValidators.this::validateUrl);
      return this;
    }

    /**
     * Adds an entity identifier (entity ID) validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder entityid() {
      this.addValidator(PropertyValidators.this::validateEntityID);
      return this;
    }

    /**
     * Adds a JWT validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder jwt() {
      this.addValidator(PropertyValidators.this::validateJWT);
      return this;
    }

    /**
     * Adds a regex match validation rule.
     *
     * @param regex the pattern the value must match
     * @return this builder
     */
    public ValidationBuilder matches(final String regex) {
      this.addValidator(PropertyValidators.this.validateMatches(this.variabelResolver.insertTemplateValues(regex)));
      return this;
    }

    /**
     * Adds a duration validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder duration() {
      this.addValidator(PropertyValidators.this::validateDuration);
      return this;
    }

    /**
     * Adds a required (non-null, non-blank) validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder required() {
      this.addValidator(PropertyValidators.this::validateRequired);
      return this;
    }

    /**
     * Adds an email address validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder email() {
      this.addValidator(PropertyValidators.this::validateEmail);
      return this;
    }

    /**
     * Adds an alphabetic characters only validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder alpha() {
      this.addValidator(PropertyValidators.this::validateAlpha);
      return this;
    }

    /**
     * Adds an alphanumeric characters only validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder alphanumeric() {
      this.addValidator(PropertyValidators.this::validateAlphanumeric);
      return this;
    }

    /**
     * Adds a numeric value validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder number() {
      this.addValidator(PropertyValidators.this::validateNumber);
      return this;
    }

    /**
     * Adds a minimum value validation rule.
     *
     * @param min the minimum allowable value
     * @return this builder
     */
    public ValidationBuilder min(final int min) {
      this.addValidator(PropertyValidators.this.validateMin(String.valueOf(min)));
      return this;
    }

    /**
     * Adds a maximum value validation rule.
     *
     * @param max the maximum allowable value
     * @return this builder
     */
    public ValidationBuilder max(final int max) {
      this.addValidator(PropertyValidators.this.validateMax(String.valueOf(max)));
      return this;
    }

    /**
     * Adds a ping validation rule resolved from the plugin registry at validation time.
     *
     * @return this builder
     */
    public ValidationBuilder ping() {
      this.addValidator((key, value) -> {
        final PropertyValidator pv = PropertyValidators.this.validatorRegistry.get("PING");
        if (pv != null) {
          return pv.validate(key, value);
        }
        return ValidationStatusImpl.noEval(key, "ping");
      });
      return this;
    }

    /**
     * Adds an OIDF (OpenID Federation) policy validation rule.
     *
     * @return this builder
     */
    public ValidationBuilder oidfPolicy() {
      this.addValidator(PropertyValidators.this::validateOidfPolicy);
      return this;
    }

    /**
     * Composes all accumulated validators into a single {@link PropertyValidator}. The returned validator collects
     * {@link ValidationStatus} results from each rule and aggregates their statuses.
     *
     * @return a composite validator that runs each added rule in order
     */
    public PropertyValidator build() {
      final List<PropertyValidator> snapshot = List.copyOf(this.validators);
      this.validators.clear();
      return (key, value) -> {
        final List<ValidationStatus> statuses = new ArrayList<>(snapshot.size());
        for (final PropertyValidator v : snapshot) {
          statuses.add(v.validate(key, value));
        }
        return new ValidationStatusImpl(statuses);
      };
    }
  }

}
