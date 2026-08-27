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
package se.swedenconnect.oidf.registry.infrastructure.auth.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import se.swedenconnect.iam.security.claims.OrgRightsClaim;

import java.util.Collection;

/**
 * JWT authentication token enriched with the parsed {@code org_rights} claim.
 *
 * @author Per Fredrik Plars
 */
@Getter
public class RegistryClaims extends JwtAuthenticationToken {

  private final OrgRightsClaim orgRights;

  /**
   * Constructs a new instance.
   *
   * @param jwt        the JWT token
   * @param orgRights  parsed org_rights claim
   * @param userName   principal name
   * @param authorities granted authorities
   */
  public RegistryClaims(final Jwt jwt,
      final OrgRightsClaim orgRights,
      final String userName,
      final Collection<? extends GrantedAuthority> authorities) {
    super(jwt, authorities, userName);
    this.orgRights = orgRights;
  }
}
