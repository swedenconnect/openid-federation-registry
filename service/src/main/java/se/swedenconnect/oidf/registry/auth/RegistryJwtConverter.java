/*
 * Copyright 2025 Sweden Connect
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
package se.swedenconnect.oidf.registry.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import se.swedenconnect.oidf.registry.config.SecurityConfig;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.service.OrganizationService;

import java.util.List;

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

    final List<OrganizationEntity> orgs = information.organizations()
        .stream()
        .map(orgInfo -> this.organizationService.findCreate(orgInfo.orgNumber(), orgInfo.orgName()))
        .toList();

    final SecurityConfig.RegistryClaims registryClaims = new SecurityConfig.RegistryClaims(jwt, orgs, information);
    registryClaims.setAuthenticated(true);
    return registryClaims;
  }
}
