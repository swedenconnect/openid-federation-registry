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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationStepRepositoryTest {

  private static final List<StepConfigurationValue> REVIEW_VALUES = List.of(new StepConfigurationValue(
      "manualreview", StepConfigurationValue.DATA_TYPE.BOOLEAN, "Pause for review", "false"));

  private static class PlainStep implements Step {

    private final List<StepConfigurationValue> values;

    PlainStep(final List<StepConfigurationValue> values) {
      this.values = values;
    }

    @Override
    public StepResult execute(final ProcessContext ctx, final StepConfig config) {
      return StepResult.success();
    }

    @Override
    public UUID getStepId() {
      return UUID.randomUUID();
    }

    @Override
    public List<StepConfigurationValue> getStepConfigurationValues() {
      return this.values;
    }
  }

  private static final class PreviewingStep extends PlainStep implements ReviewableStep {

    PreviewingStep() {
      super(REVIEW_VALUES);
    }

    @Override
    public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
      return StepPreview.of();
    }
  }

  @Test
  @DisplayName("A step offering manual review without a preview stops the application from starting")
  void manualReviewWithoutPreviewIsRefused() {
    assertThatThrownBy(() -> new RegistrationStepRepository(new PlainStep(REVIEW_VALUES)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ReviewableStep");
  }

  @Test
  @DisplayName("Reviewable steps, and steps that never pause, are accepted")
  void reviewableAndPlainStepsAreAccepted() {
    assertThatCode(() -> new RegistrationStepRepository(new PreviewingStep(), new PlainStep(List.of())))
        .doesNotThrowAnyException();
  }
}
