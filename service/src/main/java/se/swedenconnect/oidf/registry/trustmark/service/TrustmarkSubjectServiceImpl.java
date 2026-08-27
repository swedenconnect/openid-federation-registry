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

package se.swedenconnect.oidf.registry.trustmark.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.persistence.InsertOnly;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.trustmark.mapper.DtoToTrustmarkMapper;
import se.swedenconnect.oidf.registry.trustmark.mapper.TrustmarkToDtoMapper;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkSubjectRepository;

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
  private final OrganizationService organizationService;

  /**
   * Constructor.
   *
   * @param trustMarkSubjectRepository the trust mark subject repository
   * @param trustMarkRepository the trust mark repository
   * @param auditService the audit service
   * @param organizationService service for resolving the calling organization
   */
  public TrustmarkSubjectServiceImpl(final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final TrustMarkRepository trustMarkRepository,
      final RegistryAuditService auditService,
      final OrganizationService organizationService) {
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.auditService = auditService;
    this.organizationService = organizationService;
  }

  private UUID resolveOrganizationIdOrThrow(final OrganizationRecord organizationRecord,
      final RegistryServerException notFound) {
    return this.organizationService.find(organizationRecord)
        .map(Organization::getOrganizationId)
        .orElseThrow(() -> notFound);
  }

  private TrustMarkSubject findSubjectOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    final RegistryServerException notFound = new RegistryServerException(
        ErrorTypes.NOT_FOUND, "No trust mark subject found for id %s".formatted(id));
    final UUID organizationId = this.resolveOrganizationIdOrThrow(organizationRecord, notFound);
    return this.trustMarkSubjectRepository.findByOrganizationIdAndTrustmarkId(organizationId, id)
        .orElseThrow(() -> notFound);
  }

  private TrustMark findTrustMarkOrThrow(final OrganizationRecord organizationRecord, final UUID trustmarkId) {
    final RegistryServerException notFound = new RegistryServerException(
        ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(trustmarkId));
    final UUID organizationId = this.resolveOrganizationIdOrThrow(organizationRecord, notFound);
    return this.trustMarkRepository.findByOrganizationIdAndTrustmarkId(organizationId, trustmarkId)
        .orElseThrow(() -> notFound);
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
    ValidateDto.init(organizationRecord).validate(input);

    final UUID trustmarkId = input.getTrustmarkId();
    final TrustMark trustMarkEntity = this.findTrustMarkOrThrow(organizationRecord, trustmarkId);

    final TrustMarkSubject entity =
        DtoToTrustmarkMapper.toEntity(id, input, trustMarkEntity);

    InsertOnly.save(this.trustMarkSubjectRepository, entity, "A trust mark subject with this ID already exists: " + id);
    final TrustmarkSubjectDto dto = TrustmarkToDtoMapper.toDto(entity);
    this.auditService.trustmarkSubjectCreated(id,
        trustMarkEntity.getTrustmarkIssuer().getEntity().getOrganization().getInstance().getInstanceId(),
        trustmarkId,
        trustMarkEntity.getTrustmarkIssuer().getEntity().getOrganization().getOrganizationId(), null, dto);
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
    ValidateDto.init(organizationRecord).validate(input);

    final TrustMarkSubject existing = this.findSubjectOrThrow(organizationRecord, id);
    final TrustmarkSubjectDto oldDto = TrustmarkToDtoMapper.toDto(existing);
    final UUID trustmarkId = existing.getTrustMark().getTrustmarkId();

    DtoToTrustmarkMapper.updateEntity(existing, input);

    this.trustMarkSubjectRepository.save(existing);
    final TrustmarkSubjectDto newDto = TrustmarkToDtoMapper.toDto(existing);
    this.auditService.trustmarkSubjectUpdated(id,
        existing.getTrustMark().getTrustmarkIssuer().getEntity().getOrganization().getInstance().getInstanceId(),
        trustmarkId,
        existing.getTrustMark().getTrustmarkIssuer().getEntity().getOrganization().getOrganizationId(), oldDto,
        newDto);
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
    final TrustMarkSubject entity = this.findSubjectOrThrow(organizationRecord, id);
    return TrustmarkToDtoMapper.toDto(entity);
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
    final TrustMarkSubject entity = this.findSubjectOrThrow(organizationRecord, id);
    final TrustmarkSubjectDto dto = TrustmarkToDtoMapper.toDto(entity);
    final UUID trustmarkId = entity.getTrustMark().getTrustmarkId();
    this.trustMarkSubjectRepository.delete(entity);
    this.auditService.trustmarkSubjectDeleted(id,
        entity.getTrustMark().getTrustmarkIssuer().getEntity().getOrganization().getInstance().getInstanceId(),
        trustmarkId,
        entity.getTrustMark().getTrustmarkIssuer().getEntity().getOrganization().getOrganizationId(), dto);
  }
}
