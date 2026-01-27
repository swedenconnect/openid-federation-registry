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

package se.swedenconnect.oidf.registry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.EntityToDto;
import se.swedenconnect.oidf.registry.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.entity.SubordinateEntity;
import se.swedenconnect.oidf.registry.entity.TaImEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.repository.TaImRepository;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

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

  private static SubordinateDto toDto(final SubordinateEntity entity) {
    return EntityToDto.toDto(entity);
  }

  private TaImEntity findTaImOrThrow(final OrganizationRecord organizationRecord, final UUID taImId) {
    return this.taImRepository.findByOrgNumberAndTaImId(
        organizationRecord.orgNumber(), taImId).orElseThrow(() -> new RegistryServerException(
        ErrorTypes.NOT_FOUND, "No TaIm found for id %s".formatted(taImId)));
  }

  private SubordinateEntity findSubordinateOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    //TODO: Make this type of query in repository with a join sql command.
    return this.subordinateRepository.findById(id)
        .filter(sub -> {
          final TaImEntity taIm = sub.getTaIm();
          return taIm.getOrganization().getOrgNumber().equals(organizationRecord.orgNumber());
        })
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No subordinate found for id %s".formatted(id)));
  }

  @Override
  @Transactional(readOnly = true)
  public SubordinateDto getSubordinate(final OrganizationRecord organizationRecord, final UUID id) {
    final SubordinateEntity entity = this.findSubordinateOrThrow(organizationRecord, id);
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

    final TaImEntity taIm = this.findTaImOrThrow(organizationRecord, input.getTaImId());

    final SubordinateEntity subordinateEntity = EntityToDto.toEntity(id, input, taIm);

    taIm.getOrganization()
        .getPolicies()
        .stream()
        .filter(policyEntity -> policyEntity.getPolicyId().equals(input.getPolicyId()))
        .findFirst()
        .ifPresentOrElse(subordinateEntity::setPolicy, () -> {
          throw new RegistryServerException(ErrorTypes.NOT_FOUND, "No policy found for id %s".formatted(id));
        });

    // If automatic resolve is selected, the system try to find a hosted entity. If not an exception is thrown.
    if (input.isEcLocationAutomaticResolve()) {
      this.entityRepository.findByOrgNumberAndEntityKeyTypeAndIssuer(taIm.getOrganization().getOrgNumber(),
              EntityKeyType.HOSTED_ENTITY, subordinateEntity.getEntityidentifier())
          .orElseThrow(() ->
              new RegistryServerException(ErrorTypes.RELATION_NOT_FOUND,
                  "No hosted entity found for entityid %s".formatted(subordinateEntity.getEntityidentifier()))
          );
    }

    this.subordinateRepository.save(subordinateEntity);
    final SubordinateDto dto = toDto(subordinateEntity);
    this.auditService.subordinateCreated(id, taIm.getOrganization().getOrganizationId(), null, dto);
    return dto;
  }

  @Override
  @Transactional
  public SubordinateDto updateSubordinate(final OrganizationRecord organizationRecord, final UUID id,
      final SubordinateDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final SubordinateEntity existing = this.findSubordinateOrThrow(organizationRecord, id);
    final SubordinateDto oldDto = toDto(existing);

    EntityToDto.updateEntity(existing, input);

    if (input.getPolicyId() != null) {
      final PolicyEntity policyEntity = this.policyRepository.findByOrgNumberAndPolicyId(
              organizationRecord.orgNumber(), input.getPolicyId())
          .orElseThrow(() -> new RegistryServerException(
              ErrorTypes.NOT_FOUND, "No policy found for id %s".formatted(input.getPolicyId())));
      existing.setPolicy(policyEntity);
    }
    else {
      existing.setPolicy(null);
    }
    if (input.isEcLocationAutomaticResolve()) {
      this.entityRepository
          .findByOrgNumberAndEntityKeyTypeAndIssuer(existing.getTaIm().getOrganization().getOrgNumber(),
              EntityKeyType.HOSTED_ENTITY, existing.getEntityidentifier())
          .orElseThrow(() ->
              new RegistryServerException(ErrorTypes.RELATION_NOT_FOUND,
                  "No hosted entity found for entityid: %s. Subordinatestatement can not be created"
                      .formatted(existing.getEntityidentifier()))
          );
    }
    this.subordinateRepository.save(existing);
    final SubordinateDto newDto = toDto(existing);
    this.auditService.subordinateUpdated(id, existing.getTaIm().getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional
  public void deleteSubordinate(final OrganizationRecord organizationRecord, final UUID id) {
    final SubordinateEntity entity = this.findSubordinateOrThrow(organizationRecord, id);
    final SubordinateDto dto = toDto(entity);
    this.subordinateRepository.delete(entity);
    this.auditService.subordinateDeleted(id, entity.getTaIm().getOrganization().getOrganizationId(), dto);
  }
}

