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

package se.swedenconnect.oidf.registry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.api.dto.EntityToDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.api.dto.input.TrustmarkSubjectInputDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkSubjectRepository;

import java.util.UUID;

/**
 * Default implementation of {@link TrustmarkSubjectService}.
 *
 * @author Per Fredrik Plars
 */
@Service
public class TrustmarkSubjectServiceImpl implements TrustmarkSubjectService {

  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final TrustMarkRepository trustMarkRepository;

  public TrustmarkSubjectServiceImpl(final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final TrustMarkRepository trustMarkRepository) {
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.trustMarkRepository = trustMarkRepository;
  }

  private TrustMarkSubjectEntity findSubjectOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    return this.trustMarkSubjectRepository.findByOrgNumberAndTrustmarkId(
            organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark subject found for id %s".formatted(id)));
  }

  private TrustMarkEntity findTrustMarkOrThrow(final OrganizationRecord organizationRecord, final UUID trustmarkId) {
    return this.trustMarkRepository.findByOrgNumberAndTrustmarkId(
            organizationRecord.orgNumber(), trustmarkId)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(trustmarkId)));
  }

  @Override
  @Transactional
  public TrustmarkSubjectDto createTrustmarkSubject(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkSubjectInputDto input) {

    final UUID trustmarkId = UUID.fromString(input.getTrustmarkId());
    final TrustMarkEntity trustMarkEntity = this.findTrustMarkOrThrow(organizationRecord, trustmarkId);

    final TrustMarkSubjectEntity entity =
        EntityToDto.toEntity(id, input, trustMarkEntity);

    this.trustMarkSubjectRepository.save(entity);
    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public TrustmarkSubjectDto updateTrustmarkSubject(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkSubjectInputDto input) {

    final TrustMarkSubjectEntity existing = this.findSubjectOrThrow(organizationRecord, id);

    EntityToDto.updateEntity(existing, input);

    this.trustMarkSubjectRepository.save(existing);
    return EntityToDto.toDto(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public TrustmarkSubjectDto getTrustmarkSubject(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkSubjectEntity entity = this.findSubjectOrThrow(organizationRecord, id);
    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public void deleteTrustmarkSubject(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkSubjectEntity entity = this.findSubjectOrThrow(organizationRecord, id);
    this.trustMarkSubjectRepository.delete(entity);
  }
}


