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
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MID step for trust mark enrollment flows.
 * <p>
 * Lifecycle:
 * <ul>
 *   <li>{@code buildContext} — validates that every requested trust mark type exists in the
 *       registry. Aborts early (FAILURE) if any type is missing.</li>
 *   <li>Approval gate — halts when {@code manualreview=true}.</li>
 *   <li>{@code execute} — sets {@link ContextKey#TRUSTMARK_SUBJECT_PROCEED} so that the
 *       downstream POST step ({@link CreateTrustMarkSubjectStep}) knows it is safe to create
 *       the subject. Without this signal the POST step is a no-op.</li>
 * </ul>
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class AddTrustMarkSubjectStep extends NoConfigStepAdapter {

  private final TrustMarkRepository trustMarkRepository;

  /**
   * Constructor.
   *
   * @param trustMarkRepository repository for resolving trust marks
   */
  public AddTrustMarkSubjectStep(final TrustMarkRepository trustMarkRepository) {
    this.trustMarkRepository = trustMarkRepository;
  }

  @Override
  public StepType stepType() {
    return StepType.MID;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public FlowType flowType() {
    return FlowType.TRUST_MARK_ISSUER;
  }

  @Override
  public StepResult buildContext(final ProcessContext ctx, final StepConfig config) {
    final var requested = ctx.<SerializableList<TrustmarkSource>>get(ContextKey.TRUSTMARKS_REQUESTED);
    if (requested.isEmpty() || requested.get().isEmpty()) {
      return StepResult.success("No trust marks requested");
    }

    final List<StepIssue> issues = new ArrayList<>();
    for (final TrustmarkSource source : requested.get()) {
      for (final TrustmarkSource.TrustMarkStatus status : source.trustmarks()) {
        final boolean exists = this.trustMarkRepository
            .findByIssuerEntityIdAndTrustmarkType(source.trustMarkIssuer(), status.trustmarkType())
            .isPresent();
        if (!exists) {
          log.warn("AddTrustMarkSubjectStep.buildContext: trust mark not found: {}/{}",
              source.trustMarkIssuer(), status.trustmarkType());
          issues.add(new StepIssue("trustmarkType",
              "Trust mark not found: " + status.trustmarkType(), Severity.ERROR));
        }
      }
    }
    if (!issues.isEmpty()) {
      return StepResult.failure("One or more trust mark types not found in registry", issues);
    }
    return StepResult.success("Trust mark types validated");
  }

  /**
   * Sets {@link ContextKey#TRUSTMARK_SUBJECT_PROCEED} to signal the POST step to create the subject.
   * Only reached when the approval gate has been passed (or skipped).
   */
  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    ctx.put(ContextKey.TRUSTMARK_SUBJECT_PROCEED, Boolean.TRUE);
    return StepResult.success("Enrollment approved — subject creation signalled");
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("3F8A1C2D-7E4B-4F9A-B5D6-8C0E2A3F1B4D");
  }

  @Override
  public String getDescription() {
    return "Validates requested trust mark types. "
        + "When manualreview=true the pipeline pauses until an admin approves. "
        + "After approval the downstream POST step creates the trust mark subject.";
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(new StepConfigurationValue("manualreview",
        StepConfigurationValue.DATA_TYPE.BOOLEAN,
        "If true, enrollment requires manual approval before the subject is created", "false"));
  }
}
