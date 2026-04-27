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
package se.swedenconnect.oidf.registry.registrationflow.process;

import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.io.Serializable;

/**
 * Binds a {@link Step} to its typed configuration so that the {@link ProcessEngine} can handle heterogeneous step
 * definitions without knowing the concrete config type.
 *
 * @author Per Fredrik Plars
 */
public record StepDefinition(Step step, StepConfig config) implements Serializable {

  public StepResult run(final ProcessContext ctx) {
    return step.execute(ctx, config);
  }

  public String name() {
    return step.getName();
  }

  public boolean enabled() {
    return true;
  }

}
