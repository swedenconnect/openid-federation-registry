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
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformation;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformationFactory;

/**
 * Jwt converter for extracting relevant claims.
 *
 * @author Felix Hellman
 */
public class RegistryJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  /**
   * Constructor
   *
   */
  public RegistryJwtConverter() {

  }

  @Override
  public AbstractAuthenticationToken convert(final Jwt jwt) {
    final OrganizationInformation information = OrganizationInformationFactory.getInformation(jwt.getClaims());
    Assert.isTrue(!information.organizations().isEmpty(), "Organizations can not be empty");
    String username = jwt.getClaimAsString("preferred_username");
    if (username == null) {
      username = jwt.getSubject();
    }

    final RegistryClaims registryClaims = new RegistryClaims(
        jwt,
        information,
        username,
        this.jwtGrantedAuthoritiesConverter.convert(jwt));
    registryClaims.setAuthenticated(true);
    return registryClaims;
  }

}
