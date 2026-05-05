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
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessEngine;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowRepository;
import se.swedenconnect.oidf.registry.registrations.dto.FedRegStatus;
import se.swedenconnect.oidf.registry.registrations.dto.FlowDto;
import se.swedenconnect.oidf.registry.registrations.dto.JoinDto;
import se.swedenconnect.oidf.registry.registrations.dto.JoinRequestDto;
import se.swedenconnect.oidf.registry.registrations.dto.TrustmarkRequestDto;
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

  /** Step ID for ManualValidationStep — used to detect manual flows. */
  private static final UUID MANUAL_VALIDATION_STEP_ID =
      UUID.fromString("B292AA20-0F6A-4362-830F-B22AC36B76ED");

  private final FlowRepository flowRepository;
  private final FlowAssignmentRepository flowAssignmentRepository;
  private final RegistrationRepository registrationRepository;
  private final RegistrationFlowService registrationFlowService;
  private final ProcessEngine processEngine;
  private final SubordinateService subordinateService;
  private final JsonMapper objectMapper;

  /**
   * Constructs a new RegistrationServiceImpl.
   *
   * @param flowRepository repository for registration flows
   * @param flowAssignmentRepository repository for flow assignments
   * @param registrationRepository repository for registration records
   * @param registrationFlowService service for managing registration flows
   * @param processEngine engine that executes the pipeline
   * @param subordinateService service for subordinate statement management
   * @param objectMapper JSON mapper
   */
  public RegistrationServiceImpl(final FlowRepository flowRepository,
      final FlowAssignmentRepository flowAssignmentRepository,
      final RegistrationRepository registrationRepository,
      final RegistrationFlowService registrationFlowService,
      final ProcessEngine processEngine,
      final SubordinateService subordinateService,
      final JsonMapper objectMapper) {
    this.flowRepository = flowRepository;
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.registrationRepository = registrationRepository;
    this.registrationFlowService = registrationFlowService;
    this.processEngine = processEngine;
    this.subordinateService = subordinateService;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public JoinDto createJoin(final JoinRequestDto request) {
    return this.createJoinWithId(UUID.randomUUID(), request);
  }

  @Override
  @Transactional
  public JoinDto createJoinWithId(final UUID joinId, final JoinRequestDto request) {
    final var assignment = this.flowAssignmentRepository.findById(request.getRegistrationAssignId())
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Assignment not found: %s".formatted(request.getRegistrationAssignId())));

    final RegistrationFlow flow = assignment.getRegistrationFlow();
    final TrustAnchorIntermediateModule taIm = assignment.getTaIm();

    final ProcessFlow processFlow = this.registrationFlowService.buildProcessFlow(flow.getFlowId());

    final boolean isManualFlow = processFlow.getProcessFlow().stream()
        .anyMatch(s -> MANUAL_VALIDATION_STEP_ID.equals(s.step().getStepId()));

    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.ENTITY_ID, request.getEntityId());
    ctx.put(ContextKey.TAIM_ID, taIm.getTaImId());
    ctx.put(ContextKey.REGISTRATION_FLOW_ID, flow.getFlowId());
    ctx.put(ContextKey.TRUSTMARKS_REQUESTED, this.serializeTrustmarks(request.getTrustmarks()));

    final ProcessReport report = this.processEngine.run(processFlow.getProcessFlow(), ctx);

    if (!report.isSuccessful()) {
      final String reason = report.steps().getLast().result().message();
      if (reason != null && reason.contains("already registered")) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
      }
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          "Registration flow failed: " + reason);
    }

    if (isManualFlow) {
      final Registration reg = this.registrationRepository
          .findByEntityIdAndStatus(request.getEntityId(), RegistrationStatus.PENDING)
          .orElseThrow(() -> new IllegalStateException("ManualValidationStep did not persist registration"));
      return toJoinDto(reg);
    }

    this.autoCreateSubordinate(ctx, taIm);
    final Registration reg = this.createApprovedRecord(joinId, request, taIm, flow, ctx);
    return toJoinDto(reg);
  }

  @Override
  @Transactional
  public void deleteJoin(final UUID joinId) {
    final Registration reg = this.registrationRepository.findById(joinId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(joinId)));
    if (reg.getStatus() == RegistrationStatus.APPROVED) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Cannot delete an approved registration — remove the subordinate statement first.");
    }
    this.registrationRepository.delete(reg);
  }

  @Override
  @Transactional(readOnly = true)
  public List<JoinDto> listJoins() {
    return this.registrationRepository.findAll().stream()
        .map(RegistrationServiceImpl::toJoinDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<FlowDto> listFlows() {
    return this.flowRepository.findAll().stream()
        .map(f -> {
          final FlowDto dto = new FlowDto();
          dto.setRegistrationId(f.getFlowId());
          dto.setName(f.getName());
          dto.setDescription(f.getDescription());
          return dto;
        })
        .toList();
  }

  private void autoCreateSubordinate(final ProcessContext ctx, final TrustAnchorIntermediateModule taIm) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final String jwks = ctx.<String>get(ContextKey.ENTITY_CONFIGURATION_JWKS).orElse(null);
    final String metadataPolicyJson = ctx.<String>get(ContextKey.METADATA_POLICY).orElse(null);

    final SubordinateDto sub = new SubordinateDto();
    sub.setEntityIdentifier(entityId);
    sub.setTaImId(taIm.getTaImId());
    sub.setJwks(this.deserializePolicy(jwks));
    sub.setMetadataPolicy(this.deserializePolicy(metadataPolicyJson));

    final OrganizationRecord orgRecord = new OrganizationRecord(
        taIm.getOrganization().getOrgNumber(),
        taIm.getOrganization().getOrgName(),
        "");
    this.subordinateService.createSubordinate(orgRecord, sub);
  }

  private Registration createApprovedRecord(final UUID id, final JoinRequestDto request,
      final TrustAnchorIntermediateModule taIm, final RegistrationFlow flow, final ProcessContext ctx) {
    final Registration reg = new Registration();
    reg.setRegistrationId(id);
    reg.setTaIm(taIm);
    reg.setRegistrationFlow(flow);
    reg.setEntityId(request.getEntityId());
    reg.setJwks(ctx.<String>get(ContextKey.ENTITY_CONFIGURATION_JWKS).orElse(null));
    //reg.setMetadata(ctx.<String>get(ContextKey.ENTITY_CONFIGURATION_METADATA).orElse(null));
    reg.setMetadataPolicy(ctx.<String>get(ContextKey.METADATA_POLICY).orElse(null));
    reg.setTrustmarksRequested(this.serializeTrustmarks(request.getTrustmarks()));
    reg.setStatus(RegistrationStatus.APPROVED);
    return this.registrationRepository.save(reg);
  }

  private String serializeTrustmarks(final List<TrustmarkRequestDto> trustmarks) {
    if (trustmarks == null || trustmarks.isEmpty()) {
      return null;
    }
      return this.objectMapper.writeValueAsString(trustmarks);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> deserializePolicy(final String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
      return this.objectMapper.readValue(json, Map.class);
  }

  private static JoinDto toJoinDto(final Registration reg) {
    final JoinDto dto = new JoinDto();
    dto.setJoinId(reg.getRegistrationId());
    dto.setEntityId(reg.getEntityId());
    dto.setRegistrationId(reg.getRegistrationFlow().getFlowId());
    dto.setStatusFedreg(switch (reg.getStatus()) {
      case APPROVED -> FedRegStatus.REGISTERED;
      case PENDING -> FedRegStatus.ONGOING;
      case REJECTED -> FedRegStatus.DENY;
    });
    dto.setRejectionReason(reg.getRejectionReason().toString());
    return dto;
  }
}