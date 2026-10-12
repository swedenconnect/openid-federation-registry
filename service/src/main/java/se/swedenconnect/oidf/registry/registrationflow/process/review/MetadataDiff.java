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
package se.swedenconnect.oidf.registry.registrationflow.process.review;

import net.minidev.json.JSONValue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compares two federation metadata documents, keyed by entity type and then by parameter, and lists what changed.
 *
 * @author Felix Hellman
 */
public final class MetadataDiff {

  private MetadataDiff() {
  }

  /**
   * Lists the parameters that were added, changed or removed between two metadata documents. A parameter is named
   * {@code entity_type.parameter}; an entity type that is not a JSON object is compared as a whole.
   *
   * @param before the current metadata, may be {@code null}
   * @param after the proposed metadata, may be {@code null}
   * @return the changes, in the order entity types and parameters appear
   */
  public static List<PreviewSection.DiffEntry> compare(final Map<String, Object> before,
      final Map<String, Object> after) {
    final Map<String, Object> left = before == null ? Map.of() : before;
    final Map<String, Object> right = after == null ? Map.of() : after;
    final List<PreviewSection.DiffEntry> changes = new ArrayList<>();
    for (final String entityType : union(left.keySet(), right.keySet())) {
      final Object leftValue = left.get(entityType);
      final Object rightValue = right.get(entityType);
      if (leftValue instanceof final Map<?, ?> leftParameters
          && rightValue instanceof final Map<?, ?> rightParameters) {
        for (final Object parameter : union(leftParameters.keySet(), rightParameters.keySet())) {
          addChange(changes, entityType + "." + parameter, leftParameters.get(parameter),
              rightParameters.get(parameter));
        }
      }
      else {
        addChange(changes, entityType, leftValue, rightValue);
      }
    }
    return changes;
  }

  private static void addChange(final List<PreviewSection.DiffEntry> changes, final String key,
      final Object before, final Object after) {
    if (before == null && after == null) {
      return;
    }
    if (before == null) {
      changes.add(new PreviewSection.DiffEntry(PreviewSection.ChangeType.ADDED, key, null, render(after)));
    }
    else if (after == null) {
      changes.add(new PreviewSection.DiffEntry(PreviewSection.ChangeType.REMOVED, key, render(before), null));
    }
    else if (!Objects.equals(render(before), render(after))) {
      changes.add(new PreviewSection.DiffEntry(PreviewSection.ChangeType.CHANGED, key, render(before),
          render(after)));
    }
  }

  private static String render(final Object value) {
    return value instanceof final String text ? text : JSONValue.toJSONString(value);
  }

  private static <T> Set<T> union(final Set<? extends T> first, final Set<? extends T> second) {
    final Set<T> keys = new LinkedHashSet<>(first);
    keys.addAll(second);
    return keys;
  }
}
