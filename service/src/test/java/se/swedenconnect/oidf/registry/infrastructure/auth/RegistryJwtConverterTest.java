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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import se.swedenconnect.iam.commons.types.OrganizationID;
import se.swedenconnect.iam.security.claims.OrganizationRight;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryJwtConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RegistryJwtConverter}.
 */
class RegistryJwtConverterTest {

  private static final String ORG_1 = "5520001263";
  private static final String ORG_2 = "5520002634";
  private static final String UNKNOWN_ORG = "99999";

  private static final List<Map<String, Object>> SINGLE_ORG_RIGHTS = List.of(
      Map.of(
          "organization_identifier", ORG_1,
          "organization_name#sv", "Pensionsmyndigheten",
          "organization_name#en", "Pensionsmyndigheten",
          "functions", List.of(Map.of("function", "demo", "right", "read"))
      )
  );

  private final RegistryJwtConverter converter = new RegistryJwtConverter();

  private Jwt buildJwt(final Map<String, Object> extraClaims) {
    final Jwt.Builder builder = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org_rights", SINGLE_ORG_RIGHTS);
    extraClaims.forEach(builder::claim);
    return builder.build();
  }

  @Test
  @DisplayName("Scope as list produces SCOPE_-prefixed authorities")
  void scopeListProducesAuthorities() {
    final Jwt jwt = buildJwt(Map.of(
        "scope", List.of("read", "write"),
        "preferred_username", "alice"
    ));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("SCOPE_read", "SCOPE_write");
  }

  @Test
  @DisplayName("Scope as space-separated string is split into individual authorities")
  void scopeSpaceSeparatedStringIsSplitIntoAuthorities() {
    final Jwt jwt = buildJwt(Map.of(
        "scope", "read write",
        "preferred_username", "alice"
    ));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("SCOPE_read", "SCOPE_write");
  }

