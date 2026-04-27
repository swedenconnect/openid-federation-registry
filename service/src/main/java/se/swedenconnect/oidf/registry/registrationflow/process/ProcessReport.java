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
 * @author Per Fredrik Plars
 */
public record ProcessReport(ProcessStatus status, List<StepExecutionRecord> steps) {

  public static ProcessReport completed(final List<StepExecutionRecord> steps) {
    return new ProcessReport(ProcessStatus.COMPLETED, List.copyOf(steps));
  }

  public static ProcessReport skipped(final List<StepExecutionRecord> steps) {
    return new ProcessReport(ProcessStatus.SKIPPED, List.copyOf(steps));
  }

  public boolean isSuccessful() {
    return status == ProcessStatus.COMPLETED
        && steps.stream().noneMatch(r -> r.result().status() == StepStatus.FAILURE);
  }
}
