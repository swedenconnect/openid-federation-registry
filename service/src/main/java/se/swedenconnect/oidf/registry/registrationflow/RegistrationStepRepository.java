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

import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Collects all steps and gives a way to access them
 *
 * @author Per Fredrik Plars
 */
@Configuration
public class RegistrationStepRepository {

  final List<Step> definedSteps;

  /**
   * Creating a repository of all the defined steps
   *
   * @param definedStep All the steps created in Spring
   */
  public RegistrationStepRepository(final Step... definedStep) {
    this.definedSteps = List.of(definedStep);
    Assert.isTrue(this.definedSteps.stream()
        .map(Step::getStepId)
        .collect(Collectors.toSet())
        .size() == this.definedSteps.size(), "StepId has to be unique for all steps");

  }

  /**
   * Getting step by there id
   *
   * @param stepId Id of step
   * @return Optional Step
   */
  public Optional<Step> findStepById(final UUID stepId) {
    if (stepId == null) {
      return Optional.empty();
    }
    return this.definedSteps.stream().filter(step -> step.getStepId().equals(stepId)).findFirst();
  }

  /**
   * Getting a list of defined steps
   *
   * @return Defined steps
   */
  public List<Step> getDefinedSteps() {
    return Collections.unmodifiableList(this.definedSteps);
  }

  /**
   * Returning steps that are defined as public
   * @return list of public steps
   */
  public List<Step> getPublicDefinedSteps() {
    return this.definedSteps.stream()
        .filter(Step::isPublic)
        .filter(step -> step.stepType().equals(Step.StepType.MID))
        .collect(Collectors.toList());
  }

  /**
   * If a step is public
   * @param stepId StepID to check
   * @return true if public
   */
  public boolean isPublic(final UUID stepId) {
    return this.findStepById(stepId)
        .filter(Step::isPublic)
        .filter(step -> step.stepType().equals(Step.StepType.MID))
        .map(Step::isPublic)
        .orElse(false);
  }

  /**
   * Pre default steps
   * @return list of default pre steps
   */
  public List<Step> preDefaultSteps() {
    return this.definedSteps.stream()
        .filter(step -> step.stepType().equals(Step.StepType.PRE))
        .collect(Collectors.toList());
  }

  /**
   * Post default steps
   * @return list of default post steps
   */
  public List<Step> postDefaultSteps() {
    return this.definedSteps.stream()
        .filter(step -> step.stepType().equals(Step.StepType.POST))
        .collect(Collectors.toList());
  }
}
