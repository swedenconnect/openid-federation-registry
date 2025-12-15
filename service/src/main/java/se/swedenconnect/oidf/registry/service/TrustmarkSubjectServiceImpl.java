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
import se.swedenconnect.oidf.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.EntityToDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkSubjectRepository;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

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
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param trustMarkSubjectRepository the trust mark subject repository
   * @param trustMarkRepository the trust mark repository
   * @param auditService the audit service
   */
  public TrustmarkSubjectServiceImpl(final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final TrustMarkRepository trustMarkRepository,
      final RegistryAuditService auditService) {
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.auditService = auditService;
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

  /**
   * Creates a trust mark subject.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   * @param input the trust mark subject data
   * @return the created trust mark subject
   */
  @Override
  @Transactional
  public TrustmarkSubjectDto createTrustmarkSubject(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkSubjectDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final UUID trustmarkId = input.getTrustmarkId();
    final TrustMarkEntity trustMarkEntity = this.findTrustMarkOrThrow(organizationRecord, trustmarkId);

    final TrustMarkSubjectEntity entity =
        EntityToDto.toEntity(id, input, trustMarkEntity);

    this.trustMarkSubjectRepository.save(entity);
    final TrustmarkSubjectDto dto = EntityToDto.toDtoPolicy(entity);
    this.auditService.trustmarkSubjectCreated(id, trustmarkId, null, dto);
    return dto;
  }

  /**
   * Updates a trust mark subject.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   * @param input the trust mark subject data
   * @return the updated trust mark subject
   */
  @Override
  @Transactional
  public TrustmarkSubjectDto updateTrustmarkSubject(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkSubjectDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final TrustMarkSubjectEntity existing = this.findSubjectOrThrow(organizationRecord, id);
    final TrustmarkSubjectDto oldDto = EntityToDto.toDtoPolicy(existing);
    final UUID trustmarkId = existing.getTrustMark().getTrustmarkId();

    EntityToDto.updateEntity(existing, input);

    this.trustMarkSubjectRepository.save(existing);
    final TrustmarkSubjectDto newDto = EntityToDto.toDtoPolicy(existing);
    this.auditService.trustmarkSubjectUpdated(id, trustmarkId, oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a trust mark subject by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   * @return the trust mark subject
   */
  @Override
  @Transactional(readOnly = true)
  public TrustmarkSubjectDto getTrustmarkSubject(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkSubjectEntity entity = this.findSubjectOrThrow(organizationRecord, id);
    return EntityToDto.toDtoPolicy(entity);
  }

  /**
   * Deletes a trust mark subject.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark subject ID
   */
  @Override
  @Transactional
  public void deleteTrustmarkSubject(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkSubjectEntity entity = this.findSubjectOrThrow(organizationRecord, id);
    final TrustmarkSubjectDto dto = EntityToDto.toDtoPolicy(entity);
    final UUID trustmarkId = entity.getTrustMark().getTrustmarkId();
    this.trustMarkSubjectRepository.delete(entity);
    this.auditService.trustmarkSubjectDeleted(id, trustmarkId, dto);
  }
}


