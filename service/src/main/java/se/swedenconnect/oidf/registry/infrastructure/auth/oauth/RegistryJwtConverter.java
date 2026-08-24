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

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrgRightsFactory;
import se.swedenconnect.oidf.registry.infrastructure.auth.ScopeRightsFactory;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;

/**
 * Converts a JWT into a {@link RegistryClaims} token by parsing either the {@code org_rights} claim (ID
 * token / OIDC login flow) or, when that claim is absent, the org-scoped {@code scope} claim (the separate
 * access-token flow) — see {@link OrgRightsFactory} and {@link ScopeRightsFactory} respectively. When both are
 * present, {@code org_rights} takes priority.
 *
 * @author Felix Hellman
 */
public class RegistryJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
      new JwtGrantedAuthoritiesConverter();

  @Override
  public AbstractAuthenticationToken convert(final Jwt jwt) {
    final OrgRights orgRights;
    try {
      orgRights = jwt.hasClaim("org_rights")
          ? OrgRightsFactory.fromClaims(jwt.getClaims())
          : ScopeRightsFactory.fromClaims(jwt.getClaims());
    }
    catch (final IllegalArgumentException e) {
      throw new InvalidBearerTokenException(e.getMessage(), e);
    }

    String username = jwt.getClaimAsString("preferred_username");
    if (username == null) {
      username = jwt.getSubject();
    }

    final RegistryClaims registryClaims = new RegistryClaims(
        jwt,
        orgRights,
        username,
        this.jwtGrantedAuthoritiesConverter.convert(jwt));
    registryClaims.setAuthenticated(true);
    return registryClaims;
  }
}
