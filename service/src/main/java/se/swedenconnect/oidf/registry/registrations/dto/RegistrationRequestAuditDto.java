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

import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.util.UUID;

/**
 * A registration request as it is written to the audit log. The request's metadata and the results of its steps are
 * left out: they are audited as the records they produce, and are too large for an audit record.
 *
 * @param registrationId the registration the request belongs to
 * @param entityId the entity identifier, or the trust mark type for a trust mark registration
 * @param registrationType the kind of registration
 * @param status the registration's status after the request
 * @param joinId the assignment of the flow the request was submitted to
 * @param submittedBy the account that submitted the request
 * @author Per Fredrik Plars
 */
public record RegistrationRequestAuditDto(
    UUID registrationId,
    String entityId,
    RegistrationType registrationType,
    RegistrationStatus status,
    UUID joinId,
    String submittedBy
) {
}
