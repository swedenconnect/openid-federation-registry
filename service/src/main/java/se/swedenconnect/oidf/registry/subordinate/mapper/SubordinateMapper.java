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
import se.swedenconnect.oidf.registry.subordinate.dto.ConstraintsDto;
import se.swedenconnect.oidf.registry.subordinate.dto.NamingConstraintsDto;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for converting DTO objects to Subordinate objects.
 *
 * @author Per Fredrik Plars
 */
public final class SubordinateMapper {
  private SubordinateMapper() {
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
    entity.setMetadata(dto.getMetadata());
    applyConstraints(entity, dto.getConstraints());
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
    entity.setMetadata(dto.getMetadata());
    applyConstraints(entity, dto.getConstraints());
  }

  /**
   * Converts Subordinate to SubordinateDto.
   *
   * @param subordinate the subordinate entity
   * @return the subordinate DTO
   */
  public static SubordinateDto toDto(final Subordinate subordinate) {
    final SubordinateDto dto = new SubordinateDto();
    dto.setSubordinateId(subordinate.getSubordinateId());
    dto.setTaImId(subordinate.getTaIm().getTaImId());
    dto.setJwks(subordinate.getJwks());
    dto.setEntityIdentifier(subordinate.getEntityidentifier());

    Optional.ofNullable(subordinate.getCrit()).ifPresent(dto::setCrit);
    Optional.ofNullable(subordinate.getMetadataPolicyCrit()).ifPresent(dto::setMetadataPolicyCrit);

    dto.setEcLocation(subordinate.getEcLocation());
    dto.setEcLocationAutomaticResolve(subordinate.isEcLocationAutomatic());
    dto.setMetadataPolicy(subordinate.getMetadataPolicy());
    dto.setMetadata(subordinate.getMetadata());
    dto.setConstraints(toConstraintsDto(subordinate));
    return dto;
  }

  private static void applyConstraints(final Subordinate entity, final ConstraintsDto constraints) {
    final NamingConstraintsDto naming = constraints == null ? null : constraints.getNamingConstraints();
    entity.setConstraintsMaxPathLength(constraints == null ? null : constraints.getMaxPathLength());
    entity.setConstraintsNamingPermitted(naming == null ? null : emptyToNull(naming.getPermitted()));
    entity.setConstraintsNamingExcluded(naming == null ? null : emptyToNull(naming.getExcluded()));
    entity.setConstraintsAllowedEntityTypes(constraints == null ? null
        : emptyToNull(constraints.getAllowedEntityTypes()));
  }

  /**
   * Builds the constraints of a subordinate, or null if there are none.
   */
  private static ConstraintsDto toConstraintsDto(final Subordinate subordinate) {
    final ConstraintsDto constraints = new ConstraintsDto();
    constraints.setMaxPathLength(subordinate.getConstraintsMaxPathLength());
    constraints.setAllowedEntityTypes(subordinate.getConstraintsAllowedEntityTypes());
    if (subordinate.getConstraintsNamingPermitted() != null || subordinate.getConstraintsNamingExcluded() != null) {
      final NamingConstraintsDto naming = new NamingConstraintsDto();
      naming.setPermitted(subordinate.getConstraintsNamingPermitted());
      naming.setExcluded(subordinate.getConstraintsNamingExcluded());
      constraints.setNamingConstraints(naming);
    }
    final boolean empty = constraints.getMaxPathLength() == null && constraints.getNamingConstraints() == null
        && constraints.getAllowedEntityTypes() == null;
    return empty ? null : constraints;
  }

  private static List<String> emptyToNull(final List<String> list) {
    return list == null || list.isEmpty() ? null : list;
  }
}
