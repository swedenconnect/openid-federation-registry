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

import net.minidev.json.JSONObject;
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
import se.swedenconnect.oidf.registry.registrationflow.dto.TrustMarkFlowAssignmentDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.TrustMarkIssuerFlowAssignmentDto;
import se.swedenconnect.oidf.registry.registrationflow.model.ConfigValueModel;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.model.StepModel;
import se.swedenconnect.oidf.registry.registrationflow.model.TrustMarkFlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.model.TrustMarkIssuerFlowAssignment;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.module.repository.TrustmarkIssuerRepository;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessEngine;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.step.MissingContextValueException;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowRepository;
import se.swedenconnect.oidf.registry.registrationflow.repository.TrustMarkFlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrationflow.repository.TrustMarkIssuerFlowAssignmentRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;
import com.nimbusds.jose.jwk.JWKSet;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.DefaultConfig;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

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
  private final TrustMarkIssuerFlowAssignmentRepository tmIssuerFlowAssignmentRepository;
  private final TrustMarkFlowAssignmentRepository tmFlowAssignmentRepository;
  private final TrustmarkIssuerRepository trustmarkIssuerRepository;
  private final TrustMarkRepository trustMarkRepository;
  private final OrganizationService organizationService;
  private final ProcessEngine processEngine;
  private final RegistrationRepository registrationRepository;

  /**
   * Constructs a new RegistrationFlowService.
   *
   * @param registrationStepRepository repository of defined pipeline steps
   * @param taImRepository repository of trust anchor intermediates
   * @param flowRepository repository of registration flows
   * @param flowAssignmentRepository repository of flow assignments
   * @param tmIssuerFlowAssignmentRepository repository of trust mark issuer flow assignments
   * @param tmFlowAssignmentRepository repository of trust mark flow assignments
   * @param trustMarkRepository repository of trust marks
   * @param trustmarkIssuerRepository repository of trust mark issuers
   * @param organizationService service for resolving organizations
   * @param processEngine engine that handle the processing of a flow
   * @param registrationRepository repository for persisting step results
   */
  public RegistrationFlowService(final RegistrationStepRepository registrationStepRepository,
      final TaImRepository taImRepository, final FlowRepository flowRepository,
      final FlowAssignmentRepository flowAssignmentRepository,
      final TrustMarkIssuerFlowAssignmentRepository tmIssuerFlowAssignmentRepository,
      final TrustMarkFlowAssignmentRepository tmFlowAssignmentRepository,
      final TrustmarkIssuerRepository trustmarkIssuerRepository,
      final TrustMarkRepository trustMarkRepository,
      final OrganizationService organizationService, final ProcessEngine processEngine,
      final RegistrationRepository registrationRepository) {
    this.registrationStepRepository = registrationStepRepository;
    this.taImRepository = taImRepository;
    this.flowRepository = flowRepository;
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.tmIssuerFlowAssignmentRepository = tmIssuerFlowAssignmentRepository;
    this.tmFlowAssignmentRepository = tmFlowAssignmentRepository;
    this.trustmarkIssuerRepository = trustmarkIssuerRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.organizationService = organizationService;
    this.processEngine = processEngine;
    this.registrationRepository = registrationRepository;
  }

  private Organization resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(organizationRecord);

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
    final Organization org = this.resolveOrganization(organizationRecord);
    final RegistrationFlowDto dtoWithId = new RegistrationFlowDto(flowId, registrationFlowDto.name(),
        registrationFlowDto.description(), registrationFlowDto.descriptionSv(),
        registrationFlowDto.technology(), registrationFlowDto.entityType(),
        registrationFlowDto.steps(), registrationFlowDto.flowType());
    final RegistrationFlow registrationFlow = Mapper.toModel(dtoWithId, flowId, org,
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
    return new RegistrationFlowDto(existing.getFlowId(), existing.getName(), existing.getDescription(),
        existing.getDescriptionSv(), existing.getTechnology(), existing.getEntityType(), List.of(),
        existing.getFlowType());
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
    return new RegistrationFlowDto(flow.getFlowId(), flow.getName(), flow.getDescription(),
        flow.getDescriptionSv(), flow.getTechnology(), flow.getEntityType(), steps, flow.getFlowType());
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

    return new StepDto(defined.getStepId(), defined.getName(), defined.getDescription(), configs, defined.flowType());
  }

  /**
   * Returns a summary list of all registration flows owned by the calling organization.
   *
   * @param organizationRecord the calling organization
   * @return list of flow summaries (ID, name, description)
   */
  public List<FlowSummaryDto> listFlows(final OrganizationRecord organizationRecord) {
    return this.flowRepository.findByOrganizationOrgNumber(organizationRecord.orgNumber()).stream()
        .map(f -> new FlowSummaryDto(f.getFlowId(), f.getName(), f.getDescription(), f.getFlowType()))
        .toList();
  }

  /**
   * Returns all pipeline steps defined in the system.
   *
   * @return list of step DTOs
   */
  public List<StepDto> getDefineSteps() {
    return this.registrationStepRepository.getPublicDefinedSteps()
        .stream()
        .map(step -> new StepDto(step.getStepId(),
            step.getName(),
            step.getDescription(),
            step.getStepConfigurationValues()
                .stream()
                .map(ConfigValueDto::create)
                .toList(),
            step.flowType()))
        .toList();
  }

  /**
   * Trigger registration flow engine
   *
   * @param organizationRecord the organization initiating the registration
   * @param registrationRequestDto the registration request data
   * @return join ID string for the created registration flow
   */
  public ProcessReport executeRegistrationFlow(final OrganizationRecord organizationRecord,
      final RegistrationJoinRequestDto registrationRequestDto) {

    final UUID joinId = registrationRequestDto.getJoinId();
    final FlowAssignment flowAssignment = this.flowAssignmentRepository.findById(joinId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND, "Join flow not found"));

    final RegistrationFlow registrationFlow = flowAssignment.getRegistrationFlow();
    final ProcessFlow processFlow = Mapper.toProcessFlow(registrationFlow, this.registrationStepRepository);

    final ProcessContext processContext = new ProcessContext();
    processContext.put(ContextKey.ENTITY_ID, registrationRequestDto.getEntityIdentifier());
    final List<TrustmarkSource> trustmarkSources =
        RegistrationMapper.toTrustmarkSourceList(registrationRequestDto.getTrustmarksRequested());
    if (trustmarkSources != null) {
      processContext.put(ContextKey.TRUSTMARKS_REQUESTED, new SerializableList<>(trustmarkSources));
    }
    processContext.put(ContextKey.TAIM_ID, flowAssignment.getTaIm().getTaImId());
    processContext.put(ContextKey.JOIN_ID, flowAssignment.getAssignId());
    processContext.put(ContextKey.ORG, organizationRecord);
    final Map<String, Object> bodyMetadata = registrationRequestDto.getMetadata();
    if (bodyMetadata != null && !bodyMetadata.isEmpty()) {
      processContext.put(ContextKey.REQUEST_METADATA, new JSONObject(bodyMetadata));
    }

    try {
      final List<StepDefinition> allSteps = processFlow.getProcessFlow();
      final ProcessReport report = this.processEngine.run(allSteps, processContext);
      processContext.<UUID>get(ContextKey.REGISTRATION_ID).flatMap(this.registrationRepository::findById)
          .ifPresent(reg -> {
            reg.setStepResults(RegistrationMapper.toStepExecutionRecordDtos(report));
            if (report.isPendingApproval()) {
              reg.setPendingStepIndex(report.steps().size() - 1);
              reg.setStatus(RegistrationStatus.PENDING_APPROVAL);
              processContext.<net.minidev.json.JSONObject>get(ContextKey.REQUEST_METADATA)
                  .ifPresent(m -> reg.setRequestMetadata(new java.util.HashMap<>(m)));
            } else {
              reg.setPendingStepIndex(null);
            }
            this.registrationRepository.save(reg);
          });
      return report;
    }
    catch (final MissingContextValueException e) {
      throw new RegistryServerException(ErrorTypes.BLANK,"MissingContextValueException in process engine",e);
    }

  }


  /**
   * Approves a specific pending step, reconstructs the pipeline context from stored registration
   * data, and resumes execution from that step onwards.
   *
   * @param registrationId the registration ID
   * @param stepIndex the index of the pending step within the full step list
   * @return updated process report from the resumed run
   */
  @Transactional
  public ProcessReport approveStep(final UUID registrationId, final int stepIndex) {
    final Registration reg = this.registrationRepository.findById(registrationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: " + registrationId));

    if (reg.getStatus() != RegistrationStatus.PENDING_APPROVAL) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Registration is not pending approval: " + registrationId);
    }
    if (!Integer.valueOf(stepIndex).equals(reg.getPendingStepIndex())) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Step index %d does not match pending step %d".formatted(stepIndex, reg.getPendingStepIndex()));
    }

    if (reg.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE) {
      return this.approveTrustMarkSubordinateStep(reg, stepIndex);
    }

    final RegistrationFlow flow = reg.getFlowAssignment().getRegistrationFlow();
    final List<StepDefinition> allSteps = Mapper.toProcessFlow(flow, this.registrationStepRepository).getProcessFlow();

    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.ENTITY_ID, reg.getEntityId());
    ctx.put(ContextKey.REGISTRATION_ID, reg.getRegistrationId());
    ctx.put(ContextKey.JOIN_ID, reg.getFlowAssignment().getAssignId());
    ctx.put(ContextKey.TAIM_ID, reg.getFlowAssignment().getTaIm().getTaImId());
    final se.swedenconnect.oidf.registry.organization.model.Organization org = reg.getOrganization();
    ctx.put(ContextKey.ORG, new OrganizationRecord(org.getOrgNumber(), org.getOrgName(), null, null));
    if (reg.getTrustmarksRequested() != null) {
      ctx.put(ContextKey.TRUSTMARKS_REQUESTED, new SerializableList<>(reg.getTrustmarksRequested()));
    }
    if (reg.getJwks() != null && !reg.getJwks().isEmpty()) {
      try {
        ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS,
            JWKSet.parse(new net.minidev.json.JSONObject(reg.getJwks()).toJSONString()));
      } catch (final java.text.ParseException e) {
        throw new RegistryServerException(ErrorTypes.BLANK, "Failed to parse stored JWKS", e);
      }
    }
    if (reg.getMetadataPolicy() != null) {
      ctx.put(ContextKey.METADATA_POLICY, new net.minidev.json.JSONObject(reg.getMetadataPolicy()));
    }
    if (reg.getRequestMetadata() != null && !reg.getRequestMetadata().isEmpty()) {
      ctx.put(ContextKey.REQUEST_METADATA, new net.minidev.json.JSONObject(reg.getRequestMetadata()));
    }
    ctx.put(ContextKey.STEP_APPROVED, Boolean.TRUE);

    final List<StepDefinition> remaining = allSteps.subList(stepIndex, allSteps.size());
    final ProcessReport resumeReport = this.processEngine.run(remaining, ctx);

    final List<StepExecutionRecordDto> merged = new java.util.ArrayList<>(
        Optional.ofNullable(reg.getStepResults()).orElse(List.of()).subList(0, stepIndex));
    merged.addAll(RegistrationMapper.toStepExecutionRecordDtos(resumeReport));

    reg.setStepResults(merged);
    if (resumeReport.isPendingApproval()) {
      reg.setPendingStepIndex(stepIndex + resumeReport.steps().size() - 1);
      reg.setStatus(RegistrationStatus.PENDING_APPROVAL);
    } else {
      reg.setPendingStepIndex(null);
    }
    this.registrationRepository.save(reg);
    return resumeReport;
  }

  private ProcessReport approveTrustMarkSubordinateStep(final Registration reg, final int stepIndex) {
    final String trustmarkType = reg.getEntityId();
    final TrustMark trustMark = this.trustMarkRepository.findAllByTrustmarkType(trustmarkType)
        .stream().findFirst()
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Trust mark not found for type: " + trustmarkType));

    final TrustMarkFlowAssignment tmAssignment = this.tmFlowAssignmentRepository
        .findByTrustMarkTrustmarkId(trustMark.getTrustmarkId())
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "No flow assignment for trust mark: " + trustmarkType));

    final DefaultConfig emptyConfig = new DefaultConfig(Map.of());
    final List<StepDefinition> midSteps = Mapper.toMidOnlyProcessFlow(
        tmAssignment.getRegistrationFlow(), this.registrationStepRepository).getProcessFlow();

    final List<StepDefinition> allSubSteps = new java.util.ArrayList<>();
    this.registrationStepRepository.preTrustMarkSteps().stream()
        .map(s -> new StepDefinition(s, emptyConfig)).forEach(allSubSteps::add);
    allSubSteps.addAll(midSteps);
    this.registrationStepRepository.postTrustMarkSteps().stream()
        .map(s -> new StepDefinition(s, emptyConfig)).forEach(allSubSteps::add);

    final String resolvedIssuerId = trustMark.getTrustmarkIssuer().getEntity().getSubject();
    final TrustmarkSource tmSource = new TrustmarkSource(resolvedIssuerId,
        List.of(new TrustmarkSource.TrustMarkStatus(trustmarkType, RegistrationStatus.STARTED)));

    final ProcessContext ctx = new ProcessContext();
    final String entityId = reg.getParentRegistration() != null
        ? reg.getParentRegistration().getEntityId() : reg.getEntityId();
    ctx.put(ContextKey.ENTITY_ID, entityId);
    ctx.put(ContextKey.REGISTRATION_ID, reg.getRegistrationId());
    ctx.put(ContextKey.JOIN_ID, reg.getFlowAssignment().getAssignId());
    ctx.put(ContextKey.TAIM_ID, reg.getFlowAssignment().getTaIm().getTaImId());
    final Organization org = reg.getOrganization();
    ctx.put(ContextKey.ORG, new OrganizationRecord(org.getOrgNumber(), org.getOrgName(), null, null));
    ctx.put(ContextKey.TRUSTMARKS_REQUESTED, new SerializableList<>(List.of(tmSource)));
    ctx.put(ContextKey.STEP_APPROVED, Boolean.TRUE);

    final List<StepDefinition> remaining = allSubSteps.subList(stepIndex, allSubSteps.size());
    final ProcessReport resumeReport = this.processEngine.run(remaining, ctx);

    final List<StepExecutionRecordDto> merged = new java.util.ArrayList<>(
        Optional.ofNullable(reg.getStepResults()).orElse(List.of()).subList(0, stepIndex));
    merged.addAll(RegistrationMapper.toStepExecutionRecordDtos(resumeReport));
    reg.setStepResults(merged);

    if (resumeReport.isPendingApproval()) {
      reg.setPendingStepIndex(stepIndex + resumeReport.steps().size() - 1);
      reg.setStatus(RegistrationStatus.PENDING_APPROVAL);
    } else {
      reg.setPendingStepIndex(null);
    }
    this.registrationRepository.save(reg);
    return resumeReport;
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
          return new RegistrationFlowDto(f.getFlowId(), f.getName(), f.getDescription(),
              f.getDescriptionSv(), f.getTechnology(), f.getEntityType(), List.of(), f.getFlowType());
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

  /**
   * Returns all flow assignments for the given trust mark issuer, including the assign ID required
   * for unassign calls.
   *
   * @param tmIssuerId the trust mark issuer ID
   * @return list of assignment summaries
   */
  @Transactional(readOnly = true)
  public List<TrustMarkIssuerFlowAssignmentDto> getFlowAssignmentsForTrustMarkIssuer(final UUID tmIssuerId) {
    return this.tmIssuerFlowAssignmentRepository.findByTrustMarkIssuerTrustmarkIssuerId(tmIssuerId).stream()
        .map(a -> {
          final RegistrationFlow f = a.getRegistrationFlow();
          return new TrustMarkIssuerFlowAssignmentDto(a.getAssignId(), f.getFlowId(), f.getName(), f.getDescription());
        })
        .toList();
  }

  /**
   * Assigns a flow to a trust mark issuer. Idempotent: returns the existing assignment ID if the
   * flow is already assigned.
   *
   * @param tmIssuerId the trust mark issuer ID
   * @param flowId the flow ID to assign
   * @return the assignment response containing the assign ID
   */
  @Transactional
  public AssignFlowResponse assignFlowToTrustMarkIssuer(final UUID tmIssuerId, final UUID flowId) {
    return this.tmIssuerFlowAssignmentRepository
        .findByTrustMarkIssuerTrustmarkIssuerIdAndRegistrationFlowFlowId(tmIssuerId, flowId)
        .map(existing -> new AssignFlowResponse(existing.getAssignId()))
        .orElseGet(() -> {
          final TrustMarkIssuer issuer = this.trustmarkIssuerRepository.findById(tmIssuerId)
              .orElseThrow(() -> new RegistryServerException(
                  ErrorTypes.NOT_FOUND, "Trust mark issuer not found: " + tmIssuerId));
          final RegistrationFlow flow = this.flowRepository.findById(flowId)
              .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND, "Flow not found: " + flowId));
          final TrustMarkIssuerFlowAssignment assignment =
              new TrustMarkIssuerFlowAssignment(UUID.randomUUID(), issuer, flow);
          this.tmIssuerFlowAssignmentRepository.save(assignment);
          return new AssignFlowResponse(assignment.getAssignId());
        });
  }

  /**
   * Removes a flow assignment from a trust mark issuer. Throws 404 if the assignment is not found.
   *
   * @param tmIssuerId the trust mark issuer ID
   * @param assignId the assignment ID to remove
   */
  @Transactional
  public void unassignFlowFromTrustMarkIssuer(final UUID tmIssuerId, final UUID assignId) {
    final TrustMarkIssuerFlowAssignment assignment = this.tmIssuerFlowAssignmentRepository
        .findByAssignIdAndTrustMarkIssuerTrustmarkIssuerId(assignId, tmIssuerId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Assignment not found: " + assignId + " for trust mark issuer: " + tmIssuerId));
    this.tmIssuerFlowAssignmentRepository.delete(assignment);
  }

  /**
   * Returns all flow assignments for trust marks belonging to the given issuer.
   *
   * @param tmIssuerId the trust mark issuer module ID
   * @return list of assignment DTOs
   */
  @Transactional(readOnly = true)
  public List<TrustMarkFlowAssignmentDto> getFlowAssignmentsForTrustMarkIssuerTrustmarks(final UUID tmIssuerId) {
    return this.tmFlowAssignmentRepository
        .findByTrustMarkTrustmarkIssuerTrustmarkIssuerId(tmIssuerId).stream()
        .map(a -> new TrustMarkFlowAssignmentDto(
            a.getAssignId(),
            a.getTrustMark().getTrustmarkId(),
            a.getTrustMark().getTrustmarkType(),
            a.getRegistrationFlow().getFlowId(),
            a.getRegistrationFlow().getName(),
            a.getRegistrationFlow().getDescription()))
        .toList();
  }

  /**
   * Assigns a flow to a specific trust mark. Idempotent.
   *
   * @param trustmarkId the trust mark ID
   * @param flowId the flow to assign
   * @return assign response with the assignment ID
   */
  @Transactional
  public AssignFlowResponse assignFlowToTrustMark(final UUID trustmarkId, final UUID flowId) {
    return this.tmFlowAssignmentRepository
        .findByTrustMarkTrustmarkIdAndRegistrationFlowFlowId(trustmarkId, flowId)
        .map(existing -> new AssignFlowResponse(existing.getAssignId()))
        .orElseGet(() -> {
          final TrustMark tm = this.trustMarkRepository.findById(trustmarkId)
              .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
                  "Trust mark not found: " + trustmarkId));
          final RegistrationFlow flow = this.flowRepository.findById(flowId)
              .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
                  "Flow not found: " + flowId));
          final TrustMarkFlowAssignment assignment =
              new TrustMarkFlowAssignment(UUID.randomUUID(), tm, flow);
          this.tmFlowAssignmentRepository.save(assignment);
          return new AssignFlowResponse(assignment.getAssignId());
        });
  }

  /**
   * Removes a flow assignment from a trust mark.
   *
   * @param trustmarkId the trust mark ID
   * @param assignId the assignment ID
   */
  @Transactional
  public void unassignFlowFromTrustMark(final UUID trustmarkId, final UUID assignId) {
    final TrustMarkFlowAssignment assignment = this.tmFlowAssignmentRepository.findById(assignId)
        .filter(a -> a.getTrustMark().getTrustmarkId().equals(trustmarkId))
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Assignment not found: " + assignId + " for trust mark: " + trustmarkId));
    this.tmFlowAssignmentRepository.delete(assignment);
  }

}