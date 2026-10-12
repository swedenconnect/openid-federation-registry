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
package se.swedenconnect.oidf.registry.infrastructure.persistence;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Converts stored timestamps for the API.
 *
 * @author Felix Hellman
 */
public final class Timestamps {

  private Timestamps() {
  }

  /**
   * Resolves a stored, zone-less timestamp against the running service's own zone, so the API emits an absolute
   * instant rather than a wall-clock reading a consumer would have to guess the zone for.
   *
   * @param timestamp the stored timestamp, may be {@code null}
   * @return the same point in time with an offset, or {@code null}
   */
  public static OffsetDateTime toOffsetDateTime(final LocalDateTime timestamp) {
    return timestamp == null ? null : timestamp.atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }
}
