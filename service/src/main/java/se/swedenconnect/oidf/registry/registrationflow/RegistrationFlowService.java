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

  public RegistrationFlowService(final RegistrationStepRepository registrationStepRepository,
      final TaImRepository taImRepository, final FlowRepository flowRepository,
      final JsonMapper objectMapper) {
    this.registrationStepRepository = registrationStepRepository;
    this.taImRepository = taImRepository;
    this.flowRepository = flowRepository;
    this.objectMapper = objectMapper;
  }

  public RegistrationFlowDto createRegistrationFlow(final RegistrationFlowDto registrationFlowDto, final UUID flowId) {
    final ProcessFlow processFlow = Mapper.toDomain(registrationFlowDto, registrationStepRepository);
    // Think of how the data will be stored

    return registrationFlowDto;
  }

  public RegistrationFlowDto updateRegistrationFlow(final RegistrationFlowDto registrationFlowDto) {
    return registrationFlowDto;
  }

  public void deleteRegistrationFlow(final UUID registrationFlowId) {
  }

  public RegistrationFlowDto getRegistrationFlow(final UUID registrationFlowId) {
    return null;
  }

  public List<StepDto> getDefineSteps() {
    return registrationStepRepository.getDefinedSteps()
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

  public ProcessFlow buildProcessFlow(final UUID flowId) {
    final RegistrationFlow flow = this.flowRepository.findById(flowId)
        .orElseThrow(() -> new EntityNotFoundException("Flow not found: " + flowId));
    final RegistrationFlowDto dto = this.objectMapper.convertValue(
        flow.getFlowDefinition(), RegistrationFlowDto.class);
    return Mapper.toDomain(dto, this.registrationStepRepository);
  }

  public void loadRegistrationFlow(final UUID registrationFlowId) {
  }

  public List<RegistrationFlowDto> getFlowsForIntermediate(final UUID taImId) {
    final TrustAnchorIntermediateModule taIm = taImRepository.findById(taImId)
        .orElseThrow(() -> new EntityNotFoundException("Intermediate not found: " + taImId));
    return taIm.getFlows().stream()
        .map(f -> new RegistrationFlowDto(f.getFlowId(), f.getName(), f.getDescription(), List.of()))
        .toList();
  }

  @Transactional
  public void setFlowsForIntermediate(final UUID taImId, final List<UUID> flowIds) {
    final TrustAnchorIntermediateModule taIm = taImRepository.findById(taImId)
        .orElseThrow(() -> new EntityNotFoundException("Intermediate not found: " + taImId));
    final List<RegistrationFlow> flows = flowRepository.findAllById(flowIds);
    taIm.getFlows().clear();
    taIm.getFlows().addAll(flows);
    taImRepository.save(taIm);
  }

  @Transactional
  public void removeFlowFromIntermediate(final UUID taImId, final UUID flowId) {
    final TrustAnchorIntermediateModule taIm = taImRepository.findById(taImId)
        .orElseThrow(() -> new EntityNotFoundException("Intermediate not found: " + taImId));
    taIm.getFlows().removeIf(f -> f.getFlowId().equals(flowId));
    taImRepository.save(taIm);
  }






}
