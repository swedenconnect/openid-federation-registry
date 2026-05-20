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

package se.swedenconnect.oidf.registry.subordinate.mapper;

import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for converting DTO objects to Subordinate objects.
 *
 * @author Per Fredrik Plars
 */
public final class DtoToSubordinateMapper {
  private DtoToSubordinateMapper() {
  }

  /**
   * Converts SubordinateDto to Subordinate.
   *
   * @param id the subordinate ID
   * @param dto the subordinate DTO
   * @param taIm the TaIm entity
   * @return the subordinate entity
   */
  public static Subordinate toEntity(final UUID id,
      final SubordinateDto dto,
      final TrustAnchorIntermediateModule taIm) {
    final Subordinate entity = new Subordinate();
    entity.setSubordinateId(id);
    entity.setTaIm(taIm);
    entity.setJwks(dto.getJwks());
    entity.setEntityidentifier(dto.getEntityIdentifier());

    Optional.ofNullable(dto.getCrit()).ifPresent(entity::setCrit);
    Optional.ofNullable(dto.getMetadataPolicyCrit()).ifPresent(entity::setMetadataPolicyCrit);

    if (!entity.isEcLocationAutomatic()) {
      entity.setEcLocation(dto.getEcLocation());
    }
    entity.setEcLocationAutomatic(Optional.ofNullable(dto.getEcLocationAutomaticResolve()).orElse(false));
    entity.setMetadataPolicy(dto.getMetadataPolicy());
    return entity;
  }

  /**
   * Updates Subordinate with SubordinateDto data.
   *
   * @param entity the subordinate entity
   * @param dto the subordinate DTO
   */
  public static void updateEntity(final Subordinate entity, final SubordinateDto dto) {
    entity.setJwks(dto.getJwks());
    entity.setEntityidentifier(dto.getEntityIdentifier());

    entity.setCrit(Optional.ofNullable(dto.getCrit()).orElse(Collections.emptyList()));
    entity.setMetadataPolicyCrit(Optional.ofNullable(dto.getMetadataPolicyCrit()).orElse(Collections.emptyList()));

    entity.setEcLocation(dto.getEcLocation());
    entity.setEcLocationAutomatic(Optional.ofNullable(dto.getEcLocationAutomaticResolve()).orElse(false));
    entity.setMetadataPolicy(dto.getMetadataPolicy());
  }
}
