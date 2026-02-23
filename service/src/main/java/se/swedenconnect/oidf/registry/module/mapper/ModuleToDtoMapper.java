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

import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.model.Resolver;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.mapper.SubordinateToDtoMapper;

import java.util.List;

/**
 * Utility class for converting Module objects to DTO objects.
 *
 * @author Per Fredrik Plars
 */
public final class ModuleToDtoMapper {
  private ModuleToDtoMapper() {
  }

  /**
   * Converts TrustAnchorIntermediateModule to TrustAnchorDto.
   *
   * @param moduleEntity the module entity
   * @return the trust anchor DTO
   */
  public static TrustAnchorDto toDto(final TrustAnchorIntermediateModule moduleEntity) {
    if (!moduleEntity.isOfType(ModuleType.TRUSTANCHOR)) {
      throw new IllegalArgumentException("Module is not a TrustAnchor");
    }
    final TrustAnchorDto dto = new TrustAnchorDto();
    dto.setTrustAnchorId(moduleEntity.getTaImId());
    dto.setEntityId(moduleEntity.getEntity().getEntityId());
    dto.setActive(moduleEntity.getActive());
    dto.setTrustMarkIssuers(moduleEntity.getTrustMarkIssuers());

    // Add subordinates
    if (moduleEntity.getSubordinates() != null) {
      final List<SubordinateDto> subordinates = moduleEntity.getSubordinates().stream()
          .map(SubordinateToDtoMapper::toDto)
          .toList();
      dto.setSubordinates(subordinates);
    }

    return dto;
  }

  /**
   * Converts TrustAnchorIntermediateModule to IntermediateDto.
   *
   * @param moduleEntity the module entity
   * @return the intermediate DTO
   */
  public static IntermediateDto toDtoIntermediate(final TrustAnchorIntermediateModule moduleEntity) {
    if (!moduleEntity.isOfType(ModuleType.INTERMEDIATE)) {
      throw new IllegalArgumentException("Module is not a INTERMEDIATE");
    }

    final IntermediateDto dto = new IntermediateDto();
    dto.setIntermediateId(moduleEntity.getTaImId());
    dto.setEntityId(moduleEntity.getEntity().getEntityId());
    dto.setActive(moduleEntity.getActive());

    // Add subordinates
    if (moduleEntity.getSubordinates() != null) {
      final List<SubordinateDto> subordinates = moduleEntity.getSubordinates().stream()
          .map(SubordinateToDtoMapper::toDto)
          .toList();
      dto.setSubordinates(subordinates);
    }

    return dto;
  }

  /**
   * Converts Resolver to ResolverDto.
   *
   * @param resolverEntity the resolver entity
   * @return the resolver DTO
   */
  public static ResolverDto toDto(final Resolver resolverEntity) {
    final ResolverDto dto = new ResolverDto();
    dto.setResolverId(resolverEntity.getResolverId());
    dto.setEntityId(resolverEntity.getEntity().getEntityId());
    dto.setActive(resolverEntity.getActive());
    dto.setResolveResponseDuration(resolverEntity.getResolveResponseDuration());
    dto.setTrustAnchor(resolverEntity.getTrustAnchor());
    dto.setTrustedKeys(resolverEntity.getTrustedKeys());
    dto.setStepRetryDuration(resolverEntity.getStepRetryDuration());
    dto.setStepCachedValueThreshold(resolverEntity.getStepCachedValueThreshold());
    return dto;
  }

  /**
   * Converts TrustMarkIssuer to TrustmarkIssuerDto.
   *
   * @param entity the trustmark issuer entity
   * @return the trustmark issuer DTO
   */
  public static TrustmarkIssuerDto toDto(final TrustMarkIssuer entity) {
    final TrustmarkIssuerDto dto = new TrustmarkIssuerDto();
    dto.setTrustmarkIssuerId(entity.getTrustmarkIssuerId());
    dto.setEntityId(entity.getEntity().getEntityId());
    dto.setActive(entity.getActive());
    dto.setTrustMarkTokenValidityDuration(entity.getTrustMarkTokenValidityDuration());
    return dto;
  }
}
