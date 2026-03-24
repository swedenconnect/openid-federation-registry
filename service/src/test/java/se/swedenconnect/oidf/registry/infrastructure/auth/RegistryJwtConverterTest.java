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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryClaims;
import se.swedenconnect.oidf.registry.infrastructure.auth.oauth.RegistryJwtConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RegistryJwtConverter}.
 */
class RegistryJwtConverterTest {

  private static final List<Map<String, Object>> SINGLE_ORG = List.of(
      Map.of("orgNumber", "55555", "orgName", "Pensionsmyndigheten", "entity_prefix", "https://www.pm.se/oidf")
  );

  private final RegistryJwtConverter converter = new RegistryJwtConverter();

  private Jwt buildJwt(final Map<String, Object> extraClaims) {
    final Jwt.Builder builder = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org", SINGLE_ORG);
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
  @DisplayName("Returns RegistryClaims with organization information")
  void returnsRegistryClaimsWithOrganizationInformation() {
    final Jwt jwt = buildJwt(Map.of(
        "scope", List.of("read"),
        "preferred_username", "alice"
    ));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token).isInstanceOf(RegistryClaims.class);
    final RegistryClaims claims = (RegistryClaims) token;
    assertThat(claims.getOrganizationInformation().organizations()).hasSize(1);
    assertThat(claims.getOrganizationInformation().organizations().getFirst().orgNumber()).isEqualTo("55555");
  }

  @Test
  @DisplayName("Multiple organizations are all present in RegistryClaims")
  void multipleOrganizationsAllPresentInClaims() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("org", List.of(
            Map.of("orgNumber", "55555", "orgName", "Pensionsmyndigheten", "entity_prefix", "https://www.pm.se/oidf"),
            Map.of("orgNumber", "66666", "orgName", "Arbetsförmedlingen", "entity_prefix", "https://www.af.se/oidf")
        ))
        .claim("scope", List.of("read"))
        .build();

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrganizationInformation().organizations())
        .extracting(OrganizationRecord::orgNumber)
        .containsExactlyInAnyOrder("55555", "66666");
  }

  @Test
  @DisplayName("getOrganizationRecordByOrgNumber returns the matching organization")
  void getOrganizationRecordByOrgNumberReturnsMatch() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThat(claims.getOrganizationRecordByOrgNumber("55555").orgName())
        .isEqualTo("Pensionsmyndigheten");
  }

  @Test
  @DisplayName("getOrganizationRecordByOrgNumber throws when organization not found")
  void getOrganizationRecordByOrgNumberThrowsWhenNotFound() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final RegistryClaims claims = (RegistryClaims) converter.convert(jwt);

    assertThatThrownBy(() -> claims.getOrganizationRecordByOrgNumber("99999"))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("Missing org claim throws IllegalArgumentException")
  void missingOrgClaimThrowsIllegalArgumentException() {
    final Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("test-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("scope", List.of("read"))
        .build();

    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Token is marked as authenticated after conversion")
  void tokenIsAuthenticatedAfterConversion() {
    final Jwt jwt = buildJwt(Map.of("scope", List.of("read"), "preferred_username", "alice"));

    final AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.isAuthenticated()).isTrue();
  }
}