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
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.registrationflow.dto.Mapper;
import se.swedenconnect.oidf.registry.registrationflow.metadata.AppliedPolicy;
import se.swedenconnect.oidf.registry.registrationflow.metadata.MetadataPolicyGenerator;
import se.swedenconnect.oidf.registry.registrationflow.metadata.PinnedValues;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.step.MissingContextValueException;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepStatus;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.LoadEntityConfigurationStep;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.MetadataPolicyCreationStep;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.dto.MetadataDryRunDto;
import se.swedenconnect.oidf.registry.registrations.dto.PinnedParameterDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dry run of the metadata steps of a registration flow: works out what the Subordinate Statement would pin and
 * override, and what is in error, without registering anything.
 *
 * @author Felix Hellman
 */
@Service
public class MetadataDryRunService {

  private final FlowAssignmentRepository flowAssignmentRepository;
  private final RegistrationStepRepository registrationStepRepository;
  private final OrganizationService organizationService;

  /**
   * Constructor.
   *
   * @param flowAssignmentRepository repository for flow assignments, by join ID
   * @param registrationStepRepository repository of the defined steps
   * @param organizationService service checking the organization's registered domains
   */
  public MetadataDryRunService(final FlowAssignmentRepository flowAssignmentRepository,
      final RegistrationStepRepository registrationStepRepository,
      final OrganizationService organizationService) {
    this.flowAssignmentRepository = flowAssignmentRepository;
    this.registrationStepRepository = registrationStepRepository;
    this.organizationService = organizationService;
  }

  /**
   * Dry run of the metadata steps of the flow behind a join ID. The request is checked as a join is. The entity's
   * metadata is the one in the request (hosted entity) or, without one, its Entity Configuration is fetched as
   * {@link LoadEntityConfigurationStep} does (self-managed entity). The flow's {@link MetadataPolicyCreationStep} then
   * builds the claims and runs its checks. Nothing is stored: no registration, hosted entity or Subordinate Statement
   * is created or changed.
   *
   * @param organizationRecord the calling organization
   * @param joinId the join ID of the flow
   * @param request the join request: the entity identifier and, for a hosted entity, its metadata
   * @return what the Subordinate Statement would pin and override, and what is in error
   * @throws RegistryServerException if the join ID is unknown or its flow is disabled
   */
  @Transactional(readOnly = true)
  public MetadataDryRunDto dryRun(final OrganizationRecord organizationRecord, final UUID joinId,
      final RegistrationJoinRequestDto request) {
    request.setJoinId(joinId);
    ValidateDto.init(organizationRecord).validate(request);
    final Map<String, Object> bodyMetadata = request.getMetadata();
    final boolean hosted = bodyMetadata != null && !bodyMetadata.isEmpty();
    if (hosted) {
      this.organizationService.requireRegisteredDomain(organizationRecord, request.getEntityIdentifier());
    }

    final FlowAssignment flowAssignment = this.flowAssignmentRepository.findById(joinId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND, "Join flow not found"));
    final RegistrationFlow registrationFlow = flowAssignment.getRegistrationFlow();
    if (!registrationFlow.isEnabled()) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Registration flow is disabled: " + registrationFlow.getName());
    }
    final List<StepDefinition> steps =
        Mapper.toProcessFlow(registrationFlow, this.registrationStepRepository).getProcessFlow();

    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.ENTITY_ID, request.getEntityIdentifier());
    ctx.put(ContextKey.TAIM_ID, flowAssignment.getTaIm().getTaImId());
    ctx.put(ContextKey.JOIN_ID, flowAssignment.getAssignId());
    ctx.put(ContextKey.ORG, organizationRecord);
    if (hosted) {
      // As HostedEntityRegistrationStep does; the federation keys it also loads play no part in the metadata.
      ctx.put(ContextKey.REQUEST_METADATA, new JSONObject(bodyMetadata));
      ctx.put(ContextKey.ENTITY_CONFIGURATION_METADATA, new JSONObject(bodyMetadata));
    }

    MetadataPolicyCreationStep.Evaluation evaluation = null;
    try {
      for (final StepDefinition def : steps) {
        if (def.step() instanceof LoadEntityConfigurationStep && def.step().canApply(ctx, def.config())) {
          final StepResult loaded = def.step().buildContext(ctx, def.config());
          if (loaded.status() == StepStatus.FAILURE) {
            return failed(Optional.ofNullable(loaded.issues()).orElse(List.of()).stream()
                .map(StepIssue::message).toList());
          }
        }
        else if (def.step() instanceof final MetadataPolicyCreationStep creation
            && creation.canApply(ctx, def.config())) {
          if (ctx.get(ContextKey.ENTITY_CONFIGURATION_METADATA).isEmpty()) {
            return failed(List.of("The flow loads no entity configuration: give the entity's metadata"));
          }
          evaluation = creation.dryRun(ctx, def.config());
          break;
        }
      }
    }
    catch (final MissingContextValueException | IllegalStateException e) {
      return failed(List.of(e.getMessage()));
    }
    if (evaluation == null) {
      return new MetadataDryRunDto(List.of(), List.of(), List.of(), false,
          List.of(new MetadataDryRunDto.Check(PreviewSection.CheckStatus.INFO.name(),
              "The flow builds no metadata or metadata_policy claim")),
          null, null);
    }

    final MetadataPolicyGenerator.Result result = evaluation.result();
    final JSONObject entityMetadata = ctx.getRequired(ContextKey.ENTITY_CONFIGURATION_METADATA, JSONObject.class);
    final AppliedPolicy.Outcome applied =
        AppliedPolicy.apply(entityMetadata, result.metadata(), result.metadataPolicy());
    final List<String> errors = new ArrayList<>(evaluation.problems());
    errors.addAll(applied.errors());

    return new MetadataDryRunDto(
        PinnedValues.parameters(Map.of("metadata_policy", result.metadataPolicy()), result.metadata()).stream()
            .map(p -> new PinnedParameterDto(p.entityType(), p.parameter(), p.value(), p.claim()))
            .toList(),
        applied.overrides().stream()
            .map(o -> new MetadataDryRunDto.Overridden(o.entityType(), o.parameter(), o.entityValue(), o.value(),
                o.claim(), o.operators()))
            .toList(),
        List.copyOf(errors),
        evaluation.failed(),
        result.checks().stream()
            .map(check -> new MetadataDryRunDto.Check(check.status().name(), check.message()))
            .toList(),
        result.metadata(),
        result.metadataPolicy());
  }

  private static MetadataDryRunDto failed(final List<String> errors) {
    return new MetadataDryRunDto(List.of(), List.of(), errors, true, List.of(), null, null);
  }
}
