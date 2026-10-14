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

import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.step.UserPresentation;

import java.util.List;

/**
 * Immutable record of one step's execution outcome within a {@link ProcessReport}.
 *
 * @param stepName display name of the step
 * @param result the step's execution result
 * @param contextDiff changes made to the pipeline context by this step
 * @param preview what the step showed the operator when it paused for manual review, otherwise {@code null}
 * @param userStep how the step is shown to the user, {@code null} if it is not shown for the user
 * @param declineReason the reason the user sees if the step fails, {@code null} if the flow gives none
 * @author Per Fredrik Plars
 */
public record StepExecutionRecord(String stepName, StepResult result, List<ContextDiffEntry> contextDiff,
    StepPreview preview, UserPresentation.UserStep userStep, String declineReason) {

  /**
   * Creates a record for a step that is not shown for the user.
   *
   * @param stepName display name of the step
   * @param result the step's execution result
   * @param contextDiff changes made to the pipeline context by this step
   * @param preview what the step showed the operator when it paused for manual review, otherwise {@code null}
   */
  public StepExecutionRecord(final String stepName, final StepResult result,
      final List<ContextDiffEntry> contextDiff, final StepPreview preview) {
    this(stepName, result, contextDiff, preview, null, null);
  }

  /**
   * Creates a record for a step that did not pause for review.
   *
   * @param stepName display name of the step
   * @param result the step's execution result
   * @param contextDiff changes made to the pipeline context by this step
   */
  public StepExecutionRecord(final String stepName, final StepResult result,
      final List<ContextDiffEntry> contextDiff) {
    this(stepName, result, contextDiff, null);
  }

  /**
   * Returns a copy of this record shown to the user as the flow configures the step, with the reason the user sees if
   * it fails.
   *
   * @param definition the step as configured in the flow
   * @return the copy
   */
  public StepExecutionRecord shownAs(final StepDefinition definition) {
    return new StepExecutionRecord(this.stepName, this.result, this.contextDiff, this.preview,
        UserPresentation.of(definition.config()).orElse(null),
        UserPresentation.declineReason(definition.config()).orElse(null));
  }
}
