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

package se.swedenconnect.oidf.registry.module.mapper;

import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.model.Resolver;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.organization.model.Organization;

import java.util.UUID;

/**
 * Utility class for converting DTO objects to Module objects.
 *
 * @author Per Fredrik Plars
 */
public final class DtoToModuleMapper {
  private DtoToModuleMapper() {
  }

  /**
   * Converts TrustAnchorDto to TrustAnchorIntermediateModule.
   *
   * @param id the module ID
   * @param dto the trust anchor DTO
   * @param federationEntity the federation entity
   * @param organization the organization entity
   * @return the module entity
   */
  public static TrustAnchorIntermediateModule toEntity(final UUID id,
      final TrustAnchorDto dto,
      final FederationEntity federationEntity,
      final Organization organization) {
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setTaImId(id);
    module.setModuleType(ModuleType.TRUSTANCHOR);
    module.setEntity(federationEntity);
    module.setOrganization(organization);
    module.setActive(dto.getActive());
    module.setTrustMarkIssuers(dto.getTrustMarkIssuers());

    return module;
  }

  /**
   * Converts IntermediateDto to TrustAnchorIntermediateModule.
   *
   * @param id the module ID
   * @param dto the intermediate DTO
   * @param federationEntity the federation entity
   * @param organization the organization entity
   * @return the module entity
   */
  public static TrustAnchorIntermediateModule toEntity(final UUID id,
      final IntermediateDto dto,
      final FederationEntity federationEntity,
      final Organization organization) {
    final TrustAnchorIntermediateModule module = new TrustAnchorIntermediateModule();
    module.setTaImId(id);
    module.setModuleType(ModuleType.INTERMEDIATE);
    module.setEntity(federationEntity);
    module.setOrganization(organization);
    module.setActive(dto.getActive());

    return module;
  }

  /**
   * Converts ResolverDto to Resolver.
   *
   * @param id the resolver ID
   * @param dto the resolver DTO
   * @param federationEntity the federation entity
   * @return the resolver entity
   */
  public static Resolver toEntity(final UUID id,
      final ResolverDto dto,
      final FederationEntity federationEntity) {
    return Resolver.builder()
        .resolverId(id)
        .entity(federationEntity)
        .active(dto.getActive())
        .resolveResponseDuration(dto.getResolveResponseDuration())
        .trustAnchor(dto.getTrustAnchor())
        .trustedKeys(dto.getTrustedKeys())
        .stepRetryDuration(dto.getStepRetryDuration())
        .stepCachedValueThreshold(dto.getStepCachedValueThreshold())
        .build();
  }

  /**
   * Updates TrustAnchorIntermediateModule with TrustAnchorDto data.
   *
   * @param module the module entity
   * @param dto the trust anchor DTO
   */
  public static void updateIntermediate(final TrustAnchorIntermediateModule module, final TrustAnchorDto dto) {
    module.setActive(dto.getActive());
    module.setTrustMarkIssuers(dto.getTrustMarkIssuers());
  }

  /**
   * Updates TrustAnchorIntermediateModule with IntermediateDto data.
   *
   * @param module the module entity
   * @param dto the intermediate DTO
   */
  public static void updateIntermediate(final TrustAnchorIntermediateModule module, final IntermediateDto dto) {
    module.setActive(dto.getActive());
  }

  /**
   * Updates Resolver with ResolverDto data.
   *
   * @param entity the resolver entity
   * @param dto the resolver DTO
   */
  public static void updateEntity(final Resolver entity, final ResolverDto dto) {
    entity.setActive(dto.getActive());
    entity.setResolveResponseDuration(dto.getResolveResponseDuration());
    entity.setTrustAnchor(dto.getTrustAnchor());
    entity.setTrustedKeys(dto.getTrustedKeys());
    entity.setStepRetryDuration(dto.getStepRetryDuration());
    entity.setStepCachedValueThreshold(dto.getStepCachedValueThreshold());
  }

  /**
   * Converts TrustmarkIssuerDto to TrustMarkIssuer.
   *
   * @param id the trustmark issuer ID
   * @param dto the trustmark issuer DTO
   * @param federationEntity the entity entity
   * @return the trustmark issuer entity
   */
  public static TrustMarkIssuer toEntity(final UUID id,
      final TrustmarkIssuerDto dto,
      final FederationEntity federationEntity) {
    return TrustMarkIssuer.builder()
        .trustmarkIssuerId(id)
        .entity(federationEntity)
        .active(dto.getActive())
        .trustMarkTokenValidityDuration(dto.getTrustMarkTokenValidityDuration())
        .build();
  }

  /**
   * Updates TrustMarkIssuer with TrustmarkIssuerDto data.
   *
   * @param entity the trustmark issuer entity
   * @param dto the trustmark issuer DTO
   */
  public static void updateEntity(final TrustMarkIssuer entity, final TrustmarkIssuerDto dto) {
    entity.setActive(dto.getActive());
    entity.setTrustMarkTokenValidityDuration(dto.getTrustMarkTokenValidityDuration());
  }
}
