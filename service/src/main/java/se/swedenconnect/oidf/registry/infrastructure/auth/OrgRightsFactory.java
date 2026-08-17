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
package se.swedenconnect.oidf.registry.infrastructure.auth;

import se.swedenconnect.oidf.registry.infrastructure.auth.domain.FunctionRight;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRightEntry;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.Right;

import java.util.List;
import java.util.Map;

/**
 * Parses the {@code org_rights} JWT claim into an {@link OrgRights} object.
 *
 * <p>Expected claim format:
 * <pre>
 * "org_rights": [
 *   {
 *     "organization_identifier": "5590026042",
 *     "organization_name#sv": "Litsec AB",
 *     "organization_name#en": "Litsec AB",
 *     "functions": [
 *       {"function": "demo-function-group", "right": "write"}
 *     ]
 *   }
 * ]
 * </pre>
 * Superuser format: {@code [{"superuser": true}]}
 *
 * @author Per Fredrik Plars
 */
public class OrgRightsFactory {

  @SuppressWarnings("unchecked")
  public static OrgRights fromClaims(final Map<String, Object> claims) {
    final Object raw = claims.get("org_rights");
    if (raw == null) {
      throw new IllegalArgumentException("org_rights claim is missing from token");
    }

    final List<Map<String, Object>> entries = (List<Map<String, Object>>) raw;
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("org_rights claim is empty");
    }

    if (entries.size() == 1 && Boolean.TRUE.equals(entries.getFirst().get("superuser"))) {
      return new OrgRights(true, List.of());
    }

    final List<OrgRightEntry> orgEntries = entries.stream()
        .map(OrgRightsFactory::parseOrgEntry)
        .toList();

    return new OrgRights(false, orgEntries);
  }

  @SuppressWarnings("unchecked")
  private static OrgRightEntry parseOrgEntry(final Map<String, Object> entry) {
    final String orgId = requiredString(entry, "organization_identifier");
    final String nameSv = entry.getOrDefault("organization_name#sv", "").toString();
    final String nameEn = entry.getOrDefault("organization_name#en", "").toString();

    final List<Map<String, Object>> rawFunctions =
        (List<Map<String, Object>>) entry.getOrDefault("functions", List.of());

    final List<FunctionRight> functions = rawFunctions.stream()
        .map(OrgRightsFactory::parseFunctionRight)
        .toList();

    return new OrgRightEntry(orgId, nameSv, nameEn, functions);
  }

  private static FunctionRight parseFunctionRight(final Map<String, Object> f) {
    final String function = requiredString(f, "function");
    final String rightStr = requiredString(f, "right");
    final Right right = Right.valueOf(rightStr.toUpperCase());
    return new FunctionRight(function, right);
  }

  private static String requiredString(final Map<String, Object> map, final String key) {
    final Object value = map.get(key);
    if (value == null) {
      throw new IllegalArgumentException("Missing required field '" + key + "' in org_rights entry");
    }
    return value.toString();
  }
}
