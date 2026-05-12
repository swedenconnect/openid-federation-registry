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

import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.repository.FlowAssignmentRepository;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.util.List;
import java.util.UUID;

/**
 * Pre step that handle the registration state in database.
 *
 * @author Per Fredrik Plars
 */
@Component
public class InternalPreRegistrationStep extends NoConfigStepAdapter {

  final RegistrationRepository registrationRepository;
  final FlowAssignmentRepository flowAssignmentRepository;

  /**
   * Constructor
   *
   * @param registrationRepository repository for registration records
   * @param flowAssignmentRepository repository for flow assignments
   */
  public InternalPreRegistrationStep(final RegistrationRepository registrationRepository,
      final FlowAssignmentRepository flowAssignmentRepository) {
    super();
    this.registrationRepository = registrationRepository;
    this.flowAssignmentRepository = flowAssignmentRepository;
  }

  @Override
  public String getDescription() {
    return """
        Internal pre step, that check if there is an ongoing registration, if not a new one is created. 
        """;
  }

  @Override
  public StepType stepType() {
    return StepType.PRE;
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final UUID joinIdAssignmentId = ctx.getRequired(ContextKey.JOIN_ID);


    final FlowAssignment flow = this.flowAssignmentRepository.findById(joinIdAssignmentId)
        .orElseThrow(() -> new IllegalArgumentException("Join id assignment not found"));

    final Registration registration = this.registrationRepository.findByEntityId(entityId).orElseGet(() -> {
      final Registration newRegistration = new Registration();
      newRegistration.setRegistrationId(UUID.randomUUID());
      newRegistration.setEntityId(entityId);
      newRegistration.setStatus(RegistrationStatus.STARTED);
      newRegistration.setFlowAssignment(flow);
      return newRegistration;
    });

    ctx.<SerializableList<TrustmarkSource>>get(ContextKey.TRUSTMARKS_REQUESTED)
        .ifPresent(registration::setTrustmarksRequested);
    this.registrationRepository.save(registration);

    ctx.put(ContextKey.REGISTRATION_ID, registration.getRegistrationId());
    if(registration.getStatus() == RegistrationStatus.PENDING_APPROVAL) {
      return StepResult.failure("Registration is waiting for approval",
          List.of(new StepIssue("%s.status".formatted(this.getClass().getSimpleName()),
              "There is a pending_approval in progress",
              Severity.ERROR)));
    }



    return StepResult.success("Registration was created");
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("79D24184-555D-4906-AF13-D1076CEC05D5");
  }

}
