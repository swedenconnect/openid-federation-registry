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
import java.util.Map;
import java.util.Optional;

/**
 * Shared blackboard for passing data between pipeline steps.
 *
 * @author Per Fredrik Plars
 */
public class ProcessContext {

  private final Map<String, Object> data = new HashMap<>();

  public <T extends Serializable> void put(final String key, final T value) {
    data.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T extends Serializable> Optional<T> get(final String key) {
    return Optional.ofNullable((T) data.get(key));
  }

  public <T extends Serializable> T getRequired(final String key) {
    return this.<T>get(key)
        .orElseThrow(() -> new MissingContextValueException(key));
  }
}
