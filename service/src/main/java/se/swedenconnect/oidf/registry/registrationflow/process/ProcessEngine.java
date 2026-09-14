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
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.registrationflow.process;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepStatus;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes a sequential pipeline of {@link StepDefinition} instances against a shared {@link ProcessContext}.
 *
 * @author Per Fredrik Plars
 */
@Component
@Slf4j
public class ProcessEngine {

  /**
   * Recorded on a gated step that was passed because the organization holds the trust mark as pre-validated.
   * The concrete trust mark type is named by {@code TrustMarkIssuerRegistrationStep} in the parent step trail.
   */
  static final String AUTO_APPROVED_MESSAGE =
      "Auto-approved: organization holds a pre-validated trust mark for this enrollment";

  /**
   * Executes the pipeline sequentially using a four-phase lifecycle per step:
   * <ol>
   *   <li>{@code canApply} — skip the step if not applicable</li>
   *   <li>{@code buildContext} — read-only data loading; skipped when resuming after approval</li>
   *   <li>approval gate — halts the pipeline when {@code manualreview=true} and not yet approved</li>
   *   <li>{@code execute} — write-side action</li>
   * </ol>
   * The {@code STEP_APPROVED} context flag is consumed per step so that only the first
   * resumed step is bypassed; subsequent steps requiring approval still halt normally.
   *
   * @param steps ordered list of step definitions
   * @param ctx shared pipeline context
   * @return aggregated report
   */
  public ProcessReport run(final List<StepDefinition> steps, final ProcessContext ctx) {
    final List<StepExecutionRecord> records = new ArrayList<>();

    for (final StepDefinition def : steps) {
      if (!def.enabled()) {
        log.debug("Skipping step: {}", def.name());
        continue;
      }

      final Map<String, String> before = ctx.snapshot();

      // Phase 1: canApply
      if (!def.step().canApply(ctx, def.config())) {
        log.info("Step {} not applicable — skipping", def.name());
        records.add(new StepExecutionRecord(def.name(),
            StepResult.skipped("Step not applicable"), List.of()));
        continue;
      }

      // Read the approval flag — do NOT remove yet so execute (and any sub-flows it spawns)
      // can still propagate it. Flag is removed after execute returns.
      final boolean alreadyApproved = ctx.<Boolean>get(ContextKey.STEP_APPROVED).orElse(false);
      String autoApprovalMessage = null;

      if (!alreadyApproved) {
        // Phase 2: buildContext
        final StepResult buildResult = def.step().buildContext(ctx, def.config());
        if (buildResult.status() == StepStatus.FAILURE) {
          log.error("Step {} buildContext failed — aborting pipeline", def.name());
          final List<ContextDiffEntry> diff = computeDiff(before, ctx.snapshot());
          records.add(new StepExecutionRecord(def.name(), buildResult, diff));
          return ProcessReport.skipped(records);
        }

        // Phase 3: approval gate. A trust mark the organization is already pre-validated for needs no second
        // look: the sub-flow runs in full, the gated step just does not stop at the gate.
        if (def.config().getBoolean("manualreview")) {
          if (!ctx.<Boolean>get(ContextKey.TRUST_MARK_PRE_VALIDATED).orElse(false)) {
            log.info("Step {} requires manual approval — pausing pipeline", def.name());
            final List<ContextDiffEntry> diff = computeDiff(before, ctx.snapshot());
            records.add(new StepExecutionRecord(def.name(),
                StepResult.pendingApproval("Step requires manual approval"), diff));
            return ProcessReport.pendingApproval(records);
          }
          log.info("Step {} requires manual approval, but the organization holds a pre-validated trust mark "
              + "for this enrollment — auto-approving", def.name());
          autoApprovalMessage = AUTO_APPROVED_MESSAGE;
        }
      }

      // Phase 4: execute
      log.info("Running step: {}", def.name());
      final StepResult result = withAutoApprovalNote(def.run(ctx), autoApprovalMessage);
      // Consume flag after execute so sub-pipelines inside execute can propagate it
      ctx.remove(ContextKey.STEP_APPROVED);
      final List<ContextDiffEntry> diff = computeDiff(before, ctx.snapshot());
      records.add(new StepExecutionRecord(def.name(), result, diff));

      if (result.status() == StepStatus.FAILURE) {
        log.error("Step {} failed — aborting pipeline", def.name());
        return ProcessReport.skipped(records);
      }

      if (result.status() == StepStatus.PENDING_APPROVAL) {
        log.info("Step {} returned pending approval from execute — pausing pipeline", def.name());
        return ProcessReport.pendingApproval(records);
      }
    }

    return ProcessReport.completed(records);
  }

  /**
   * Folds the auto-approval note into the executed step's own result, so the step trail shows both why the gate
   * was passed and what the step then did — without inserting a synthetic extra step, which would shift the step
   * indices that a later resume is addressed by.
   *
   * @param result the step's own result
   * @param autoApprovalMessage the note, or {@code null} when the step was not gated
   * @return the result to record
   */
  private static StepResult withAutoApprovalNote(final StepResult result, final String autoApprovalMessage) {
    if (autoApprovalMessage == null) {
      return result;
    }
    final String message = result.message() == null
        ? autoApprovalMessage
        : autoApprovalMessage + " — " + result.message();
    return new StepResult(StepStatus.WARNING, message, result.issues());
  }

  private static List<ContextDiffEntry> computeDiff(
      final Map<String, String> before, final Map<String, String> after) {
    final List<ContextDiffEntry> diff = new ArrayList<>();
    for (final Map.Entry<String, String> e : after.entrySet()) {
      if (!before.containsKey(e.getKey())) {
        diff.add(new ContextDiffEntry(e.getKey(), "ADDED", null, e.getValue()));
      } else if (!Objects.equals(before.get(e.getKey()), e.getValue())) {
        diff.add(new ContextDiffEntry(e.getKey(), "CHANGED", before.get(e.getKey()), e.getValue()));
      }
    }
    for (final String key : before.keySet()) {
      if (!after.containsKey(key)) {
        diff.add(new ContextDiffEntry(key, "REMOVED", before.get(key), null));
      }
    }
    return diff;
  }
}
