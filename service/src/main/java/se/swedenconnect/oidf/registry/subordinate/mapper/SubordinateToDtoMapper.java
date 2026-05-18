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

import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;

import java.util.Optional;

/**
 * Utility class for converting Subordinate objects to DTO objects.
 *
 * @author Per Fredrik Plars
 */
public final class SubordinateToDtoMapper {
  private SubordinateToDtoMapper() {
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

    return dto;
  }
}
