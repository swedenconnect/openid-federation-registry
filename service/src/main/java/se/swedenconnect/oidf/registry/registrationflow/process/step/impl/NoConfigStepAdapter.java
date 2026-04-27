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

import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;

import java.util.List;

/**
 * Validates RP metadata against a configured validation profile.
 *
 * @author Per Fredrik Plars
 */
public abstract class NoConfigStepAdapter implements Step {

  @Override
  public String getName() {
    return Step.super.getName();
  }

  @Override
  public String getDescription() {
    return Step.super.getDescription();
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of();
  }
}
