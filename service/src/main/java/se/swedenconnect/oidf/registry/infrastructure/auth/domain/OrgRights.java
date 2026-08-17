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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Parsed representation of the {@code org_rights} JWT claim.
 * A superuser bypasses all right checks.
 *
 * @param superuser true if the token carries {@code [{"superuser": true}]}
 * @param entries   list of per-organization right entries
 * @author Per Fredrik Plars
 */
public record OrgRights(boolean superuser, List<OrgRightEntry> entries) implements Serializable {

  /**
   * Returns true if the user has at least the required right for the given organization and function group.
   * Superusers always return true.
   *
   * @param orgNumber     organization number from the request path
   * @param functionGroup the function group backing the tenant from the request path
   * @param required      minimum required right level
   * @return true if access is granted
   */
  public boolean hasRight(final String orgNumber, final String functionGroup, final Right required) {
    if (this.superuser) {
      return true;
    }
    return this.entries.stream()
        .filter(e -> e.organizationIdentifier().equals(orgNumber))
        .anyMatch(e -> e.hasRight(functionGroup, required));
  }

  /**
   * Finds the organization entry for the given organization number.
   *
   * @param orgNumber the organization number to look up
   * @return the matching entry, or empty if not present
   */
  public Optional<OrgRightEntry> findOrg(final String orgNumber) {
    return this.entries.stream()
        .filter(e -> e.organizationIdentifier().equals(orgNumber))
        .findFirst();
  }
}
