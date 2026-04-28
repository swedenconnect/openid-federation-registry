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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.registrationflow.dto.ConfigValueDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.Mapper;
import se.swedenconnect.oidf.registry.registrationflow.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.StepDto;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowRepository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

/**
 * Operations for registration flows
 *
 * @author Per Fredrik Plars
 */
@Service
public class RegistrationFlowService {

  private final RegistrationStepRepository registrationStepRepository;
  private final TaImRepository taImRepository;
  private final FlowRepository flowRepository;
  private final JsonMapper objectMapper;

  /**
   * Constructs a new RegistrationFlowService.
   *
   * @param registrationStepRepository repository of defined pipeline steps
   * @param taImRepository repository of trust anchor intermediates
   * @param flowRepository repository of registration flows
   * @param objectMapper JSON mapper for flow definition serialization
   */
  public RegistrationFlowService(final RegistrationStepRepository registrationStepRepository,
      final TaImRepository taImRepository, final FlowRepository flowRepository,
      final JsonMapper objectMapper) {
    this.registrationStepRepository = registrationStepRepository;
    this.taImRepository = taImRepository;
    this.flowRepository = flowRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a new registration flow.
   *
   * @param registrationFlowDto the flow definition
   * @param flowId the ID to assign to the flow
   * @return the created flow DTO
   */
  public RegistrationFlowDto createRegistrationFlow(final RegistrationFlowDto registrationFlowDto, final UUID flowId) {
    final ProcessFlow processFlow = Mapper.toDomain(registrationFlowDto, this.registrationStepRepository);
    // Think of how the data will be stored

    return registrationFlowDto;
  }

  /**
   * Updates an existing registration flow.
   *
   * @param registrationFlowDto the updated flow definition
   * @return the updated flow DTO
   */
  public RegistrationFlowDto updateRegistrationFlow(final RegistrationFlowDto registrationFlowDto) {
    return registrationFlowDto;
  }

  /**
   * Deletes the registration flow with the given ID.
   *
   * @param registrationFlowId the ID of the flow to delete
   */
  public void deleteRegistrationFlow(final UUID registrationFlowId) {
  }

  /**
   * Returns the registration flow with the given ID.
   *
   * @param registrationFlowId the flow ID
   * @return the flow DTO, or null if not found
   */
  public RegistrationFlowDto getRegistrationFlow(final UUID registrationFlowId) {
    return null;
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
    return Mapper.toDomain(dto, this.registrationStepRepository);
  }

  /**
   * Loads the registration flow with the given ID.
   *
   * @param registrationFlowId the flow ID to load
   */
  public void loadRegistrationFlow(final UUID registrationFlowId) {
  }

  /**
   * Returns all flows assigned to the given intermediate.
   *
   * @param taImId the intermediate ID
   * @return list of flow DTOs
   */
  public List<RegistrationFlowDto> getFlowsForIntermediate(final UUID taImId) {
    final TrustAnchorIntermediateModule taIm = this.taImRepository.findById(taImId)
        .orElseThrow(() -> new EntityNotFoundException("Intermediate not found: " + taImId));
    return taIm.getFlows().stream()
        .map(f -> new RegistrationFlowDto(f.getFlowId(), f.getName(), f.getDescription(), List.of()))
        .toList();
  }

  /**
   * Replaces all flows assigned to the given intermediate.
   *
   * @param taImId the intermediate ID
   * @param flowIds IDs of flows to assign
   */
  @Transactional
  public void setFlowsForIntermediate(final UUID taImId, final List<UUID> flowIds) {
    final TrustAnchorIntermediateModule taIm = this.taImRepository.findById(taImId)
        .orElseThrow(() -> new EntityNotFoundException("Intermediate not found: " + taImId));
    final List<RegistrationFlow> flows = this.flowRepository.findAllById(flowIds);
    taIm.getFlows().clear();
    taIm.getFlows().addAll(flows);
    this.taImRepository.save(taIm);
  }

  /**
   * Removes a specific flow from the given intermediate.
   *
   * @param taImId the intermediate ID
   * @param flowId the flow ID to remove
   */
  @Transactional
  public void removeFlowFromIntermediate(final UUID taImId, final UUID flowId) {
    final TrustAnchorIntermediateModule taIm = this.taImRepository.findById(taImId)
        .orElseThrow(() -> new EntityNotFoundException("Intermediate not found: " + taImId));
    taIm.getFlows().removeIf(f -> f.getFlowId().equals(flowId));
    this.taImRepository.save(taIm);
  }






}
