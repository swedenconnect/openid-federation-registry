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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * One organization entry from the {@code org_rights} JWT claim, containing the
 * organization identity and the set of function-scoped rights held by the user.
 *
 * @param organizationIdentifier organization number
 * @param organizationNameSv     organization name in Swedish
 * @param organizationNameEn     organization name in English
 * @param functions              list of function+right entries
 * @author Per Fredrik Plars
 */
public record OrgRightEntry(
    String organizationIdentifier,
    String organizationNameSv,
    String organizationNameEn,
    List<FunctionRight> functions) {

  /**
   * Computes the effective right for a specific function group by taking the highest right among all function
   * entries whose {@link FunctionRight#function()} matches it exactly.
   *
   * @param functionGroup the function group to evaluate
   * @return the effective right, or empty if no matching entry exists
   */
  public Optional<Right> effectiveRight(final String functionGroup) {
    return this.functions.stream()
        .filter(f -> f.function().equals(functionGroup))
        .map(FunctionRight::right)
        .max(Comparator.naturalOrder());
  }

  /**
   * Returns true if the user has at least the required right for the given function group.
   *
   * @param functionGroup the function group to check
   * @param required      the minimum required right level
   * @return true if the effective right covers the required right
   */
  public boolean hasRight(final String functionGroup, final Right required) {
    return this.effectiveRight(functionGroup)
        .map(r -> r.covers(required))
        .orElse(false);
  }
}
