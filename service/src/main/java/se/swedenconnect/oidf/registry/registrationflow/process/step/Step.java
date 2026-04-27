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

/**
 * A single unit of work in a processing pipeline.
 *
 * @param <C> the configuration type for this step
 * @author Per Fredrik Plars
 */
public interface Step<C extends StepConfig> {

  StepResult execute(ProcessContext ctx, C config);

  default String stepName(final C config) {
    return config.getName() != null ? config.getName() : getClass().getSimpleName();
  }
}
