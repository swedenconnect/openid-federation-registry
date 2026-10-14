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

/**
 * A single issue reported by a pipeline step.
 *
 * @param field the field that caused the issue
 * @param message human-readable description of the issue
 * @param severity how severe the issue is
 * @param type whether the entity can fix it ({@link Type#VALIDATION}) or it needs an operator ({@link Type#TECHNICAL})
 * @author Per Fredrik Plars
 */
public record StepIssue(String field, String message, Severity severity, Type type) {

  /**
   * Creates a validation issue.
   *
   * @param field the field that caused the issue
   * @param message human-readable description of the issue
   * @param severity how severe the issue is
   */
  public StepIssue(final String field, final String message, final Severity severity) {
    this(field, message, severity, Type.VALIDATION);
  }

  /**
   * Creates a technical issue: one the entity cannot fix from what it registers, such as a fetch that failed.
   *
   * @param field the field that caused the issue
   * @param message human-readable description of the issue, for operators
   * @param severity how severe the issue is
   * @return the issue
   */
  public static StepIssue technical(final String field, final String message, final Severity severity) {
    return new StepIssue(field, message, severity, Type.TECHNICAL);
  }

  /**
   * What kind of issue it is, which decides what the member is told.
   */
  public enum Type {
    /** Something the entity registers is not accepted; the member is shown the message and can fix it. */
    VALIDATION,
    /** Something went wrong that the member cannot fix from the registration, such as a fetch that failed; resolved
     * with an operator. */
    TECHNICAL
  }
}
