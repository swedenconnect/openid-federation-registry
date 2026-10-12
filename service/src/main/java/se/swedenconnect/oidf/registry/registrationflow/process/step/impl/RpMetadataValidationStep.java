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

import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.util.List;
import java.util.UUID;

/**
 * Validates RP metadata against a configured validation profile.
 *
 * @author Per Fredrik Plars
 */
@Component
public class RpMetadataValidationStep extends NoConfigStepAdapter implements ReviewableStep {

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    return StepResult.success();
  }

  @Override
  public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
    return StepPreview.of(PreviewSection.json("Metadata",
        ctx.<JSONObject>get(ContextKey.ENTITY_CONFIGURATION_METADATA).orElse(new JSONObject())));
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("B39C5E4E-12AB-4598-9D7B-54B76E62BCC9");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(new StepConfigurationValue(MANUAL_REVIEW,
        StepConfigurationValue.DATA_TYPE.BOOLEAN,
        "If true, registration requires manual approval before proceeding", "false"));
  }
}
