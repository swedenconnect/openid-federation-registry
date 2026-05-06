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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessEngine;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.dto.FedRegStatus;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestStatusDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.service.SubordinateService;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
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
  private final ProcessEngine processEngine;
  private final SubordinateService subordinateService;
  private final JsonMapper objectMapper;

  /**
   * Constructs a new RegistrationServiceImpl.
   *
   * @param flowAssignmentRepository repository for flow assignments
   * @param registrationRepository repository for registration records
   * @param registrationFlowService service for managing registration flows
   * @param processEngine engine that executes the pipeline
   * @param subordinateService service for subordinate statement management
   * @param objectMapper JSON mapper
   */
  public RegistrationServiceImpl(final FlowAssignmentRepository flowAssignmentRepository,
      final RegistrationRepository registrationRepository,
      final RegistrationFlowService registrationFlowService,
      final ProcessEngine processEngine,
      final SubordinateService subordinateService,
      final JsonMapper objectMapper) {
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.registrationRepository = registrationRepository;
    this.registrationFlowService = registrationFlowService;
    this.processEngine = processEngine;
    this.subordinateService = subordinateService;
    this.objectMapper = objectMapper;
  }



  @Override
  @Transactional
  public RegistrationRequestStatusDto createRegistrationRequestWithId(final OrganizationRecord organizationRecord,
      final UUID joinId, final RegistrationRequestDto request) {

    this.processEngine.register(request);
    return  new RegistrationRequestStatusDto();
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
  public List<RegistrationDto> listRegistrations(final OrganizationRecord organizationRecord) {
    //TODO this is not the right way to handle organizations
    return this.registrationRepository.findAll().stream()
        .filter(reg -> reg.getFlowAssignment().getTaIm().getOrganization().getOrgNumber()
            .equals(organizationRecord.orgNumber()))
        .map(RegistrationServiceImpl::toJoinDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationFlowDto> listRegistrationFlows(final OrganizationRecord organizationRecord) {
    return this.flowAssignmentRepository.findAll()
        .stream()
        .map(flowAssignment -> {
          final RegistrationFlow registrationFlow = flowAssignment.getRegistrationFlow();
          final TrustAnchorIntermediateModule intermediate = flowAssignment.getTaIm();

          final RegistrationFlowDto dto = new RegistrationFlowDto();
          dto.setJoinId(flowAssignment.getAssignId());
          dto.setName(registrationFlow.getName());
          dto.setDescription(registrationFlow.getDescription());
          dto.setIntermidiateEntityId(intermediate.getEntity().getSubject());
          return dto;
        })
        .toList();
  }


  private static RegistrationDto toJoinDto(final Registration reg) {
    final RegistrationDto dto = new RegistrationDto();
    dto.setJoinId(reg.getFlowAssignment().getAssignId());
    dto.setEntityId(reg.getEntityId());
    dto.setIntermediateEntityId(reg.getFlowAssignment().getTaIm().getEntity().getSubject());
    dto.setRegistrationId(reg.getRegistrationId());
    dto.setStatusFedreg(switch (reg.getStatus()) {
      case APPROVED -> FedRegStatus.APPROVED;
      case PENDING -> FedRegStatus.PENDING;
      case REJECTED -> FedRegStatus.REJECTED;
    });
    if (reg.getRejectionReason() != null) {
      dto.setRejectionReason(reg.getRejectionReason().toString());
    }
    return dto;
  }
}