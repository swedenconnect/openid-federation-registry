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
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformationFactory;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationInformation;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * RegistryOidcUser that can handle the selected OrganizationRecord:s
 *
 * @author Per Fredrik Plars
 */
public class RegistryOidcUser implements OidcUser, Serializable {

  final OidcUser defaultOidcUser;

  /**
   * OidcUser that contains information about the login.
   *
   * @param oidcUser OidcUser from the DefaultOidcUser
   */
  public RegistryOidcUser(final OidcUser oidcUser) {
    this.defaultOidcUser = oidcUser;
  }

  /**
   * Retrieves an {@link OrganizationRecord} that matches the given organization number from the list of available
   * organizations. If no matching record is found, an exception is thrown.
   *
   * @param orgNumber the unique identifier of the organization for which the record is to be retrieved
   * @return the {@link OrganizationRecord} corresponding to the specified organization number
   */
  public OrganizationRecord getOrganizationRecordByOrgNumber(final String orgNumber) {
    final OrganizationInformation oi = OrganizationInformationFactory.getInformation(this.getIdToken().getClaims());
    return oi
        .organizations()
        .stream()
        .filter(organizationRecord -> Objects.nonNull(orgNumber))
        .filter(e -> e.orgNumber().equals(orgNumber))
        .findFirst()
        .orElse(oi.organizations().getFirst());
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
