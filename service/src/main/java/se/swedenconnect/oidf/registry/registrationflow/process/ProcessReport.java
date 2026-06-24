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

import se.swedenconnect.oidf.registry.registrationflow.process.step.StepStatus;

import java.util.List;

/**
 * Aggregated report for a complete pipeline run.
 *
 * @param status overall pipeline status
 * @param steps ordered list of step execution records
 * @author Per Fredrik Plars
 */
public record ProcessReport(ProcessStatus status, List<StepExecutionRecord> steps) {

  /**
   * Creates a report for a fully completed pipeline.
   *
   * @param steps step execution records
   * @return completed report
   */
  public static ProcessReport completed(final List<StepExecutionRecord> steps) {
    return new ProcessReport(ProcessStatus.COMPLETED, List.copyOf(steps));
  }

  /**
   * Creates a report for a pipeline that was skipped after a step failure.
   *
   * @param steps step execution records up to the failure
   * @return skipped report
   */
  public static ProcessReport skipped(final List<StepExecutionRecord> steps) {
    return new ProcessReport(ProcessStatus.SKIPPED, List.copyOf(steps));
  }

  /**
   * Creates a report for a pipeline paused awaiting manual step approval.
   *
   * @param steps step execution records up to and including the pending step
   * @return pending-approval report
   */
  public static ProcessReport pendingApproval(final List<StepExecutionRecord> steps) {
    return new ProcessReport(ProcessStatus.PENDING_APPROVAL, List.copyOf(steps));
  }

  /**
   * Returns true if the pipeline completed without any step failures.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return this.status == ProcessStatus.COMPLETED
        && this.steps.stream().noneMatch(r -> r.result().status() == StepStatus.FAILURE);
  }

  /**
   * Returns true if the pipeline is paused awaiting manual step approval.
   *
   * @return true if pending approval
   */
  public boolean isPendingApproval() {
    return this.status == ProcessStatus.PENDING_APPROVAL;
  }
}
