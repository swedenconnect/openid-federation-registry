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

import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An operator decision on a registration, as recorded in the audit log.
 *
 * @param registrationId the registration decided on
 * @param entityId the registration's entity identifier, or trust mark type for a trust mark registration
 * @param registrationType the kind of registration
 * @param stepIndex index of the approved step, {@code null} for a rejection
 * @param stepName name of the approved step, {@code null} for a rejection
 * @param reviewer who made the decision
 * @param reviewedAt when the decision was made
 * @param rejectionReason why the registration was rejected, {@code null} for an approval
 * @author Felix Hellman
 */
public record RegistrationReviewDto(
    UUID registrationId,
    String entityId,
    RegistrationType registrationType,
    Integer stepIndex,
    String stepName,
    String reviewer,
    LocalDateTime reviewedAt,
    String rejectionReason) {
}
