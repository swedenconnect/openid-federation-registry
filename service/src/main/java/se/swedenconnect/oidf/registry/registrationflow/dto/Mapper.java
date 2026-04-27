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

import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessFlow;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
public class DtoToModelMapper {

  public static RegistrationFlow convert(final RegistrationFlowDto dto) {

    final RegistrationFlow registrationFlow = new RegistrationFlow();
    registrationFlow.setFlowId(dto.flowId());
    registrationFlow.setName(dto.name());
    registrationFlow.setDescription(dto.description());

    final ProcessFlow processFlow = dto.steps()
        .stream()
        .map(stepInfoDto ->
            new ProcessFlow(stepInfoDto.stepId(),
                stepInfoDto
                    .config()
                    .stream()
                    .map(configValueDto ->)
                    .toList()))
        .toList();

    //registrationFlow.setFlowDefinition(dto.steps());

    return registrationFlow;
  }

}
