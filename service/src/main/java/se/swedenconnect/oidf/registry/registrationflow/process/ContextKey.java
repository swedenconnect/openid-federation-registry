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

/**
 * Well-known keys for values stored in a {@link ProcessContext}.
 * <p>
 * Using string constants here keeps the API simple; a typed {@code ContextKey<T>} variant
 * can be introduced later if cast-safety becomes a concern.
 *
 * @author Per Fredrik Plars
 */
public final class ContextKey {

  public static final String ENTITY_METADATA = "entityMetadata";
  public static final String ENTITY_ID = "entityId";

  private ContextKey() {
  }
}
