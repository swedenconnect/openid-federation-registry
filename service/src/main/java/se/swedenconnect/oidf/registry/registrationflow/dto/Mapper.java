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

import se.swedenconnect.oidf.registry.registrationflow.RegistrationStepRepository;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * oidf-entity-registry
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
  public static ProcessFlow toDomain(final RegistrationFlowDto dto,
      final RegistrationStepRepository registrationStepRepository) {

    final List<StepDefinition> steps = dto.steps()
        .stream()
        .map(stepInfoDto ->
            new StepDefinition(registrationStepRepository.findStepById(stepInfoDto.stepId()).orElseThrow(),
                toStepConfig(stepInfoDto.config())))
        .toList();

    return new ProcessFlow(dto.flowId(), dto.name(), dto.description(), steps);
  }

  private static StepConfig toStepConfig(final List<ConfigValueDto> configValueDtos) {
    final Map<String, Object> stepConfig = new HashMap<>();
    configValueDtos.forEach(configValueDto ->
    {
      stepConfig.put(configValueDto.key(), configValueDto.value());
    });

    return new StepConfig(stepConfig) {
      @Override
      public List<StepConfigurationValue> getStepConfigurationValues() {
        return List.of();
      }
    };
  }

}
