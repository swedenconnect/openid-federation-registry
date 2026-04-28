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
package se.swedenconnect.oidf.registry.registrationflow.process.step;

import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;

import java.util.List;
import java.util.UUID;

/**
 * A single unit of work in a processing pipeline.
 *
 * @author Per Fredrik Plars
 */
public interface Step {

  /**
   * Executes this step against the shared pipeline context.
   *
   * @param ctx shared pipeline context
   * @param config step-specific configuration
   * @return execution result
   */
  StepResult execute(ProcessContext ctx, StepConfig config);

  /**
   * Returns the unique ID of this step.
   *
   * @return step UUID
   */
  UUID getStepId();

  /**
   * Returns the display name of this step.
   *
   * @return step name
   */
  default String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Returns a human-readable description of what this step does.
   *
   * @return step description
   */
  default String getDescription() {
    return "<NoDescription defined>";
  }

  /**
   * Returns all declared configuration values for this step.
   *
   * @return list of configuration value descriptors
   */
  List<StepConfigurationValue> getStepConfigurationValues();
}
