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
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.infrastructure.auth.domain;

/**
 * Hierarchical rights levels from the {@code org_rights} JWT claim.
 * Ordinal order defines hierarchy: ADMIN covers WRITE covers READ.
 *
 * @author Per Fredrik Plars
 */
public enum Right {
  READ, WRITE, ADMIN;

  /**
   * Returns true if this right level satisfies the required level.
   *
   * @param required the minimum required right
   * @return true if this right covers the required right
   */
  public boolean covers(final Right required) {
    return this.ordinal() >= required.ordinal();
  }
}
