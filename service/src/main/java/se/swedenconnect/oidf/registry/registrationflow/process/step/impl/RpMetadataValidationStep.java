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
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.EntityMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates RP metadata against a configured validation profile.
 *
 * @author Per Fredrik Plars
 */
@Component
public class RpMetadataValidationStep implements Step<RpMetadataValidationConfig> {

  @Override
  public StepResult execute(final ProcessContext ctx, final RpMetadataValidationConfig config) {
    final EntityMetadata metadata = ctx.getRequired(ContextKey.ENTITY_METADATA);

    final List<StepIssue> issues = new ArrayList<>();

    if (metadata.scopes() == null || metadata.scopes().isEmpty()) {
      issues.add(new StepIssue("scopes", "Scopes are missing", Severity.ERROR));
    }

    if (!config.getRequiredScopes().isEmpty() && metadata.scopes() != null) {
      config.getRequiredScopes().stream()
          .filter(required -> !metadata.scopes().contains(required))
          .forEach(missing -> issues.add(
              new StepIssue("scopes", "Required scope missing: " + missing, Severity.ERROR)));
    }

    if (!issues.isEmpty()) {
      return StepResult.failure("Metadata validation failed", issues);
    }
    return StepResult.success();
  }
}
