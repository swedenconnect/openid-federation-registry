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
package se.swedenconnect.oidf.registry.policy;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicyEntry;
import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperationApplication;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.EssentialOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.OneOfOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.SubsetOfOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.SupersetOfOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.ValueOperation;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.nimbus.policy.ExtendedMetadataPolicy;
import se.swedenconnect.oidf.nimbus.policy.ExtendedPolicyOperationCombinationValidator;
import se.swedenconnect.oidf.nimbus.policy.ExtendedValueOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks, combines and applies metadata policies, as stored: {@code {"metadata_policy": {<entity type>: {<parameter>:
 * {<operator>: <value>}}}}}.
 *
 * <p>Combining follows OpenID Federation: a superior's policy (the Trust Anchor's or Intermediate's) and the
 * subordinate's own policy for the same entity type are merged operator by operator, and a conflict refuses the
 * combination.</p>
 *
 * <p>{@code value} accepts any JSON value, including {@code null} to remove a parameter, and {@code one_of} accepts
 * strings, numbers and booleans.</p>
 *
 * @author Felix Hellman
 */
public final class MetadataPolicies {

  /** The root key of a stored policy. */
  public static final String ROOT = "metadata_policy";

  private static final Set<OperationName> SKIPPED_WITHOUT_VALUE =
      Set.of(OneOfOperation.NAME, SubsetOfOperation.NAME, SupersetOfOperation.NAME);

  private MetadataPolicies() {
  }

  /**
   * Checks a stored policy.
   *
   * @param policy the policy, with the {@value #ROOT} root key; {@code null} is no policy
   * @return what is wrong, one message each; empty if the policy can be used
   */
  public static List<String> problems(final Map<String, Object> policy) {
    if (policy == null || policy.isEmpty()) {
      return List.of();
    }
    if (!(policy.get(ROOT) instanceof final Map<?, ?> types)) {
      return List.of("A metadata policy needs the root key '%s' holding a JSON object".formatted(ROOT));
    }
    final List<String> problems = new ArrayList<>();
    types.forEach((type, parameters) -> {
      try {
        parse(String.valueOf(type), parameters);
      }
      catch (final IllegalArgumentException e) {
        problems.add(e.getMessage());
      }
    });
    return problems;
  }

  /**
   * Leaves out the parameters of a stored policy that the policy library cannot read. What is left out is reported, so
   * it can be logged.
   *
   * @param policy the policy, with the {@value #ROOT} root key
   * @return the policy without the parameters left out, and one message per parameter left out
   */
  public static Supported supportedOnly(final Map<String, Object> policy) {
    if (policy == null || !(policy.get(ROOT) instanceof final Map<?, ?> types)) {
      return new Supported(policy, List.of());
    }
    final List<String> omitted = new ArrayList<>();
    final Map<String, Object> kept = new LinkedHashMap<>();
    types.forEach((type, parameters) -> {
      if (!(parameters instanceof final Map<?, ?> map)) {
        kept.put(String.valueOf(type), parameters);
        return;
      }
      final Map<String, Object> keptParameters = new LinkedHashMap<>();
      map.forEach((parameter, operators) -> {
        final Map<String, Object> single = new LinkedHashMap<>();
        single.put(String.valueOf(parameter), operators);
        try {
          read(single);
          keptParameters.put(String.valueOf(parameter), operators);
        }
        catch (final ParseException | PolicyViolationException | RuntimeException e) {
          omitted.add("%s.%s (%s)".formatted(type, parameter, e.getMessage()));
        }
      });
      kept.put(String.valueOf(type), keptParameters);
    });
    return new Supported(wrap(kept), omitted);
  }

  /**
   * @param policy the policy that is kept, with the root key
   * @param omitted one message per parameter left out: entity type, parameter and why
   */
  public record Supported(Map<String, Object> policy, List<String> omitted) {
  }

  /**
   * Reads the policy of one entity type.
   *
   * @param parameters the policy, keyed by metadata parameter
   * @return the policy
   * @throws ParseException if the policy cannot be read
   * @throws PolicyViolationException if an operator is not supported or the operators cannot be combined
   */
  @SuppressWarnings("unchecked")
  public static MetadataPolicy read(final Map<?, ?> parameters) throws ParseException, PolicyViolationException {
    return ExtendedMetadataPolicy.parse(
        JSONObjectUtils.parse(JSONObject.toJSONString((Map<String, ? extends Object>) parameters)));
  }

  /**
   * Applies the policy of one entity type to metadata of that type. A parameter whose resolved value is {@code null}
   * is removed.
   *
   * @param policy the policy, see {@link #read(Map)}
   * @param metadata the metadata
   * @return the resolved metadata
   * @throws PolicyViolationException if the metadata does not satisfy the policy
   */
  public static JSONObject apply(final MetadataPolicy policy, final JSONObject metadata)
      throws PolicyViolationException {
    final JSONObject out = new JSONObject();
    metadata.forEach((key, value) -> {
      if (policy.getEntry(key) == null) {
        out.put(key, value);
      }
    });
    for (final MetadataPolicyEntry entry : policy.entrySet()) {
      final Object value = apply(entry, metadata.get(entry.getParameterName()));
      if (value != null) {
        out.put(entry.getParameterName(), value);
      }
    }
    return out;
  }

