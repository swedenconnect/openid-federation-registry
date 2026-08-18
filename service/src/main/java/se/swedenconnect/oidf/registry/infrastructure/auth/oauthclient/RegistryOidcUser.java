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
package se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrgRightsFactory;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrgRights;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * OIDC user wrapper that exposes the parsed {@code org_rights} claim.
 *
 * @author Per Fredrik Plars
 */
public class RegistryOidcUser implements OidcUser, Serializable {

  private final OidcUser defaultOidcUser;

  /**
   * Creates a new wrapper.
   *
   * @param oidcUser the underlying OIDC user to delegate to
   */
  public RegistryOidcUser(final OidcUser oidcUser) {
    this.defaultOidcUser = oidcUser;
  }

  /**
   * Returns the parsed org_rights from the OIDC ID token claims.
   *
   * @return parsed OrgRights
   */
  public OrgRights getOrgRights() {
    return OrgRightsFactory.fromClaims(this.getIdToken().getClaims());
  }

  @Override
  public Map<String, Object> getAttributes() {
    return this.defaultOidcUser.getAttributes();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return this.defaultOidcUser.getAuthorities();
  }

  @Override
  public String getName() {
    return this.defaultOidcUser.getName();
  }

  @Override
  public Map<String, Object> getClaims() {
    return this.defaultOidcUser.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return this.defaultOidcUser.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return this.defaultOidcUser.getIdToken();
  }
}
