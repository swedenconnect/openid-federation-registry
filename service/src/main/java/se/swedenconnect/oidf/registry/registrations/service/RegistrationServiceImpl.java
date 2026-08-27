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
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationFlowInformationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.subordinate.service.SubordinateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RegistrationService}.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

  private final FlowAssignmentRepository flowAssignmentRepository;
  private final RegistrationRepository registrationRepository;
  private final RegistrationFlowService registrationFlowService;
  private final SubordinateRepository subordinateRepository;
  private final SubordinateService subordinateService;
  private final EntityConfigService entityConfigService;
  private final OrganizationService organizationService;


  /**
   * Constructs a new RegistrationServiceImpl.
   *
   * @param flowAssignmentRepository repository for flow assignments
   * @param registrationRepository repository for registration records
   * @param registrationFlowService service for managing registration flows
   * @param subordinateRepository repository for subordinate statements
   * @param subordinateService service for deleting subordinate statements
   * @param entityConfigService service for deleting hosted entities
   * @param organizationService service for resolving the calling organization
   */
  public RegistrationServiceImpl(final FlowAssignmentRepository flowAssignmentRepository,
      final RegistrationRepository registrationRepository,
      final RegistrationFlowService registrationFlowService,
      final SubordinateRepository subordinateRepository,
      final SubordinateService subordinateService,
      final EntityConfigService entityConfigService,
      final OrganizationService organizationService) {
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.registrationRepository = registrationRepository;
    this.registrationFlowService = registrationFlowService;
    this.subordinateRepository = subordinateRepository;
    this.subordinateService = subordinateService;
    this.entityConfigService = entityConfigService;
    this.organizationService = organizationService;
  }

  /**
   * Finds a registration by ID, verifying the calling organization is the registrant. Both "no such registration" and
   * "registration belongs to another organization" collapse to the same not-found error.
   *
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @return the owned registration
   */
  private Registration findOwnedRegistrationOrThrow(final OrganizationRecord organizationRecord,
      final UUID registrationId) {
    final RegistryServerException notFound = new RegistryServerException(ErrorTypes.NOT_FOUND,
        "Registration not found: %s".formatted(registrationId));
    final UUID organizationId = this.organizationService.find(organizationRecord)
        .map(Organization::getOrganizationId)
        .orElseThrow(() -> notFound);
    return this.registrationRepository
        .findByRegistrationIdAndOrganization_OrganizationId(registrationId, organizationId)
        .orElseThrow(() -> notFound);
  }


  @Override
  @Transactional(readOnly = true)
  public RegistrationDto getRegistrationById(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    final List<HostedEntityDto> hostedEntities =
        this.entityConfigService.listHostedEntity(organizationRecord, reg.getEntityId());
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
  public RegistrationDto createRegistrationRequest(final OrganizationRecord organizationRecord,
      final UUID joinId, final RegistrationJoinRequestDto request) {

    request.setJoinId(joinId);
    ValidateDto.init(organizationRecord).validate(request);
    final ProcessReport report = this.registrationFlowService.executeRegistrationFlow(organizationRecord, request);
    final Registration registration = this.registrationRepository.findByEntityId(request.getEntityIdentifier())
        .orElseThrow(() -> new IllegalArgumentException("No registration found for this registrationid"));
    final List<HostedEntityDto> hostedEntities =
        this.entityConfigService.listHostedEntity(organizationRecord, request.getEntityIdentifier());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    return RegistrationMapper.toRegistrationRequestStatusDto(registration, report, isHosted, hostedMetadata);
  }

  @Override
  @Transactional
  public void deleteRegistrationRequest(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    if (reg.getStatus() == RegistrationStatus.APPROVED) {
      this.subordinateRepository
          .findByOrganizationIdAndEntityidentifier(reg.getOrganization().getOrganizationId(), reg.getEntityId())
          .forEach(sub -> this.subordinateService.deleteSubordinate(organizationRecord, sub.getSubordinateId()));
    }
    this.entityConfigService.listHostedEntity(organizationRecord, reg.getEntityId())
        .forEach(hosted -> this.entityConfigService.deleteHostedEntity(organizationRecord, hosted.getEntityId()));
    this.registrationRepository.delete(reg);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationDto> listRegistrationsForThisOrg(final OrganizationRecord organizationRecord) {
    final Map<String, Map<String, Object>> hostedMetadataByEntityId = new HashMap<>();
    this.entityConfigService.listHostedEntity(organizationRecord, null)
        .forEach(h -> hostedMetadataByEntityId.put(h.getEntityIdentifier(), h.getMetadata()));
    final List<Registration> allRegs = this.organizationService.find(organizationRecord)
        .map(org -> this.registrationRepository.findByOrganization_OrganizationId(org.getOrganizationId()))
        .orElse(List.of());
    final Map<UUID, Map<String, RegistrationStatus>> tmStatusByParent = allRegs.stream()
        .filter(r -> r.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE)
        .filter(r -> r.getParentRegistration() != null)
        .collect(Collectors.groupingBy(
            r -> r.getParentRegistration().getRegistrationId(),
            Collectors.toMap(Registration::getEntityId, Registration::getStatus)));
    return allRegs.stream()
        .filter(r -> r.getRegistrationType() != RegistrationType.TRUST_MARK_SUBORDINATE)
        .map(r -> RegistrationMapper.toRegistrationDto(r,
            hostedMetadataByEntityId.containsKey(r.getEntityId()),
            hostedMetadataByEntityId.get(r.getEntityId()),
            tmStatusByParent.getOrDefault(r.getRegistrationId(), Map.of())))
        .toList();
  }

  @Override
  @Transactional
  public RegistrationDto updateRegistrationRequest(final OrganizationRecord organizationRecord,
      final UUID registrationId, final RegistrationJoinRequestDto request) {
    final Registration existing = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    request.setJoinId(existing.getFlowAssignment().getAssignId());
    ValidateDto.init(organizationRecord).validate(request);
    final ProcessReport report = this.registrationFlowService.executeRegistrationFlow(organizationRecord, request);
    final Registration registration = this.registrationRepository.findByEntityId(request.getEntityIdentifier())
        .orElseThrow(() -> new IllegalArgumentException("No registration found for this entity id"));
    final List<HostedEntityDto> hostedEntities =
        this.entityConfigService.listHostedEntity(organizationRecord, request.getEntityIdentifier());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    return RegistrationMapper.toRegistrationRequestStatusDto(registration, report, isHosted, hostedMetadata);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationFlowInformationDto> listRegistrationFlows() {
    return this.flowAssignmentRepository.findAll()
        .stream()
        .map(RegistrationMapper::toRegistrationFlowDto)
        .toList();
  }


}