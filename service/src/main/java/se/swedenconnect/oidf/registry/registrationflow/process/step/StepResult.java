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
package se.swedenconnect.oidf.registry.registrationflow.process.step;

import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome returned by a pipeline step after execution.
 *
 * @param status execution outcome
 * @param message optional human-readable summary
 * @param issues list of validation issues found
 * @author Per Fredrik Plars
 */
public record StepResult(StepStatus status, String message, List<StepIssue> issues) {
  /**
   * Merge one stepResult with the other
   * @param appendResult
   * @return
   */
  public StepResult merge(final StepResult appendResult) {
    final StepStatus s = this.status;
    final String message = this.message+ ", " +appendResult.message;
    final List<StepIssue> issues = new ArrayList<>(this.issues);
    issues.addAll(appendResult.issues);
    return new StepResult(this.status, message, issues);
  }

  /**
   * Creates a successful result with no issues.
   *
   * @return successful StepResult
   */
  public static StepResult success() {
    return new StepResult(StepStatus.SUCCESS, null, List.of());
  }

  /**
   * Create a success result with a statusmessage
   *
   * @param statusMessage Status message
   * @return successful StepResult
   */
  public static StepResult success(final String statusMessage) {
    return new StepResult(StepStatus.SUCCESS, statusMessage, List.of());
  }

  /**
   * Creates a warning result.
   *
   * @param message human-readable summary
   * @param issues list of issues
   * @return warning StepResult
   */
  public static StepResult warning(final String message, final List<StepIssue> issues) {
    return new StepResult(StepStatus.WARNING, message, List.copyOf(issues));
  }

  /**
   * Creates a failure result.
   *
   * @param message human-readable summary
   * @param issues list of issues
   * @return failure StepResult
   */
  public static StepResult failure(final String message, final List<StepIssue> issues) {
    return new StepResult(StepStatus.FAILURE, message, List.copyOf(issues));
  }
}
