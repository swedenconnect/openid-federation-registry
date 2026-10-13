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
package se.swedenconnect.oidf.registry.registrations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Execution outcome of a single pipeline step.
 *
 * @param stepName display name of the step
 * @param status step outcome (SUCCESS, SKIPPED, WARNING, FAILURE, PENDING_APPROVAL)
 * @param message optional human-readable summary
 * @param issues validation issues found during this step
 * @param contextDiff changes made to the pipeline context by this step
 * @param preview what the step showed the operator when it paused for manual review, otherwise {@code null}
 * @param reviewedBy who approved the step, {@code null} unless it ran after manual review
 * @param reviewedAt when the step was approved, {@code null} unless it ran after manual review
 * @author Per Fredrik Plars
 */
@Schema(name = "StepExecutionRecord")
public record StepExecutionRecordDto(

    @Schema(description = "Display name of the step", example = "LoadEntityConfigurationStep")
    String stepName,

    @Schema(description = "Step execution outcome", example = "SUCCESS")
    String status,

    @Schema(description = "Human-readable summary of the step result")
    String message,

    @Schema(description = "Validation issues found during this step")
    List<StepIssueDto> issues,

    @Schema(description = "Changes made to the pipeline context by this step")
    List<ContextDiffEntryDto> contextDiff,
    @Schema(description = "What the step showed the operator when it paused for manual review")
    StepPreview preview,
    @Schema(description = "Who approved this step, set when the step ran after manual review")
    String reviewedBy,
    @Schema(description = "When this step was approved, set when the step ran after manual review")
    OffsetDateTime reviewedAt
) {

  /**
   * Creates a record for a step that was not paused for review.
   *
   * @param stepName display name of the step
   * @param status step outcome
   * @param message optional human-readable summary
   * @param issues validation issues found during this step
   * @param contextDiff changes made to the pipeline context by this step
   */
  public StepExecutionRecordDto(final String stepName, final String status, final String message,
      final List<StepIssueDto> issues, final List<ContextDiffEntryDto> contextDiff) {
    this(stepName, status, message, issues, contextDiff, null, null, null);
  }

  /**
   * Returns a copy of this record marked as approved by an operator.
   *
   * @param reviewer who approved the step
   * @param at when the step was approved
   * @return the marked copy
   */
  public StepExecutionRecordDto approvedBy(final String reviewer, final OffsetDateTime at) {
    return new StepExecutionRecordDto(this.stepName, this.status, this.message, this.issues, this.contextDiff,
        this.preview, reviewer, at);
  }
}
