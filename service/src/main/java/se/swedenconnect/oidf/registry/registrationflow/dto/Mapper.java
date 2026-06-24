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

package se.swedenconnect.oidf.registry.registrationflow.dto;

import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationStepRepository;
import se.swedenconnect.oidf.registry.registrationflow.model.ConfigValueModel;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.model.StepModel;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.DefaultConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RegistrationFlowMapper
 *
 * @author Per Fredrik Plars
 */
public class Mapper {

  /**
   * Maps a {@link RegistrationFlowDto} to a domain {@link ProcessFlow}.
   *
   * @param dto the source DTO
   * @param registrationStepRepository repository used to resolve step references
   * @return domain process flow
   */
  public static ProcessFlow toProcessFlow(final RegistrationFlow dto,
      final RegistrationStepRepository registrationStepRepository) {

    final List<StepDefinition> steps = new ArrayList<>();
    registrationStepRepository.preDefaultSteps()
        .stream()
        .map(step -> new StepDefinition(step, new DefaultConfig(step.getStepConfigurationValues().stream()
            .collect(Collectors.toMap(
                StepConfigurationValue::name,
                StepConfigurationValue::defaultValue
            )))))
        .forEach(steps::add);

    List<StepModel> flowDef = Optional.ofNullable(dto.getFlowDefinition()).orElse(new ArrayList<>(0));
    if (flowDef.isEmpty()) {
      flowDef = registrationStepRepository.defaultMidSteps().stream()
          .map(step -> new StepModel(step.getStepId(), List.of()))
          .toList();
    }
    flowDef.stream()
        .filter(stepModel -> registrationStepRepository.isPublic(stepModel.stepId()))
        .map(stepInfoDto ->
            new StepDefinition(registrationStepRepository.findStepById(stepInfoDto.stepId()).orElseThrow(),
                new DefaultConfig(stepInfoDto.config().stream().collect(Collectors.toMap(
                    ConfigValueModel::key,
                    ConfigValueModel::value
                )))))
        .forEach(steps::add);

    registrationStepRepository.postDefaultSteps()
        .stream()
        .map(step -> new StepDefinition(step, new DefaultConfig(step.getStepConfigurationValues().stream()
            .collect(Collectors.toMap(
                StepConfigurationValue::name,
                StepConfigurationValue::defaultValue
            )))))
        .forEach(steps::add);

    return new ProcessFlow(dto.getFlowId(), dto.getName(), dto.getDescription(), steps);
  }

  /**
   * Builds a full sub-flow for a trust mark enrollment:
   * TRUST_MARK_ISSUER PRE steps → configured MID steps → TRUST_MARK_ISSUER POST steps.
   *
   * @param flow the flow definition (MID steps)
   * @param registrationStepRepository repository used to resolve step references
   * @return complete trust mark sub-flow
   */
  public static ProcessFlow toTrustMarkSubFlow(final RegistrationFlow flow,
      final RegistrationStepRepository registrationStepRepository) {
    final List<StepDefinition> steps = new ArrayList<>();

    registrationStepRepository.preTrustMarkSteps().stream()
        .map(step -> new StepDefinition(step, new DefaultConfig(step.getStepConfigurationValues().stream()
            .collect(Collectors.toMap(StepConfigurationValue::name, StepConfigurationValue::defaultValue)))))
        .forEach(steps::add);

    Optional.ofNullable(flow.getFlowDefinition()).orElse(List.of()).stream()
        .filter(stepModel -> registrationStepRepository.isPublic(stepModel.stepId()))
        .map(stepModel ->
            new StepDefinition(registrationStepRepository.findStepById(stepModel.stepId()).orElseThrow(),
                new DefaultConfig(stepModel.config().stream().collect(Collectors.toMap(
                    ConfigValueModel::key,
                    ConfigValueModel::value
                )))))
        .forEach(steps::add);

    registrationStepRepository.postTrustMarkSteps().stream()
        .map(step -> new StepDefinition(step, new DefaultConfig(step.getStepConfigurationValues().stream()
            .collect(Collectors.toMap(StepConfigurationValue::name, StepConfigurationValue::defaultValue)))))
        .forEach(steps::add);

    return new ProcessFlow(flow.getFlowId(), flow.getName(), flow.getDescription(), steps);
  }

