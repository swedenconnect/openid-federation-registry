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

package se.swedenconnect.oidf.registry.trustmark.mapper;

import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;

import java.util.UUID;

/**
 * Utility class for converting DTO objects to Trustmark objects.
 *
 * @author Per Fredrik Plars
 */
public final class DtoToTrustmarkMapper {
  private DtoToTrustmarkMapper() {
  }

  /**
   * Converts TrustmarkDto to TrustMark.
   *
   * @param id the trust mark ID
   * @param dto the trustmark DTO
   * @param trustmarkIssuer trustmarkissuer
   * @return the trust mark entity
   */
  public static TrustMark toEntity(final UUID id,
      final TrustmarkDto dto,
      final TrustMarkIssuer trustmarkIssuer) {
    return TrustMark.builder()
        .trustmarkId(id)
        .trustmarkIssuer(trustmarkIssuer)
        .trustmarkType(dto.getTrustmarkType())
        .logoUri(dto.getLogoUri())
        .refUri(dto.getRefUri())
        .delegation(dto.getDelegation())
        .build();
  }

  /**
   * Converts TrustmarkSubjectDto to TrustMarkSubject.
   *
   * @param id the trust mark subject ID
   * @param dto the trustmark subject DTO
   * @param trustMark the trust mark entity
   * @return the trust mark subject entity
   */
  public static TrustMarkSubject toEntity(final UUID id,
      final TrustmarkSubjectDto dto,
      final TrustMark trustMark) {
    return TrustMarkSubject.builder()
        .trustmarksubjectId(id)
        .trustMark(trustMark)
        .subject(dto.getSubject())
        .revoked(dto.getRevoked())
        .granted(dto.getGranted())
        .expires(dto.getExpires())
        .build();
  }

  /**
   * Updates TrustMark with TrustmarkDto data.
   *
   * @param entity the trust mark entity
   * @param dto the trustmark DTO
   */
  public static void updateEntity(final TrustMark entity, final TrustmarkDto dto) {

    entity.setTrustmarkType(dto.getTrustmarkType());
    entity.setLogoUri(dto.getLogoUri());
    entity.setRefUri(dto.getRefUri());
    entity.setDelegation(dto.getDelegation());
  }

  /**
   * Updates TrustMarkSubject with TrustmarkSubjectDto data.
   *
   * @param entity the trust mark subject entity
   * @param dto the trustmark subject DTO
   */
  public static void updateEntity(final TrustMarkSubject entity, final TrustmarkSubjectDto dto) {
    entity.setSubject(dto.getSubject());
    entity.setRevoked(dto.getRevoked());
    entity.setGranted(dto.getGranted());
    entity.setExpires(dto.getExpires());
  }
}
