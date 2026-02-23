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
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkWithSubjectsDto;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;

import java.util.Collections;
import java.util.Optional;

/**
 * Utility class for converting Trustmark objects to DTO objects.
 *
 * @author Per Fredrik Plars
 */
public final class TrustmarkToDtoMapper {
  private TrustmarkToDtoMapper() {
  }

  /**
   * Converts TrustMark to TrustmarkDto.
   *
   * @param trustMark the trust mark entity
   * @return the trustmark DTO
   */
  public static TrustmarkDto toDto(final TrustMark trustMark) {
    final TrustmarkDto dto = new TrustmarkDto();
    dto.setTrustmarkId(trustMark.getTrustmarkId());
    dto.setTrustmarkissuerId(trustMark.getTrustmarkIssuer().getTrustmarkIssuerId());
    dto.setTrustmarkType(trustMark.getTrustmarkType());
    dto.setLogoUri(trustMark.getLogoUri());
    dto.setRefUri(trustMark.getRefUri());
    dto.setDelegation(trustMark.getDelegation());
    return dto;
  }

  /**
   * Converts TrustMark to TrustmarkWithSubjectsDto including trustmark subjects.
   *
   * @param trustMark the trust mark entity
   * @return the trustmark with subjects DTO
   */
  public static TrustmarkWithSubjectsDto toDtoWithSubjects(final TrustMark trustMark) {
    final TrustmarkWithSubjectsDto dto = new TrustmarkWithSubjectsDto();
    dto.setTrustmarkId(trustMark.getTrustmarkId());
    dto.setTrustmarkissuerId(trustMark.getTrustmarkIssuer().getTrustmarkIssuerId());
    dto.setTrustmarkType(trustMark.getTrustmarkType());
    dto.setLogoUri(trustMark.getLogoUri());
    dto.setRefUri(trustMark.getRefUri());
    dto.setDelegation(trustMark.getDelegation());

    Optional.ofNullable(trustMark.getTrustmarksubjects())
        .map(trustMarkSubjects ->
            trustMarkSubjects.stream().map(TrustmarkToDtoMapper::toDto)
                .toList())
        .ifPresent(dto::setTrustmarkSubjects);

    return dto;
  }

  /**
   * Converts TrustMark to TrustmarkWithSubjectsDto with empty subjects list.
   *
   * @param trustMark the trust mark entity
   * @return the trustmark with empty subjects DTO
   */
  public static TrustmarkWithSubjectsDto toDtoWithSubjectsEmpty(final TrustMark trustMark) {
    final TrustmarkWithSubjectsDto dto = new TrustmarkWithSubjectsDto();
    dto.setTrustmarkId(trustMark.getTrustmarkId());
    dto.setTrustmarkissuerId(trustMark.getTrustmarkIssuer().getTrustmarkIssuerId());
    dto.setTrustmarkType(trustMark.getTrustmarkType());
    dto.setLogoUri(trustMark.getLogoUri());
    dto.setRefUri(trustMark.getRefUri());
    dto.setDelegation(trustMark.getDelegation());
    dto.setTrustmarkSubjects(Collections.emptyList());
    return dto;
  }

  /**
   * Converts TrustMarkSubject to TrustmarkSubjectDto.
   *
   * @param trustMarkSubject the trust mark subject entity
   * @return the trustmark subject DTO
   */
  public static TrustmarkSubjectDto toDto(final TrustMarkSubject trustMarkSubject) {
    final TrustmarkSubjectDto dto = new TrustmarkSubjectDto();
    dto.setTrustmarksubjectId(trustMarkSubject.getTrustmarksubjectId());
    dto.setTrustmarkId(trustMarkSubject.getTrustmarkId());
    dto.setSubject(trustMarkSubject.getSubject());
    dto.setRevoked(trustMarkSubject.getRevoked());
    dto.setGranted(trustMarkSubject.getGranted());
    dto.setExpires(trustMarkSubject.getExpires());
    return dto;
  }

}
