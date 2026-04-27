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

import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;

/**
 * Response DTO for a single pipeline step — its name, base settings and full configuration.
 *
 * @author Per Fredrik Plars
 */
public record ConfigValueDto(
    String key,
    String description,
    String value,
    String type,
    Object defaultValue
) {

  public static ConfigValueDto create(StepConfigurationValue stepConfigurationValue) {
    return new ConfigValueDto(stepConfigurationValue.name(),
        stepConfigurationValue.description(),
        null,
        stepConfigurationValue.dataType().toString(),
        stepConfigurationValue.defaultValue());
  }

}
