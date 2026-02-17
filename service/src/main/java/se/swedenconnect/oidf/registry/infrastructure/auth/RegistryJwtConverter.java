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

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import se.swedenconnect.oidf.registry.infrastructure.config.SecurityConfig;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Jwt converter for extracting relevant claims.
 *
 * @author Felix Hellman
 */
public class RegistryJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final OrganizationService organizationService;

  /**
   * Constructor
   *
   * @param organizationService to use for creating/finding org
   */
  public RegistryJwtConverter(final OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @Override
  public AbstractAuthenticationToken convert(final Jwt jwt) {
    final OrganizationInformation information = OrganizationInformationFactory.getInformation(jwt.getClaims());
    Assert.isTrue(!information.organizations().isEmpty(), "Organizations can not be empty");
    String username = jwt.getClaimAsString("preferred_username");
    if (username == null) {
      username = jwt.getSubject();
    }
    final Collection<GrantedAuthority> authorities = this.extractAuthorities(jwt);
    final SecurityConfig.RegistryClaims registryClaims =
        new SecurityConfig.RegistryClaims(jwt, information, username, authorities);
    registryClaims.setAuthenticated(true);
    return registryClaims;
  }

  private Collection<GrantedAuthority> extractAuthorities(final Jwt jwt) {
    final List<String> scopes = jwt.getClaimAsStringList("scope");
    if (scopes == null || scopes.isEmpty()) {
      return Collections.emptyList();
    }
    return scopes.stream()
        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
        .collect(Collectors.toList());
  }
}