  @Test
  @DisplayName("preferred_username is used as principal name when present")
  void preferredUsernameUsedAsPrincipalName() {
    final Jwt jwt = buildJwt(Map.of(
        "scope", List.of("read"),
        "preferred_username", "alice"
    ));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getName()).isEqualTo("alice");
  }

  @Test
  @DisplayName("sub is used as principal name when preferred_username is absent")
  void subjectFallbackWhenPreferredUsernameAbsent() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read")));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getName()).isEqualTo("test-subject");
  }

  @Test
  @DisplayName("Empty scope list produces no authorities")
  void emptyScopeListProducesNoAuthorities() {
    final Jwt jwt = buildJwt(Map.of(
        "scope", List.of(),
        "preferred_username", "alice"
    ));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities()).isEmpty();
  }

  @Test
  @DisplayName("Missing scope claim produces no authorities")
  void missingScopeClaimProducesNoAuthorities() {
    final Jwt jwt = buildJwt(Map.of("preferred_username", "alice"));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities()).isEmpty();
  }

  @Test
  @DisplayName("Returns RegistryClaims with parsed org_rights")
  void returnsRegistryClaimsWithOrgRights() {
    final Jwt jwt = buildJwt(Map.of(
        "scope", List.of("read"),
        "preferred_username", "alice"
    ));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token).isInstanceOf(RegistryClaims.class);
    final RegistryClaims claims = (RegistryClaims) token;
    assertThat(claims.getOrgRights().orgEntries()).hasSize(1);
    assertThat(claims.getOrgRights().orgEntries().getFirst().orgIdentifier().getId()).isEqualTo(ORG_1);
  }

  @Test
  @DisplayName("Multiple organizations are all present in OrgRights")
  void multipleOrganizationsAllPresentInClaims() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org_rights", List.of(
            Map.of(
                "organization_identifier", ORG_1,
                "organization_name#sv", "Pensionsmyndigheten",
                "organization_name#en", "Pensionsmyndigheten",
                "functions", List.of(Map.of("function", "*", "right", "read"))
            ),
            Map.of(
                "organization_identifier", ORG_2,
                "organization_name#sv", "Arbetsförmedlingen",
                "organization_name#en", "Employment Agency",
                "functions", List.of(Map.of("function", "*", "right", "write"))
            )
        ))
        .claim("scope", List.of("read"))
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().orgEntries())
        .extracting(e -> e.orgIdentifier().getId())
        .containsExactlyInAnyOrder(ORG_1, ORG_2);
  }

  @Test
  @DisplayName("findOrgEntry returns present entry for known org")
  void findOrgEntryReturnsPresentEntryForKnownOrg() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(OrgRightsService.findOrgEntry(claims.getOrgRights(), ORG_1)).isPresent();
    assertThat(OrgRightsService.findOrgEntry(claims.getOrgRights(), ORG_1).get().name().get("sv"))
        .isEqualTo("Pensionsmyndigheten");
  }

  @Test
  @DisplayName("findOrgEntry returns empty for unknown org")
  void findOrgEntryReturnsEmptyForUnknownOrg() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(OrgRightsService.findOrgEntry(claims.getOrgRights(), UNKNOWN_ORG)).isEmpty();
  }

  @Test
  @DisplayName("hasRight returns true when function matches and right is sufficient")
  void hasRightReturnsTrueWhenSufficient() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(OrgRightsService.hasRight(claims.getOrgRights(), ORG_1, List.of("demo"), OrganizationRight.READ))
        .isTrue();
  }

  @Test
  @DisplayName("hasRight returns false when right is insufficient")
  void hasRightReturnsFalseWhenInsufficient() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(OrgRightsService.hasRight(claims.getOrgRights(), ORG_1, List.of("demo"), OrganizationRight.WRITE))
        .isFalse();
  }

  @Test
  @DisplayName("Superuser token grants access to everything")
  void superuserTokenGrantsAccessToEverything() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("superuser")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org_rights", List.of(Map.of("superuser", true)))
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().superuser()).isTrue();
    assertThat(OrgRightsService.hasRight(claims.getOrgRights(), "any-org", List.of("any-tenant"),
        OrganizationRight.ADMIN)).isTrue();
  }

  @Test
  @DisplayName("Token with neither org_rights nor an org-scoped scope claim throws InvalidBearerTokenException")
  void missingOrgRightsAndScopeClaimThrowsInvalidBearerTokenException() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("scope", List.of("read"))
        .build();

    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(InvalidBearerTokenException.class);
  }

  @Test
  @DisplayName("Token is marked as authenticated after conversion")
  void tokenIsAuthenticatedAfterConversion() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.isAuthenticated()).isTrue();
  }

  // -------------------------------------------------------------------------
  // Scope-based access-token flow (no org_rights claim)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("A scope-only token is parsed into OrgRights with a blank org name")
  void scopeOnlyTokenIsParsedIntoOrgRights() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("scope", ORG_1 + ":demo:write")
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().superuser()).isFalse();
    assertThat(claims.getOrgRights().orgEntries()).hasSize(1);
    assertThat(claims.getOrgRights().orgEntries().getFirst().orgIdentifier()).isEqualTo(OrganizationID.of(ORG_1));
    assertThat(claims.getOrgRights().orgEntries().getFirst().functions())
        .containsExactly(new se.swedenconnect.iam.security.claims.OrgRightsClaim.FunctionEntry("demo", "write"));
  }

  @Test
  @DisplayName("When both org_rights and scope are present, org_rights wins")
  void orgRightsTakesPriorityOverScope() {
    final Jwt jwt = buildJwt(Map.of("scope", ORG_2 + ":other-function:admin"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().orgEntries())
        .extracting(e -> e.orgIdentifier().getId())
        .containsExactly(ORG_1);
  }

  @Test
  @DisplayName("Multiple function entries for the same org in one scope string are grouped into one entry")
  void multipleFunctionsForSameOrgAreGrouped() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("scope", ORG_1 + ":demo:write " + ORG_1 + ":other-function:read")
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().orgEntries()).hasSize(1);
    assertThat(claims.getOrgRights().orgEntries().getFirst().functions())
        .containsExactlyInAnyOrder(
            new se.swedenconnect.iam.security.claims.OrgRightsClaim.FunctionEntry("demo", "write"),
            new se.swedenconnect.iam.security.claims.OrgRightsClaim.FunctionEntry("other-function", "read"));
  }

  @Test
  @DisplayName("Multiple orgs in one scope string produce separate entries")
  void multipleOrgsInScopeProduceSeparateEntries() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("scope", ORG_1 + ":demo:write " + ORG_2 + ":other-function:read")
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().orgEntries())
        .extracting(e -> e.orgIdentifier().getId())
        .containsExactlyInAnyOrder(ORG_1, ORG_2);
  }

  @Test
  @DisplayName("A generic non-org-scoped scope entry alongside an org-scoped one is ignored, not fatal")
  void nonOrgScopedEntryIsIgnored() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("scope", "openid " + ORG_1 + ":demo:write profile")
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrgRights().orgEntries()).hasSize(1);
    assertThat(claims.getOrgRights().orgEntries().getFirst().orgIdentifier().getId()).isEqualTo(ORG_1);
  }
}
