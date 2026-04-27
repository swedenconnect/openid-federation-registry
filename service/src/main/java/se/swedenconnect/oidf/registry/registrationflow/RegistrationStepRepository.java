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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.RpMetadataValidationConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.RpMetadataValidationStep;

import java.util.List;

/**
 * Defines the ordered pipeline of registration flow steps.
 *
 * @author Per Fredrik Plars
 */
@Configuration
public class RegistrationFlowConfiguration {

  @Bean
  public List<StepDefinition<?>> registrationFlowSteps(
      final RpMetadataValidationStep rpMetadataValidationStep) {

    final RpMetadataValidationConfig validationConfig = new RpMetadataValidationConfig();
    validationConfig.setName("RP Metadata Validation");
    validationConfig.setFailOnError(true);

    return List.of(
        new StepDefinition<>(rpMetadataValidationStep, validationConfig)
    );
  }
}
