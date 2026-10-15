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
package se.swedenconnect.oidf.registry.registrationflow.metadata;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Applies the {@code metadata} and {@code metadata_policy} claims of a Subordinate Statement to an entity's metadata,
 * as the federation does when it resolves the entity, and lists the parameters whose value changes. Only the
 * Subordinate Statement's own claims are applied, not the policies of the superiors above it.
 *
 * @author Felix Hellman
 */
public final class AppliedPolicy {

  private AppliedPolicy() {
  }

  /**
   * A parameter whose resolved value is not the one the entity declares.
   *
   * @param entityType the entity type, e.g. {@code openid_relying_party}
   * @param parameter the parameter, with language tag if it has one
   * @param entityValue the value the entity declares, {@code null} if it declares none
   * @param value the resolved value, {@code null} if the parameter is removed
   * @param claim {@link PinnedValues#METADATA} or {@link PinnedValues#METADATA_POLICY}
   * @param operators the operators of the {@code metadata_policy} claim on the parameter; empty for the
   *     {@code metadata} claim
   */
  public record Overridden(String entityType, String parameter, Object entityValue, Object value, String claim,
      List<String> operators) {
  }

  /**
   * The outcome of applying the claims.
   *
   * @param overrides the parameters whose value changes, per entity type in the order the entity declares them
   * @param errors the entity types whose metadata the policy rejects, with the reason
   */
  public record Outcome(List<Overridden> overrides, List<String> errors) {
  }

  /**
   * Applies the claims. Only the entity types the entity declares are considered.
   *
   * @param entityMetadata the entity's metadata, keyed by entity type
   * @param metadata the {@code metadata} claim, keyed by entity type; may be {@code null}
   * @param metadataPolicy the {@code metadata_policy} claim, keyed by entity type; may be {@code null}
   * @return the changed parameters and the policy errors
   */
  public static Outcome apply(final Map<String, Object> entityMetadata, final Map<String, Object> metadata,
      final Map<String, Object> metadataPolicy) {
    final List<Overridden> overrides = new ArrayList<>();
    final List<String> errors = new ArrayList<>();
    entityMetadata.forEach((entityType, declaredValue) -> {
      if (!(declaredValue instanceof final Map<?, ?> declared)) {
        return;
      }
      final Map<?, ?> set = metadata != null && metadata.get(entityType) instanceof final Map<?, ?> map
          ? map : Map.of();
      final Map<?, ?> policy = metadataPolicy != null && metadataPolicy.get(entityType) instanceof final Map<?, ?> map
          ? map : null;
      if (set.isEmpty() && policy == null) {
        return;
      }
      final JSONObject start = new JSONObject();
      declared.forEach((key, value) -> start.put(String.valueOf(key), value));
      set.forEach((key, value) -> start.put(String.valueOf(key), value));
      final JSONObject resolved;
      try {
        resolved = policy == null ? start : MetadataPolicy.parse(toJson(policy)).apply(start);
      }
      catch (final ParseException | PolicyViolationException e) {
        errors.add("%s: %s".formatted(entityType, e.getMessage()));
        return;
      }
      final Set<String> parameters = new LinkedHashSet<>();
      declared.keySet().forEach(key -> parameters.add(String.valueOf(key)));
      parameters.addAll(resolved.keySet());
      for (final String parameter : parameters) {
        final Object entityValue = declared.get(parameter);
        final Object value = resolved.get(parameter);
        if (!Objects.equals(normalize(entityValue), normalize(value))) {
          overrides.add(set.containsKey(parameter)
              ? new Overridden(entityType, parameter, entityValue, value, PinnedValues.METADATA, List.of())
              : new Overridden(entityType, parameter, entityValue, value, PinnedValues.METADATA_POLICY,
                  operators(policy, parameter)));
        }
      }
    });
    return new Outcome(List.copyOf(overrides), List.copyOf(errors));
  }

  private static List<String> operators(final Map<?, ?> policy, final String parameter) {
    return policy != null && policy.get(parameter) instanceof final Map<?, ?> operators
        ? operators.keySet().stream().map(String::valueOf).toList()
        : List.of();
  }

  private static JSONObject toJson(final Map<?, ?> map) {
    final JSONObject json = new JSONObject();
    map.forEach((key, value) -> json.put(String.valueOf(key), value));
    return json;
  }

  /** Reads a value back from JSON, so that values declared and values resolved compare the same way. */
  private static Object normalize(final Object value) {
    return value == null ? null : JSONValue.parse(JSONValue.toJSONString(value));
  }
}
