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
package se.swedenconnect.oidf.registry.infrastructure.auth;

import se.swedenconnect.iam.commons.types.LocalizedString;
import se.swedenconnect.iam.commons.types.OrganizationID;
import se.swedenconnect.iam.security.claims.OrgRightsClaim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses the {@code scope} claim of the org-scoped OAuth2 access-token flow into an {@link OrgRightsClaim}, for
 * tokens that carry no {@code org_rights} claim.
 *
 * <p>Expected claim format — a space-delimited string of {@code "{orgId}:{function}:{right}"} entries:
 * <pre>
 * "scope": "5590026042:demo:write 5590026042:other-function:read"
 * </pre>
 * Entries that don't split into exactly three {@code :}-separated parts (e.g. a generic {@code "openid"} or
 * {@code "profile"} scope) are ignored. This flow never grants superuser — that remains exclusive to the
 * {@code org_rights} claim parsed by {@link se.swedenconnect.iam.security.claims.OrgRightsClaimParser}.
 *
 * <p>Unlike the {@code org_rights} claim, this claim carries no organization display name — entries are built
 * with an empty {@link LocalizedString}.
 *
 * @author Per Fredrik Plars
 */
public class ScopeRightsFactory {

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  /**
   * Parses the {@code scope} claim into an {@link OrgRightsClaim}.
   *
   * @param claims the JWT claims map
   * @return the parsed org rights, never a superuser
   */
  public static OrgRightsClaim fromClaims(final Map<String, Object> claims) {
    final Object raw = claims.get("scope");
    if (!(raw instanceof final String scope) || scope.isBlank()) {
      throw new IllegalArgumentException("scope claim is missing from token");
    }

    final Map<String, List<OrgRightsClaim.FunctionEntry>> byOrg = new LinkedHashMap<>();
    for (final String token : WHITESPACE.split(scope.trim())) {
      final String[] parts = token.split(":");
      if (parts.length != 3) {
        continue;
      }
      byOrg.computeIfAbsent(parts[0], key -> new ArrayList<>())
          .add(new OrgRightsClaim.FunctionEntry(parts[1], parts[2]));
    }

    if (byOrg.isEmpty()) {
      throw new IllegalArgumentException("scope claim does not contain any organization-scoped entries");
    }

    final List<OrgRightsClaim.OrgEntry> entries = byOrg.entrySet().stream()
        .map(entry -> new OrgRightsClaim.OrgEntry(
            OrganizationID.of(entry.getKey()), new LocalizedString(), null, List.copyOf(entry.getValue())))
        .toList();

    return new OrgRightsClaim(false, entries);
  }
}
