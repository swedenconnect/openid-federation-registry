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
package se.swedenconnect.oidf.registry.registrations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RegistrationAdminService}.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Service
public class RegistrationAdminServiceImpl implements RegistrationAdminService {

  private final RegistrationRepository registrationRepository;
  private final EntityConfigService entityConfigService;
  private final RegistrationFlowService registrationFlowService;
  private final OrganizationService organizationService;

  /**
   * Constructor.
   *
   * @param registrationRepository repository for registration records
   * @param entityConfigService service for checking hosted entities
   * @param registrationFlowService service for resuming pipeline execution on step approval
   * @param organizationService service for resolving the calling organization
   */
  public RegistrationAdminServiceImpl(final RegistrationRepository registrationRepository,
      final EntityConfigService entityConfigService,
      final RegistrationFlowService registrationFlowService,
      final OrganizationService organizationService) {
    this.registrationRepository = registrationRepository;
    this.entityConfigService = entityConfigService;
    this.registrationFlowService = registrationFlowService;
    this.organizationService = organizationService;
  }

  /**
   * Finds a registration by ID, verifying it is connected to an intermediate owned by the calling organization. Both
   * "no such registration" and "registration belongs to another organization" collapse to the same not-found error, so
   * a foreign registration ID is indistinguishable from a nonexistent one.
   *
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @return the owned registration
   */
  private Registration findOwnedRegistrationOrThrow(final OrganizationRecord organizationRecord,
      final UUID registrationId) {
    final UUID organizationId = this.organizationService.find(organizationRecord)
        .map(Organization::getOrganizationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
    return this.registrationRepository
        .findByRegistrationIdAndFlowAssignment_TaIm_Organization_OrganizationId(registrationId, organizationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
  }

  @Override
  public long countPending(final OrganizationRecord organizationRecord, final UUID taimId) {
    return this.organizationService.find(organizationRecord)
        .map(org -> this.registrationRepository
            .countByFlowAssignment_TaIm_TaImIdAndFlowAssignment_TaIm_Organization_OrganizationIdAndStatus(
                taimId, org.getOrganizationId(), RegistrationStatus.PENDING_APPROVAL))
        .orElse(0L);
  }

  @Override
  @Transactional
  public RegistrationDto reject(final OrganizationRecord organizationRecord, final UUID registrationId,
      final String rejectionReason) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    if (reg.getStatus() != RegistrationStatus.PENDING_APPROVAL) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Registration %s is not pending approval".formatted(registrationId));
    }
    reg.setStatus(RegistrationStatus.REJECTED);
    reg.setRejectionReason(rejectionReason);
    reg.setReviewedAt(LocalDateTime.now());
    this.registrationRepository.save(reg);
    final List<HostedEntityDto> hostedEntities = this.entityConfigService.listHostedEntity(reg.getEntityId());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    return RegistrationMapper.toRegistrationDto(reg, isHosted, hostedMetadata);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationDto> listRegistrationsConnectedToThisOrgIM(final OrganizationRecord organizationRecord) {
    final Map<String, Map<String, Object>> hostedMetadataByEntityId = new HashMap<>();
    this.entityConfigService.listHostedEntity((String) null)
        .forEach(h -> hostedMetadataByEntityId.put(h.getEntityIdentifier(), h.getMetadata()));
    final List<Registration> allRegs = this.organizationService.find(organizationRecord)
        .map(org -> this.registrationRepository.findByFlowAssignment_TaIm_Organization_OrganizationId(
            org.getOrganizationId()))
        .orElse(List.of());
    final Map<UUID, Map<String, RegistrationStatus>> tmStatusByParent = allRegs.stream()
        .filter(r -> r.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE)
        .filter(r -> r.getParentRegistration() != null)
        .collect(Collectors.groupingBy(
            r -> r.getParentRegistration().getRegistrationId(),
            Collectors.toMap(Registration::getEntityId, Registration::getStatus)));
    return allRegs.stream()
        .map(r -> RegistrationMapper.toRegistrationDto(r,
            hostedMetadataByEntityId.containsKey(r.getEntityId()),
            hostedMetadataByEntityId.get(r.getEntityId()),
            tmStatusByParent.getOrDefault(r.getRegistrationId(), Map.of())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RegistrationDto getRegistrationById(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    final List<HostedEntityDto> hostedEntities = this.entityConfigService.listHostedEntity(reg.getEntityId());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    final Map<String, RegistrationStatus> tmStatusByType =
        this.registrationRepository.findByParentRegistration_RegistrationId(registrationId)
            .stream()
            .collect(Collectors.toMap(Registration::getEntityId, Registration::getStatus));
    return RegistrationMapper.toRegistrationDto(reg, isHosted, hostedMetadata, tmStatusByType);
  }

  @Override
  @Transactional
  public RegistrationDto approveStep(final OrganizationRecord organizationRecord, final UUID registrationId,
      final int stepIndex) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    this.registrationFlowService.approveStep(reg, stepIndex);
    return this.getRegistrationById(organizationRecord, registrationId);
  }
}