  /**
   * Builds a {@link ProcessFlow} containing only the MID steps of the given flow.
   * Used for trust mark issuer sub-flows, which must not re-run PRE/POST framework steps.
   *
   * @param flow the flow definition to extract MID steps from
   * @param registrationStepRepository repository used to resolve step references
   * @return a process flow with only the configured MID steps
   * @deprecated Use {@link #toTrustMarkSubFlow} instead
   */
  @Deprecated
  public static ProcessFlow toMidOnlyProcessFlow(final RegistrationFlow flow,
      final RegistrationStepRepository registrationStepRepository) {
    final List<StepModel> flowDef = Optional.ofNullable(flow.getFlowDefinition()).orElse(List.of());
    final List<StepDefinition> steps = flowDef.stream()
        .filter(stepModel -> registrationStepRepository.isPublic(stepModel.stepId()))
        .map(stepModel ->
            new StepDefinition(registrationStepRepository.findStepById(stepModel.stepId()).orElseThrow(),
                new DefaultConfig(stepModel.config().stream().collect(Collectors.toMap(
                    ConfigValueModel::key,
                    ConfigValueModel::value
                )))))
        .toList();
    return new ProcessFlow(flow.getFlowId(), flow.getName(), flow.getDescription(), steps);
  }

  /**
   * Maps a {@link RegistrationFlowDto} with an explicit flow ID to a {@link RegistrationFlow} entity.
   *
   * @param dto the source DTO
   * @param flowId the ID to assign; overrides any ID already present in the DTO
   * @param organization the owning organization
   * @param registrationStepRepository repository used to resolve step references
   * @return the mapped entity
   */
  public static RegistrationFlow toModel(final RegistrationFlowDto dto, final UUID flowId,
      final Organization organization, final RegistrationStepRepository registrationStepRepository) {

    List<StepDto> incomingSteps = Optional.ofNullable(dto.steps()).orElse(List.of());
    if (incomingSteps.isEmpty() && dto.flowType() != Step.FlowType.TRUST_MARK_ISSUER) {
      incomingSteps = registrationStepRepository.defaultMidSteps().stream()
          .map(step -> new StepDto(step.getStepId(), step.getName(), step.getDescription(), List.of(), step.flowType()))
          .toList();
    }
    final List<StepModel> stepModels = incomingSteps.stream()
        .filter(stepDto -> registrationStepRepository.isPublic(stepDto.stepId()))
        .map(s -> new StepModel(s.stepId(),
            Optional.ofNullable(s.config()).orElse(List.of()).stream()
                .map(c -> new ConfigValueModel(c.key(), c.value()))
                .toList()))
        .toList();

    final RegistrationFlow registrationFlow = new RegistrationFlow();
    registrationFlow.setFlowId(flowId);
    registrationFlow.setOrganization(organization);
    registrationFlow.setName(dto.name());
    registrationFlow.setDescription(dto.description());
    registrationFlow.setDescriptionSv(dto.descriptionSv());
    registrationFlow.setTechnology(dto.technology());
    registrationFlow.setEntityType(dto.entityType());
    registrationFlow.setFlowType(dto.flowType() != null ? dto.flowType() : Step.FlowType.INTERMEDIATE);
    registrationFlow.setFlowDefinition(stepModels);

    return registrationFlow;
  }

  /**
   * Applies updated fields from a DTO onto an existing {@link RegistrationFlow} entity.
   *
   * @param existing the entity already loaded from the database
   * @param dto the updated DTO
   * @param registrationStepRepository repository used to resolve step references
   * @return the updated entity (same instance as {@code existing})
   */
  public static RegistrationFlow applyUpdate(final RegistrationFlow existing, final RegistrationFlowDto dto,
      final RegistrationStepRepository registrationStepRepository) {

    final List<StepModel> stepModels = Optional.ofNullable(dto.steps()).orElse(List.of())
        .stream()
        .filter(stepDto -> registrationStepRepository.isPublic(stepDto.stepId()))
        .map(s -> new StepModel(s.stepId(),
            Optional.ofNullable(s.config()).orElse(List.of()).stream()
                .map(c -> new ConfigValueModel(c.key(), c.value()))
                .toList()))
        .toList();

    existing.setName(dto.name());
    existing.setDescription(dto.description());
    existing.setDescriptionSv(dto.descriptionSv());
    existing.setTechnology(dto.technology());
    existing.setEntityType(dto.entityType());
    if (dto.flowType() != null) {
      existing.setFlowType(dto.flowType());
    }
    existing.setFlowDefinition(stepModels);
    return existing;
  }

}