  /**
   * Applies the operations of one parameter in order, as {@link MetadataPolicyEntry#apply(Object)} does, letting the
   * extended operations apply themselves.
   */
  private static Object apply(final MetadataPolicyEntry entry, final Object value) throws PolicyViolationException {
    final Resolution resolution = new Resolution(value);
    try {
      entry.getOperationsMap().entrySet().stream()
          .takeWhile(operation -> !resolution.done)
          .forEach(operation -> resolution.apply(operation.getKey(), operation.getValue()));
    }
    catch (final UncheckedPolicyViolation e) {
      throw e.violation;
    }
    return resolution.updated;
  }

  private static Object applyOperation(final PolicyOperation operation, final Object value) {
    try {
      if (operation instanceof final ExtendedValueOperation extended) {
        return extended.apply(value);
      }
      return PolicyOperationApplication.apply(operation, value);
    }
    catch (final PolicyViolationException e) {
      throw new UncheckedPolicyViolation(e);
    }
  }

  /**
   * The state of one parameter while its operations are applied.
   */
  private static final class Resolution {

    private final Object value;
    private Object updated;
    private boolean essential;
    private boolean done;

    private Resolution(final Object value) {
      this.value = value;
      this.updated = value;
    }

    private void apply(final OperationName name, final PolicyOperation operation) {
      if (this.value == null && !this.essential && SKIPPED_WITHOUT_VALUE.contains(name)) {
        return;
      }
      this.updated = applyOperation(operation, this.updated);
      if (ValueOperation.NAME.equals(name)) {
        this.done = true;
        return;
      }
      if (EssentialOperation.NAME.equals(name)) {
        if (((EssentialOperation) operation).getBooleanConfiguration()) {
          this.essential = true;
        }
        else if (this.updated == null) {
          this.done = true;
          return;
        }
      }
      if (SubsetOfOperation.NAME.equals(name) && this.essential && this.updated == null) {
        throw new UncheckedPolicyViolation(
            new PolicyViolationException("Essential parameter failed subset_of check"));
      }
    }
  }

  private static final class UncheckedPolicyViolation extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final PolicyViolationException violation;

    private UncheckedPolicyViolation(final PolicyViolationException violation) {
      super(violation);
      this.violation = violation;
    }
  }

  /**
   * Combines a superior's policy with a subordinate's own policy.
   *
   * @param superior the Trust Anchor's or Intermediate's policy, with the root key; may be {@code null}
   * @param subordinate the subordinate's own policy, with the root key; may be {@code null}
   * @return the combined policy, with the root key; {@code null} if neither has one
   * @throws IllegalArgumentException if the policies cannot be combined, with the reason
   */
  public static Map<String, Object> combine(final Map<String, Object> superior, final Map<String, Object> subordinate) {
    final Map<String, Object> upper = typesOf(superior);
    final Map<String, Object> lower = typesOf(subordinate);
    if (upper.isEmpty()) {
      return lower.isEmpty() ? subordinate : wrap(lower);
    }
    if (lower.isEmpty()) {
      return wrap(upper);
    }
    final Set<String> types = new LinkedHashSet<>(upper.keySet());
    types.addAll(lower.keySet());
    final Map<String, Object> combined = new LinkedHashMap<>();
    for (final String type : types) {
      combined.put(type, combineType(type, upper.get(type), lower.get(type)));
    }
    return wrap(combined);
  }

  private static Object combineType(final String type, final Object upper, final Object lower) {
    if (upper == null) {
      return lower;
    }
    if (lower == null) {
      return upper;
    }
    try {
      return MetadataPolicy.combine(List.of(parse(type, upper), parse(type, lower)),
          new ExtendedPolicyOperationCombinationValidator()).toJSONObject();
    }
    catch (final PolicyViolationException e) {
      throw new IllegalArgumentException("The policies for %s cannot be combined: %s".formatted(type,
          e.getMessage()), e);
    }
  }

  private static MetadataPolicy parse(final String type, final Object parameters) {
    if (!(parameters instanceof final Map<?, ?> map)) {
      throw new IllegalArgumentException("The policy for %s must be a JSON object".formatted(type));
    }
    try {
      return read(map);
    }
    catch (final ParseException | PolicyViolationException | RuntimeException e) {
      throw new IllegalArgumentException("The policy for %s is not valid: %s".formatted(type, e.getMessage()), e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> typesOf(final Map<String, Object> policy) {
    return policy != null && policy.get(ROOT) instanceof final Map<?, ?> types && !types.isEmpty()
        ? (Map<String, Object>) types
        : Map.of();
  }

  private static Map<String, Object> wrap(final Map<String, Object> types) {
    final Map<String, Object> wrapped = new LinkedHashMap<>();
    wrapped.put(ROOT, types);
    return wrapped;
  }
}
