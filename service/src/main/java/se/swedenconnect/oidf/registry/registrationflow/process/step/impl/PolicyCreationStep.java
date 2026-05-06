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
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.io.Serializable;
import java.util.UUID;

/**
 * Validates RP metadata against a configured validation profile.
 *
 * @author Per Fredrik Plars
 */
@Component
public class PolicyCreationStep extends NoConfigStepAdapter {


  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final Serializable o = ctx.getRequired(ContextKey.ENTITY_CONFIGURATION_METADATA);
    return StepResult.success();
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("FCF26DE1-93BF-4B80-A5E5-F4C88BDFFFEA");
  }

}
