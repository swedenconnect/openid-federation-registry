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

package se.swedenconnect.oidf.registry.registrationflow;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.registrationflow.dto.AssignFlowResponse;
import se.swedenconnect.oidf.registry.registrationflow.dto.ConfigValueDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.FlowSummaryDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.IntermediateFlowAssignmentDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.Mapper;
import se.swedenconnect.oidf.registry.registrationflow.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.StepDto;
import se.swedenconnect.oidf.registry.registrationflow.model.ConfigValueModel;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.model.StepModel;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowRepository;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestDto;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Operations for registration flows.
 *
 * @author Per Fredrik Plars
 */
@Service
public class RegistrationFlowService {

  private final RegistrationStepRepository registrationStepRepository;
  private final TaImRepository taImRepository;
  private final FlowRepository flowRepository;
  private final FlowAssignmentRepository flowAssignmentRepository;
  private final OrganizationService organizationService;
  private final JsonMapper objectMapper;

  /**
   * Constructs a new RegistrationFlowService.
   *
   * @param registrationStepRepository repository of defined pipeline steps
   * @param taImRepository repository of trust anchor intermediates
   * @param flowRepository repository of registration flows
   * @param flowAssignmentRepository repository of flow assignments
   * @param organizationService service for resolving organizations
   * @param objectMapper JSON mapper for flow definition serialization
   */
  public RegistrationFlowService(final RegistrationStepRepository registrationStepRepository,
      final TaImRepository taImRepository, final FlowRepository flowRepository,
      final FlowAssignmentRepository flowAssignmentRepository,
      final OrganizationService organizationService, final JsonMapper objectMapper) {
    this.registrationStepRepository = registrationStepRepository;
    this.taImRepository = taImRepository;
    this.flowRepository = flowRepository;
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.organizationService = organizationService;
    this.objectMapper = objectMapper;
  }

