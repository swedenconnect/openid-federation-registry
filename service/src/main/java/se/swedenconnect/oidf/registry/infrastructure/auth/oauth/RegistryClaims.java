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
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformation;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;

import java.util.Collection;

/**
 * The RegistryClaims class extends JwtAuthenticationToken and provides additional information about the authenticated
 * client, including the associated organization and domain prefix.
 *
 * @author Per Fredrik Plars
 *
 */
@Getter
public class RegistryClaims extends JwtAuthenticationToken {
  private final OrganizationInformation organizationInformation;

  /**
   * Constructs a new instance of the RegistryClaims class.
   *
   * @param jwt the JWT object representing the JSON Web Token used for authentication and authorization.
   * @param information an instance of OrganizationInformation
   * @param userName to be used JwtAuthenticationToken
   * @param authorities to be used
   */
  public RegistryClaims(final Jwt jwt,
      final OrganizationInformation information,
      final String userName,
      final Collection<? extends GrantedAuthority> authorities) {
    super(jwt, authorities, userName);
    this.organizationInformation = information;
  }

  /**
   * Retrieves an {@link OrganizationRecord} that matches the given organization number from the list of available
   * organizations. If no matching record is found, an exception is thrown.
   *
   * @param orgNumber the unique identifier of the organization for which the record is to be retrieved
   * @return the {@link OrganizationRecord} corresponding to the specified organization number
   */
  public OrganizationRecord getOrganizationRecordByOrgNumber(@NonNull final String orgNumber) {
    return this.organizationInformation
        .organizations()
        .stream()
        .filter(e -> e.orgNumber().equals(orgNumber))
        .findFirst()
        .orElseThrow();
  }
}
