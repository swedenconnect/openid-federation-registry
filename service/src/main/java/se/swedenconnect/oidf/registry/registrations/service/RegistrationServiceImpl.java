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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationFlowInformationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestStatusDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link RegistrationService}.
 *
 * @author Per Fredrik Plars
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

  private final FlowAssignmentRepository flowAssignmentRepository;
  private final RegistrationRepository registrationRepository;
  private final RegistrationFlowService registrationFlowService;


  /**
   * Constructs a new RegistrationServiceImpl.
   *
   * @param flowAssignmentRepository repository for flow assignments
   * @param registrationRepository repository for registration records
   * @param registrationFlowService service for managing registration flows
   */
  public RegistrationServiceImpl(final FlowAssignmentRepository flowAssignmentRepository,
      final RegistrationRepository registrationRepository,
      final RegistrationFlowService registrationFlowService) {
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.registrationRepository = registrationRepository;
    this.registrationFlowService = registrationFlowService;
  }


  @Override
  @Transactional(readOnly = true)
  public RegistrationDto getRegistrationById(final OrganizationRecord organizationRecord, final UUID registrationId) {
    // todo limit by its orgbelonging
    return this.registrationRepository.findById(registrationId)
        .map(RegistrationMapper::toRegistrationDto)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
  }


  @Override
  @Transactional
  public RegistrationRequestStatusDto createRegistrationRequest(final OrganizationRecord organizationRecord,
      final UUID joinId, final RegistrationJoinRequestDto request) {

    request.setJoinId(joinId);
    ValidateDto.init(organizationRecord).validate(request);
    final ProcessReport report = this.registrationFlowService.executeRegistrationFlow(organizationRecord, request);
    final UUID registrationId = this.registrationRepository.findByEntityId(request.getEntityIdentifier())
        .map(Registration::getRegistrationId)
        .orElse(null);
    return RegistrationMapper.toRegistrationRequestStatusDto(request.getEntityIdentifier(), registrationId, report);
  }

  @Override
  @Transactional
  public void deleteRegistrationRequest(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final Registration reg = this.registrationRepository.findById(registrationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
    if (reg.getStatus() == RegistrationStatus.APPROVED) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Cannot delete an approved registration — remove the subordinate statement first.");
    }
    this.registrationRepository.delete(reg);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationDto> listRegistrationsForThisOrg(final OrganizationRecord organizationRecord) {
    return this.registrationRepository.findAllByOrganizationOrgNumber(organizationRecord.orgNumber())
        .stream()
        .map(RegistrationMapper::toRegistrationDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationFlowInformationDto> listRegistrationFlows(final OrganizationRecord organizationRecord) {
    return this.flowAssignmentRepository.findAll()
        .stream()
        .map(RegistrationMapper::toRegistrationFlowDto)
        .toList();
  }


}