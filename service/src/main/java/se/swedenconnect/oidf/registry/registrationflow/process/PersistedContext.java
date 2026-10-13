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
package se.swedenconnect.oidf.registry.registrationflow.process;

import com.nimbusds.jose.jwk.JWKSet;
import net.minidev.json.JSONObject;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves the context values a paused pipeline needs when it resumes after manual review, and puts them back.
 *
 * <p>When a step pauses, the steps before it have already loaded data into the context: the entity's metadata and
 * keys, a generated metadata policy. Resuming starts a new run from the paused step, so without this the data those
 * earlier steps produced would be gone. Values that identify the registration (entity, organization, flow) are not
 * saved here; they are rebuilt from the registration itself.</p>
 *
 * @author Felix Hellman
 */
public final class PersistedContext {

  /** Context keys holding a JSON object. */
  private static final List<String> JSON_KEYS = List.of(
      ContextKey.ENTITY_CONFIGURATION_METADATA,
      ContextKey.METADATA_POLICY,
      ContextKey.REQUEST_METADATA,
      ContextKey.SUBORDINATE_METADATA,
      ContextKey.FETCHED_RESOURCES,
      ContextKey.HOSTED_PREVIOUS_METADATA);

  private PersistedContext() {
  }

  /**
   * Extracts the values to save from a paused pipeline's context.
   *
   * @param ctx the context at the pause
   * @return the values to save, keyed by context key; empty if there is nothing to save
   */
  public static Map<String, Object> capture(final ProcessContext ctx) {
    final Map<String, Object> saved = new LinkedHashMap<>();
    for (final String key : JSON_KEYS) {
      ctx.<JSONObject>get(key).ifPresent(value -> saved.put(key, new LinkedHashMap<>(value)));
    }
    ctx.<JWKSet>get(ContextKey.ENTITY_CONFIGURATION_JWKS)
        .ifPresent(jwks -> saved.put(ContextKey.ENTITY_CONFIGURATION_JWKS, jwks.toJSONObject(false)));
    return saved;
  }

  /**
   * Puts saved values back into a context that is about to resume. Unknown keys are ignored, so values saved by an
   * older version do not stop a resume.
   *
   * @param saved the values returned by {@link #capture(ProcessContext)}, may be {@code null}
   * @param ctx the context to fill
   * @throws IllegalStateException if a saved key set cannot be parsed
   */
  @SuppressWarnings("unchecked")
  public static void restore(final Map<String, Object> saved, final ProcessContext ctx) {
    if (saved == null) {
      return;
    }
    for (final String key : JSON_KEYS) {
      if (saved.get(key) instanceof final Map<?, ?> value) {
        ctx.put(key, new JSONObject((Map<String, Object>) value));
      }
    }
    if (saved.get(ContextKey.ENTITY_CONFIGURATION_JWKS) instanceof final Map<?, ?> jwks) {
      try {
        ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, JWKSet.parse((Map<String, Object>) jwks));
      }
      catch (final ParseException e) {
        throw new IllegalStateException("Saved entity configuration keys cannot be parsed", e);
      }
    }
  }
}
