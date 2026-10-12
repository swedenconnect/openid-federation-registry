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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepStatus;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.DefaultConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the engine's manual-approval gate, in particular the pre-validated trust mark bypass: the
 * sub-flow must run in full, it just must not stop to ask.
 */
class ProcessEngineApprovalGateTest {

  private final ProcessEngine engine = new ProcessEngine();

  /** Records whether {@code execute} was reached, which is the whole question the gate decides. */
  private static class RecordingStep implements ReviewableStep {

    private final AtomicBoolean executed = new AtomicBoolean(false);

    @Override
    public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
      return StepPreview.of(PreviewSection.text("What happens", "The step records that it ran"));
    }

    @Override
    public StepResult execute(final ProcessContext ctx, final StepConfig config) {
      this.executed.set(true);
      return StepResult.success("Step ran");
    }

    @Override
    public UUID getStepId() {
      return UUID.fromString("11111111-2222-3333-4444-555555555555");
    }

    @Override
    public List<StepConfigurationValue> getStepConfigurationValues() {
      return List.of();
    }
  }

  private static List<StepDefinition> gatedPipeline(final RecordingStep step) {
    return List.of(new StepDefinition(step, new DefaultConfig(Map.of("manualreview", "true"))));
  }

  @Test
  @DisplayName("A gated step pauses the pipeline when nothing has approved it")
  void gatedStepPausesWithoutApproval() {
    final RecordingStep step = new RecordingStep();

    final ProcessReport report = this.engine.run(gatedPipeline(step), new ProcessContext());

    assertThat(report.isPendingApproval()).isTrue();
    assertThat(step.executed).isFalse();
    assertThat(report.steps()).singleElement()
        .satisfies(record -> assertThat(record.result().status()).isEqualTo(StepStatus.PENDING_APPROVAL));
  }

  @Test
  @DisplayName("A gated step runs straight through when the context carries the pre-validated trust mark flag")
  void gatedStepRunsWhenTrustMarkIsPreValidated() {
    final RecordingStep step = new RecordingStep();
    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.TRUST_MARK_PRE_VALIDATED, Boolean.TRUE);

    final ProcessReport report = this.engine.run(gatedPipeline(step), ctx);

    assertThat(report.isPendingApproval()).isFalse();
    assertThat(report.isSuccessful()).isTrue();
    assertThat(step.executed).isTrue();
    assertThat(report.steps()).singleElement().satisfies(record -> {
      assertThat(record.result().status())
          .as("The auto-approval stays visible in the step trail")
          .isEqualTo(StepStatus.WARNING);
      assertThat(record.result().message())
          .contains("Auto-approved")
          .contains("pre-validated trust mark")
          .contains("Step ran");
    });
  }

  @Test
  @DisplayName("An ungated step is unaffected by the flag")
  void ungatedStepIsUnaffectedByTheFlag() {
    final RecordingStep step = new RecordingStep();
    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.TRUST_MARK_PRE_VALIDATED, Boolean.TRUE);

    final ProcessReport report = this.engine.run(
        List.of(new StepDefinition(step, new DefaultConfig(Map.of()))), ctx);

    assertThat(report.isSuccessful()).isTrue();
    assertThat(step.executed).isTrue();
    assertThat(report.steps()).singleElement()
        .satisfies(record -> assertThat(record.result().status()).isEqualTo(StepStatus.SUCCESS));
  }

  @Test
  @DisplayName("An already-approved gated step is not double-annotated as auto-approved")
  void alreadyApprovedGatedStepIsNotAnnotated() {
    final RecordingStep step = new RecordingStep();
    final ProcessContext ctx = new ProcessContext();
    ctx.put(ContextKey.STEP_APPROVED, Boolean.TRUE);

    final ProcessReport report = this.engine.run(gatedPipeline(step), ctx);

    assertThat(report.isSuccessful()).isTrue();
    assertThat(step.executed).isTrue();
    assertThat(report.steps()).singleElement()
        .satisfies(record -> assertThat(record.result().status()).isEqualTo(StepStatus.SUCCESS));
  }

  @Test
  @DisplayName("A paused step stores its preview with the pending result")
  void pausedStepStoresItsPreview() {
    final ProcessReport report = this.engine.run(gatedPipeline(new RecordingStep()), new ProcessContext());

    assertThat(report.steps()).singleElement().satisfies(record -> {
      assertThat(record.preview()).isNotNull();
      assertThat(record.preview().sections()).singleElement()
          .satisfies(section -> assertThat(section.text()).isEqualTo("The step records that it ran"));
    });
  }

  @Test
  @DisplayName("A step that ran without pausing has no preview")
  void stepThatRanHasNoPreview() {
    final ProcessReport report = this.engine.run(
        List.of(new StepDefinition(new RecordingStep(), new DefaultConfig(Map.of()))), new ProcessContext());

    assertThat(report.steps()).singleElement().satisfies(record -> assertThat(record.preview()).isNull());
  }

  @Test
  @DisplayName("A step that cannot build its preview still pauses, with a preview saying so")
  void brokenPreviewStillPauses() {
    final RecordingStep step = new RecordingStep() {
      @Override
      public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
        throw new IllegalStateException("metadata missing");
      }
    };

    final ProcessReport report = this.engine.run(gatedPipeline(step), new ProcessContext());

    assertThat(report.isPendingApproval()).isTrue();
    assertThat(report.steps()).singleElement().satisfies(record ->
        assertThat(record.preview().sections()).singleElement()
            .satisfies(section -> assertThat(section.text()).contains("metadata missing")));
  }

  @Test
  @DisplayName("A step that is not reviewable never pauses, whatever its configuration says")
  void nonReviewableStepDoesNotPause() {
    final AtomicBoolean executed = new AtomicBoolean(false);
    final Step plain = new Step() {
      @Override
      public StepResult execute(final ProcessContext ctx, final StepConfig config) {
        executed.set(true);
        return StepResult.success();
      }

      @Override
      public UUID getStepId() {
        return UUID.fromString("22222222-2222-3333-4444-555555555555");
      }

      @Override
      public List<StepConfigurationValue> getStepConfigurationValues() {
        return List.of();
      }
    };

    final ProcessReport report = this.engine.run(
        List.of(new StepDefinition(plain, new DefaultConfig(Map.of("manualreview", "true")))), new ProcessContext());

    assertThat(report.isPendingApproval()).isFalse();
    assertThat(executed).isTrue();
  }
}
