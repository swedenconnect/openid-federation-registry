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
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks and combines metadata policies, as stored: {@code {"metadata_policy": {<entity type>: {<parameter>:
 * {<operator>: <value>}}}}}.
 *
 * <p>Combining follows OpenID Federation: a superior's policy (the Trust Anchor's or Intermediate's) and the
 * subordinate's own policy for the same entity type are merged operator by operator, and a conflict refuses the
 * combination.</p>
 *
 * <p>A parameter whose {@code value} operator is {@code null} must be removed from the metadata. The policy library
 * does not accept a {@code null} value, so such parameters are kept aside: they pass through unchanged, and combining
 * one with any other operator for the same parameter is a conflict.</p>
 *
 * @author Felix Hellman
 */
public final class MetadataPolicies {

  /** The root key of a stored policy. */
  public static final String ROOT = "metadata_policy";

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
   * Leaves out the parameters of a stored policy that the policy library cannot read yet, such as {@code value: null}
   * or {@code one_of} with values that are not strings. The policy is assumed to follow the specification; what is left
   * out is reported, so it can be logged.
   *
   * @param policy the policy, with the {@value #ROOT} root key
   * @return the policy without the parameters left out, and one message per parameter left out
   */
  @SuppressWarnings("unchecked")
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
          MetadataPolicy.parse(JSONObject.toJSONString(single));
          keptParameters.put(String.valueOf(parameter), operators);
        }
        catch (final ParseException | PolicyViolationException | RuntimeException e) {
          omitted.add("%s.%s (%s)".formatted(type, parameter, unsupportedReason(operators, e)));
        }
      });
      kept.put(String.valueOf(type), keptParameters);
    });
    return new Supported(wrap(kept), omitted);
  }

  /**
   * Says, for a person, why the policy library cannot read a parameter's operators.
   */
  private static String unsupportedReason(final Object operators, final Exception e) {
    if (operators instanceof final Map<?, ?> ops) {
      if (ops.containsKey("value") && ops.get("value") == null) {
        return "value null";
      }
      for (final Map.Entry<?, ?> op : ops.entrySet()) {
        if (op.getValue() instanceof final List<?> values && values.stream().anyMatch(v -> !(v instanceof String))) {
          return op.getKey() + " with values that are not strings";
        }
      }
    }
    return e.getMessage();
  }

  /**
   * A policy without the parameters the policy library cannot read yet.
   *
   * @param policy the policy that is kept, with the root key
   * @param omitted one message per parameter left out: entity type, parameter and why
   */
  public record Supported(Map<String, Object> policy, List<String> omitted) {
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
    final Split a = split(type, upper);
    final Split b = split(type, lower);
    for (final String removed : a.removed()) {
      if (b.rest().containsKey(removed)) {
        throw conflict(type, removed);
      }
    }
    for (final String removed : b.removed()) {
      if (a.rest().containsKey(removed)) {
        throw conflict(type, removed);
      }
    }
    final Map<String, Object> result = new LinkedHashMap<>();
    if (!a.rest().isEmpty() || !b.rest().isEmpty()) {
      try {
        result.putAll(MetadataPolicy.combine(List.of(parse(type, a.rest()), parse(type, b.rest()))).toJSONObject());
      }
      catch (final PolicyViolationException e) {
        throw new IllegalArgumentException("The policies for %s cannot be combined: %s".formatted(type,
            e.getMessage()), e);
      }
    }
    a.removed().forEach(parameter -> result.put(parameter, nullValue()));
    b.removed().forEach(parameter -> result.put(parameter, nullValue()));
    return result;
  }

  private static IllegalArgumentException conflict(final String type, final String parameter) {
    return new IllegalArgumentException(("The policies for %s cannot be combined: %s is removed (value null) by one "
        + "policy and has operators in the other").formatted(type, parameter));
  }

  private static MetadataPolicy parse(final String type, final Object parameters) {
    if (!(parameters instanceof Map<?, ?>)) {
      throw new IllegalArgumentException("The policy for %s must be a JSON object".formatted(type));
    }
    final Split split = split(type, parameters);
    try {
      return MetadataPolicy.parse(JSONObject.toJSONString(split.rest()));
    }
    catch (final ParseException | PolicyViolationException | RuntimeException e) {
      throw new IllegalArgumentException("The policy for %s is not valid: %s".formatted(type, e.getMessage()), e);
    }
  }

  /**
   * Separates the parameters removed with {@code value: null} from the rest.
   */
  @SuppressWarnings("unchecked")
  private static Split split(final String type, final Object parameters) {
    if (!(parameters instanceof final Map<?, ?> map)) {
      throw new IllegalArgumentException("The policy for %s must be a JSON object".formatted(type));
    }
    final Map<String, Object> rest = new LinkedHashMap<>();
    final List<String> removed = new ArrayList<>();
    map.forEach((parameter, operators) -> {
      if (operators instanceof final Map<?, ?> ops && ops.containsKey("value") && ops.get("value") == null) {
        if (ops.size() > 1) {
          throw new IllegalArgumentException(("The policy for %s is not valid: %s removes the parameter (value null) "
              + "and cannot have other operators").formatted(type, parameter));
        }
        removed.add(String.valueOf(parameter));
      }
      else {
        rest.put(String.valueOf(parameter), operators);
      }
    });
    return new Split(rest, removed);
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

  private static Map<String, Object> nullValue() {
    final Map<String, Object> operators = new LinkedHashMap<>();
    operators.put("value", null);
    return operators;
  }

  private record Split(Map<String, Object> rest, List<String> removed) {
  }
}
