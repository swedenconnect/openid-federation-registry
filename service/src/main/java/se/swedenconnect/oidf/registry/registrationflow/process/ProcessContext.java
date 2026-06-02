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

import se.swedenconnect.oidf.registry.registrationflow.process.step.MissingContextValueException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared blackboard for passing data between pipeline steps.
 *
 * @author Per Fredrik Plars
 */
public class ProcessContext {

  private final Map<String, Object> data = new HashMap<>();

  /**
   * Stores a value in the context under the given key.
   *
   * @param <T> value type
   * @param key context key
   * @param value value to store
   */
  public <T extends Serializable> void put(final String key, final T value) {
    this.data.put(key, value);
  }

  /**
   * Returns the value for the given key, or empty if absent.
   *
   * @param <T> value type
   * @param key context key
   * @return optional value
   */
  @SuppressWarnings("unchecked")
  public <T extends Serializable> Optional<T> get(final String key) {
    return Optional.ofNullable((T) this.data.get(key));
  }

  /**
   * Returns the value for the given key, throwing if absent.
   *
   * @param <T> value type
   * @param key context key
   * @return the value
   * @throws MissingContextValueException if the key is not present
   */
  public <T extends Serializable> T getRequired(final String key) {
    return this.<T>get(key)
        .orElseThrow(() -> new MissingContextValueException(key));
  }

  /**
   * Returns the value associated with the given key, cast to the given type.
   *
   * @param <T> value type
   * @param key context key
   * @param type the expected type class
   * @return the value
   * @throws MissingContextValueException if the key is not present
   */
  public <T extends Serializable> T getRequired(final String key, final Class<T> type) {
    return this.<T>get(key)
        .orElseThrow(() -> new MissingContextValueException(key));
  }

  /**
   * Removes a value from the context.
   *
   * @param key context key to remove
   */
  public void remove(final String key) {
    this.data.remove(key);
  }

  /**
   * Creates a shallow copy of this context. Useful for sub-flows that need an isolated context.
   *
   * @return new ProcessContext with the same key-value entries
   */
  public ProcessContext copy() {
    final ProcessContext copy = new ProcessContext();
    copy.data.putAll(this.data);
    return copy;
  }

  /**
   * Returns a string snapshot of the current context for diff computation.
   * Values are truncated to 100 000 characters to keep diff storage bounded while
   * preserving complete JSON structures for front-end pretty-printing.
   *
   * @return ordered map of key to string representation
   */
  public Map<String, String> snapshot() {
    final Map<String, String> snap = new LinkedHashMap<>();
    this.data.forEach((k, v) -> {
      String s = v == null ? "null" : v.toString();
      if (s.length() > 100_000) {
        s = s.substring(0, 100_000) + "…";
      }
      snap.put(k, s);
    });
    return snap;
  }
}
