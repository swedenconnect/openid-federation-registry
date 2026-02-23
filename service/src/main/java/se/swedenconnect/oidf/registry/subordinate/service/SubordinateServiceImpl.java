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

package se.swedenconnect.oidf.registry.subordinate.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.policy.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.mapper.DtoToSubordinateMapper;
import se.swedenconnect.oidf.registry.subordinate.mapper.SubordinateToDtoMapper;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link SubordinateService}.
 *
 * @author Per Fredrik Plars
 */
@Service
public class SubordinateServiceImpl implements SubordinateService {

  private final SubordinateRepository subordinateRepository;
  private final TaImRepository taImRepository;
  private final PolicyRepository policyRepository;
  private final EntityRepository entityRepository;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param subordinateRepository the subordinate repository
   * @param taImRepository the TaIm repository
   * @param policyRepository the policy repository
   * @param entityRepository the organization service
   * @param auditService the audit service
   */
  public SubordinateServiceImpl(final SubordinateRepository subordinateRepository,
      final TaImRepository taImRepository,
      final PolicyRepository policyRepository,
      final EntityRepository entityRepository,
      final RegistryAuditService auditService) {
    this.subordinateRepository = subordinateRepository;
    this.taImRepository = taImRepository;
    this.policyRepository = policyRepository;
    this.entityRepository = entityRepository;
    this.auditService = auditService;
  }

  private static SubordinateDto toDto(final Subordinate entity) {
    return SubordinateToDtoMapper.toDto(entity);
  }

  private TrustAnchorIntermediateModule findTaImOrThrow(final OrganizationRecord organizationRecord,
      final UUID taImId) {
    return this.taImRepository.findByOrgNumberAndTaImId(
        organizationRecord.orgNumber(), taImId).orElseThrow(() -> new RegistryServerException(
        ErrorTypes.NOT_FOUND, "No TaIm found for id %s".formatted(taImId)));
  }

  private Subordinate findSubordinateOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    //TODO: Make this type of query in repository with a join sql command.
    return this.subordinateRepository.findById(id)
        .filter(sub -> {
          final TrustAnchorIntermediateModule taIm = sub.getTaIm();
          return taIm.getOrganization().getOrgNumber().equals(organizationRecord.orgNumber());
        })
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No subordinate found for id %s".formatted(id)));
  }

  @Override
  @Transactional(readOnly = true)
  public SubordinateDto getSubordinate(final OrganizationRecord organizationRecord, final UUID id) {
    final Subordinate entity = this.findSubordinateOrThrow(organizationRecord, id);
    return toDto(entity);
  }

  @Override
  @Transactional
  public SubordinateDto createSubordinate(final OrganizationRecord organizationRecord,
      final SubordinateDto input) {
    final UUID id = UUID.randomUUID();
    return this.createSubordinateWithId(organizationRecord, id, input);
  }

  @Override
  @Transactional
  public SubordinateDto createSubordinateWithId(final OrganizationRecord organizationRecord, final UUID id,
      final SubordinateDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final TrustAnchorIntermediateModule taIm = this.findTaImOrThrow(organizationRecord, input.getTaImId());

    final Subordinate subordinateEntity = DtoToSubordinateMapper.toEntity(id, input, taIm);

    // If automatic resolve is selected, the system try to find a hosted entity. If not an exception is thrown.
    if (Optional.ofNullable(input.getEcLocationAutomaticResolve()).orElse(false)) {
      this.entityRepository.findByOrgNumberAndEntityKeyTypeAndIssuer(taIm.getOrganization().getOrgNumber(),
              EntityType.HOSTED_ENTITY, subordinateEntity.getEntityidentifier())
          .orElseThrow(() ->
              new RegistryServerException(ErrorTypes.RELATION_NOT_FOUND,
                  "No hosted entity found for entityid %s".formatted(subordinateEntity.getEntityidentifier()))
          );
    }

    this.subordinateRepository.save(subordinateEntity);
    final SubordinateDto dto = toDto(subordinateEntity);
    this.auditService.subordinateCreated(id, taIm.getOrganization().getInstance().getInstanceId(),
        taIm.getOrganization().getOrganizationId(), null, dto);
    return dto;
  }

  @Override
  @Transactional
  public SubordinateDto updateSubordinate(final OrganizationRecord organizationRecord, final UUID id,
      final SubordinateDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final Subordinate existing = this.findSubordinateOrThrow(organizationRecord, id);
    final SubordinateDto oldDto = toDto(existing);

    DtoToSubordinateMapper.updateEntity(existing, input);

    if (Optional.ofNullable(input.getEcLocationAutomaticResolve()).orElse(false)) {
      this.entityRepository
          .findByOrgNumberAndEntityKeyTypeAndIssuer(existing.getTaIm().getOrganization().getOrgNumber(),
              EntityType.HOSTED_ENTITY, existing.getEntityidentifier())
          .orElseThrow(() ->
              new RegistryServerException(ErrorTypes.RELATION_NOT_FOUND,
                  "No hosted entity found for entityid: %s. Subordinatestatement can not be created"
                      .formatted(existing.getEntityidentifier()))
          );
    }
    this.subordinateRepository.save(existing);
    final SubordinateDto newDto = toDto(existing);
    this.auditService.subordinateUpdated(id, existing.getTaIm().getOrganization().getInstance().getInstanceId(),
        existing.getTaIm().getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional
  public void deleteSubordinate(final OrganizationRecord organizationRecord, final UUID id) {
    final Subordinate entity = this.findSubordinateOrThrow(organizationRecord, id);
    final SubordinateDto dto = toDto(entity);
    this.subordinateRepository.delete(entity);
    this.auditService.subordinateDeleted(id, entity.getTaIm().getOrganization().getInstance().getInstanceId(),
        entity.getTaIm().getOrganization().getOrganizationId(), dto);
  }
}
