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

import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationReviewDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Declines a registration the registry cannot take further without a person, such as one whose automated step failed,
 * so it does not wait for a review that never comes.
 *
 * <p>The decline is audited as {@code REGISTRATION_REJECTED}, as an operator's rejection is, but with the registry as
 * the principal and no reviewer: no user made the decision. The reason says it was automatic.</p>
 *
 * @author Felix Hellman
 */
@Component
public class AutomaticDecline {

  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param auditService audit service the decline is reported to
   */
  public AutomaticDecline(final RegistryAuditService auditService) {
    this.auditService = auditService;
  }

  /**
   * Declines the registration and audits it. The caller saves the registration.
   *
   * @param registration the registration to decline
   * @param reason why it is declined, shown to the member
   */
  public void decline(final Registration registration, final String reason) {
    final LocalDateTime decidedAt = LocalDateTime.now();
    registration.setStatus(RegistrationStatus.REJECTED);
    registration.setRejectionReason(reason);
    registration.setReviewedAt(decidedAt);
    registration.setReviewedBy(null);
    registration.setPendingStepIndex(null);
    registration.setProcessContext(null);
    if (registration.getOrganization() == null) {
      return;
    }
    this.auditService.registrationDeclinedAutomatically(registration.getRegistrationId(),
        Optional.ofNullable(registration.getOrganization().getInstance()).map(Instance::getInstanceId).orElse(null),
        registration.getOrganization().getOrganizationId(),
        new RegistrationReviewDto(registration.getRegistrationId(), registration.getEntityId(),
            registration.getRegistrationType(), null, null, null, decidedAt, reason));
  }
}
