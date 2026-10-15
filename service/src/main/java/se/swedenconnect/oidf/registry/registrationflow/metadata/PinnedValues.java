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

import net.minidev.json.JSONValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The values a published Subordinate Statement pins for a self-managed entity: parameters set by the {@code metadata}
 * claim or fixed with the {@code value} operator of the {@code metadata_policy} claim, and the federation keys in
 * {@code jwks}. Whatever the entity publishes in its own Entity Configuration, these are what the federation resolves,
 * so changing one needs a new Subordinate Statement.
 *
 * @author Felix Hellman
 */
public final class PinnedValues {

  /** Claim of a parameter set by the {@code metadata} claim. */
  public static final String METADATA = "metadata";

  /** Claim of a parameter fixed with the {@code value} operator. */
  public static final String METADATA_POLICY = "metadata_policy";

  private PinnedValues() {
  }

  /**
   * A pinned metadata parameter.
   *
   * @param entityType the entity type, e.g. {@code openid_relying_party}
   * @param parameter the parameter, with language tag if it has one
   * @param value the pinned value
   * @param claim {@link #METADATA} or {@link #METADATA_POLICY}
   */
  public record Parameter(String entityType, String parameter, Object value, String claim) {
  }

  /**
   * Lists the parameters a Subordinate Statement pins.
   *
   * @param metadataPolicy the {@code metadata_policy} claim as stored, {@code { "metadata_policy": { ... } }}; may be
   *     {@code null}
   * @param metadata the {@code metadata} claim, keyed by entity type; may be {@code null}
   * @return the pinned parameters, those of the {@code metadata} claim first
   */
  public static List<Parameter> parameters(final Map<String, Object> metadataPolicy,
      final Map<String, Object> metadata) {
    final List<Parameter> pinned = new ArrayList<>();
    forEachParameter(metadata, (entityType, parameter, value) ->
        pinned.add(new Parameter(entityType, parameter, value, METADATA)));
    final Object policy = metadataPolicy == null ? null : metadataPolicy.get("metadata_policy");
    if (policy instanceof final Map<?, ?> policies) {
      forEachParameter(policies, (entityType, parameter, operators) -> {
        if (operators instanceof final Map<?, ?> map && map.containsKey("value")) {
          pinned.add(new Parameter(entityType, parameter, map.get("value"), METADATA_POLICY));
        }
      });
    }
    return pinned;
  }

  /**
   * Tells whether publishing would change the Subordinate Statement. A claim that is not built again in this run
   * ({@code null}) is kept as published, so it does not count as a change.
   *
   * @param published the published claims
   * @param metadataPolicy the {@code metadata_policy} claim built in this run, or {@code null}
   * @param metadata the {@code metadata} claim built in this run, or {@code null}
   * @param jwks the entity's federation keys loaded in this run, as a JWK set
   * @return {@code true} if the claims and keys are the ones published
   */
  public static boolean unchanged(final Published published, final Map<String, Object> metadataPolicy,
      final Map<String, Object> metadata, final Map<String, Object> jwks) {
    return (metadataPolicy == null || same(published.metadataPolicy(), metadataPolicy))
        && (metadata == null || same(published.metadata(), metadata))
        && sameKeys(published.jwks(), jwks);
  }

  /**
   * The claims of a published Subordinate Statement.
   *
   * @param metadataPolicy the {@code metadata_policy} claim as stored, may be {@code null}
   * @param metadata the {@code metadata} claim, may be {@code null}
   * @param jwks the {@code jwks} claim, may be {@code null}
   */
  public record Published(Map<String, Object> metadataPolicy, Map<String, Object> metadata,
      Map<String, Object> jwks) {
  }

  private static boolean same(final Object published, final Object built) {
    return Objects.equals(normalize(published), normalize(built));
  }

  /** Compares two JWK sets by their keys, in any order. */
  private static boolean sameKeys(final Map<String, Object> published, final Map<String, Object> built) {
    final Object publishedKeys = normalize(published == null ? null : published.get("keys"));
    final Object builtKeys = normalize(built == null ? null : built.get("keys"));
    if (publishedKeys instanceof final List<?> a && builtKeys instanceof final List<?> b) {
      return a.size() == b.size() && new HashSet<>(a).equals(new HashSet<>(b));
    }
    return Objects.equals(publishedKeys, builtKeys);
  }

  /** Reads a value back from JSON, so that values stored and values built compare the same way. */
  private static Object normalize(final Object value) {
    return value == null ? null : JSONValue.parse(JSONValue.toJSONString(value));
  }

  private static void forEachParameter(final Map<?, ?> byEntityType, final ParameterConsumer consumer) {
    if (byEntityType == null) {
      return;
    }
    byEntityType.forEach((entityType, parameters) -> {
      if (parameters instanceof final Map<?, ?> map) {
        map.forEach((parameter, value) ->
            consumer.accept(String.valueOf(entityType), String.valueOf(parameter), value));
      }
    });
  }

  @FunctionalInterface
  private interface ParameterConsumer {
    void accept(String entityType, String parameter, Object value);
  }
}