  private Organization resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(
        organizationRecord.orgNumber(), organizationRecord.orgName());
  }

  private RegistrationFlow findOwnedFlowOrThrow(final OrganizationRecord organizationRecord, final UUID flowId) {
    return this.flowRepository
        .findByOrganizationOrgNumberAndFlowId(organizationRecord.orgNumber(), flowId)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "Flow not found: " + flowId));
  }

  /**
   * Creates a new registration flow owned by the given organization.
   *
   * @param organizationRecord the calling organization
   * @param registrationFlowDto the flow definition
   * @param flowId the ID to assign when the DTO does not supply one
   * @return the created flow DTO
   */
  public RegistrationFlowDto createRegistrationFlow(final OrganizationRecord organizationRecord,
      final RegistrationFlowDto registrationFlowDto, final UUID flowId) {
    ValidateDto.init(organizationRecord).validate(registrationFlowDto);
    final UUID effectiveId = registrationFlowDto.flowId() != null ? registrationFlowDto.flowId() : flowId;
    final Organization org = this.resolveOrganization(organizationRecord);
    final RegistrationFlowDto dtoWithId = new RegistrationFlowDto(effectiveId, registrationFlowDto.name(),
        registrationFlowDto.description(), registrationFlowDto.steps());
    final RegistrationFlow registrationFlow = Mapper.toModel(dtoWithId, effectiveId, org,
        this.registrationStepRepository);
    this.flowRepository.save(registrationFlow);
    return dtoWithId;
  }

  /**
   * Updates an existing registration flow. The flow must belong to the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param flowId the flow ID to update
   * @param registrationFlowDto the updated flow definition
   * @return the updated flow DTO
   */
  @Transactional
  public RegistrationFlowDto updateRegistrationFlow(final OrganizationRecord organizationRecord,
      final UUID flowId, final RegistrationFlowDto registrationFlowDto) {
    ValidateDto.init(organizationRecord).validate(registrationFlowDto);
    final RegistrationFlow existing = this.findOwnedFlowOrThrow(organizationRecord, flowId);
    Mapper.applyUpdate(existing, registrationFlowDto, this.registrationStepRepository);
    this.flowRepository.save(existing);
    return new RegistrationFlowDto(existing.getFlowId(), existing.getName(), existing.getDescription(), List.of());
  }

  /**
   * Deletes the registration flow with the given ID. The flow must belong to the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param registrationFlowId the ID of the flow to delete
   */
  public void deleteRegistrationFlow(final OrganizationRecord organizationRecord, final UUID registrationFlowId) {
    this.findOwnedFlowOrThrow(organizationRecord, registrationFlowId);
    this.flowRepository.deleteById(registrationFlowId);
  }

  /**
   * Returns the registration flow with the given ID. The flow must belong to the calling organization.
   *
   * @param organizationRecord the calling organization
   * @param registrationFlowId the flow ID
   * @return the flow DTO
   */
  public RegistrationFlowDto getRegistrationFlow(final OrganizationRecord organizationRecord,
      final UUID registrationFlowId) {
    final RegistrationFlow flow = this.findOwnedFlowOrThrow(organizationRecord, registrationFlowId);
    final List<StepDto> steps = Optional.ofNullable(flow.getFlowDefinition()).orElse(List.of())
        .stream()
        .map(this::resolveStep)
        .toList();
    return new RegistrationFlowDto(flow.getFlowId(), flow.getName(), flow.getDescription(), steps);
  }

  private StepDto resolveStep(final StepModel storedStep) {
    final Step defined = this.registrationStepRepository.findStepById(storedStep.stepId())
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "Step not defined in service: " + storedStep.stepId()));

    final Map<String, String> storedValues = storedStep.config().stream()
        .collect(Collectors.toMap(ConfigValueModel::key, ConfigValueModel::value));

    final List<ConfigValueDto> configs = defined.getStepConfigurationValues().stream()
        .map(scv -> new ConfigValueDto(
            scv.name(),
            scv.description(),
            storedValues.containsKey(scv.name())
                ? storedValues.get(scv.name())
                : (scv.defaultValue() != null ? scv.defaultValue().toString() : null),
            scv.dataType().toString(),
            scv.defaultValue()))
        .toList();

    return new StepDto(defined.getStepId(), defined.getName(), defined.getDescription(), configs);
  }

  /**
   * Returns a summary list of all registration flows owned by the calling organization.
   *
   * @param organizationRecord the calling organization
   * @return list of flow summaries (ID, name, description)
   */
  public List<FlowSummaryDto> listFlows(final OrganizationRecord organizationRecord) {
    return this.flowRepository.findByOrganizationOrgNumber(organizationRecord.orgNumber()).stream()
        .map(f -> new FlowSummaryDto(f.getFlowId(), f.getName(), f.getDescription()))
        .toList();
  }

  /**
   * Returns all pipeline steps defined in the system.
   *
   * @return list of step DTOs
   */
  public List<StepDto> getDefineSteps() {
    return this.registrationStepRepository.getDefinedSteps()
        .stream()
        .map(step -> new StepDto(step.getStepId(),
            step.getName(),
            step.getDescription(),
            step.getStepConfigurationValues()
                .stream()
                .map(ConfigValueDto::create)
                .toList()))
        .toList();
  }

  /**
   * Trigger registration flow engine
   *
   * @param organizationRecord
   * @param registrationRequestDto
   * @return
   */
  public String executeRegistrationFlow(final OrganizationRecord organizationRecord,
      final RegistrationRequestDto registrationRequestDto) {

    final UUID joinId = registrationRequestDto.getJoinId();
    this.flowAssignmentRepository.findById(joinId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND, "Join flow not found"));
    return null;
  }

  /**
   * Builds a {@link ProcessFlow} from the stored flow definition for the given ID.
   *
   * @param flowId the flow ID
   * @return the constructed process flow
   */
  public ProcessFlow buildProcessFlow(final UUID flowId) {
    final RegistrationFlow flow = this.flowRepository.findById(flowId)
        .orElseThrow(() -> new EntityNotFoundException("Flow not found: " + flowId));
    final RegistrationFlowDto dto = this.objectMapper.convertValue(
        flow.getFlowDefinition(), RegistrationFlowDto.class);
    return Mapper.toProcessFlow(dto, this.registrationStepRepository);
  }

  /**
   * Returns all flows assigned to the given intermediate.
   *
   * @param taImId the intermediate ID
   * @return list of flow DTOs
   */
  @Transactional(readOnly = true)
  public List<RegistrationFlowDto> getFlowsForIntermediate(final UUID taImId) {
    return this.flowAssignmentRepository.findByTaImTaImId(taImId).stream()
        .map(a -> {
          final RegistrationFlow f = a.getRegistrationFlow();
          return new RegistrationFlowDto(f.getFlowId(), f.getName(), f.getDescription(), List.of());
        })
        .toList();
  }

  /**
   * Returns all flow assignments for the given intermediate, including the assign ID required
   * for unassign calls.
   *
   * @param taImId the intermediate ID
   * @return list of assignment summaries
   */
  @Transactional(readOnly = true)
  public List<IntermediateFlowAssignmentDto> getFlowAssignmentsForIntermediate(final UUID taImId) {
    return this.flowAssignmentRepository.findByTaImTaImId(taImId).stream()
        .map(a -> {
          final RegistrationFlow f = a.getRegistrationFlow();
          return new IntermediateFlowAssignmentDto(a.getAssignId(), f.getFlowId(), f.getName(), f.getDescription());
        })
        .toList();
  }

  /**
   * Assigns a flow to an intermediate. Idempotent: returns the existing assignment ID if the
   * flow is already assigned.
   *
   * @param taImId the intermediate ID
   * @param flowId the flow ID to assign
   * @return the assignment response containing the assign ID
   */
  @Transactional
  public AssignFlowResponse assignFlow(final UUID taImId, final UUID flowId) {
    return this.flowAssignmentRepository
        .findByTaImTaImIdAndRegistrationFlowFlowId(taImId, flowId)
        .map(existing -> new AssignFlowResponse(existing.getAssignId()))
        .orElseGet(() -> {
          final TrustAnchorIntermediateModule taIm = this.taImRepository.findById(taImId)
              .orElseThrow(() -> new RegistryServerException(
                  ErrorTypes.NOT_FOUND, "Intermediate not found: " + taImId));
          final RegistrationFlow flow = this.flowRepository.findById(flowId)
              .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND, "Flow not found: " + flowId));
          final FlowAssignment assignment = new FlowAssignment(UUID.randomUUID(), taIm, flow);
          this.flowAssignmentRepository.save(assignment);
          return new AssignFlowResponse(assignment.getAssignId());
        });
  }

  /**
   * Removes a flow assignment from an intermediate. Throws 404 if either the assign ID or the
   * intermediate ID does not match an existing assignment.
   *
   * @param taImId the intermediate ID
   * @param assignId the assignment ID to remove
   */
  @Transactional
  public void unassignFlow(final UUID taImId, final UUID assignId) {
    final FlowAssignment assignment = this.flowAssignmentRepository
        .findByAssignIdAndTaImTaImId(assignId, taImId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Assignment not found: " + assignId + " for intermediate: " + taImId));
    this.flowAssignmentRepository.delete(assignment);
  }

}