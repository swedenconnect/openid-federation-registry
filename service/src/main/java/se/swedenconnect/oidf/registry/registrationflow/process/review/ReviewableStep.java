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
package se.swedenconnect.oidf.registry.registrationflow.process.review;

import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;

/**
 * A step that can pause the pipeline for manual review. Only a reviewable step pauses: the engine asks
 * {@link #requiresManualReview(StepConfig)} after {@code buildContext}, and when it pauses it stores the
 * {@link #preview(ProcessContext, StepConfig)} with the step result so the operator sees what the step is about to
 * do.
 *
 * <p>The preview is built from what {@code buildContext} put in the context. A step that pauses must therefore load
 * everything the operator needs in {@code buildContext}, and leave every write to {@code execute}.</p>
 *
 * <p>A step that declares the {@link #MANUAL_REVIEW} configuration value must implement this interface; the step
 * repository refuses to start otherwise.</p>
 *
 * @author Felix Hellman
 */
public interface ReviewableStep extends Step {

  /** Name of the boolean configuration value that turns manual review on for a step. */
  String MANUAL_REVIEW = "manualreview";

  /**
   * Tells whether the pipeline pauses at this step for an operator decision. Defaults to the
   * {@link #MANUAL_REVIEW} configuration value; a step that always needs a decision overrides this.
   *
   * @param config the step configuration
   * @return {@code true} if the pipeline pauses before {@code execute}
   */
  default boolean requiresManualReview(final StepConfig config) {
    return config.getBoolean(MANUAL_REVIEW);
  }

  /**
   * Describes for the operator what this step is about to do. Must not change any state.
   *
   * @param ctx the pipeline context after {@code buildContext}
   * @param config the step configuration
   * @return the preview shown to the operator
   */
  StepPreview preview(ProcessContext ctx, StepConfig config);
}
