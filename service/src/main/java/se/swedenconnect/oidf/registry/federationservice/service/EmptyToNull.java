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
package se.swedenconnect.oidf.registry.federationservice.service;

import java.util.List;

/**
 * Helper for the records sent to the oidf-service, where a parameter without data should be {@code null} and not an
 * empty array.
 *
 * @author Per Fredrik Plars
 */
final class EmptyToNull {

  private EmptyToNull() {
  }

  /**
   * Returns the list, or {@code null} if it is {@code null} or empty.
   *
   * @param list the list
   * @param <T> element type
   * @return the list, or null if there is nothing in it
   */
  static <T> List<T> list(final List<T> list) {
    return list == null || list.isEmpty() ? null : list;
  }

}
