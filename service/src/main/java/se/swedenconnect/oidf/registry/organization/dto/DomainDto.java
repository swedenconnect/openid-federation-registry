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
package se.swedenconnect.oidf.registry.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A domain claimed by an organization, as seen by that organization.
 *
 * @param domainId the domain's identifier
 * @param domain the bare, lower-cased hostname
 * @param status the review state of the domain
 * @param rejectionReason the operator's reason for rejecting the domain, {@code null} unless rejected
 * @param createdDate when the domain was first requested
 * @param reviewedAt when the domain was approved or rejected, {@code null} while pending
 * @author Felix Hellman
 */
@Schema(name = "Domain", description = "A domain claimed by an organization")
public record DomainDto(

    @Schema(description = "Domain identifier", example = "6f1b4f7e-1f4c-4e3f-8c7f-2d43b0f6a0f1")
    UUID domainId,

    @Schema(description = "Bare, lower-cased hostname", example = "example.com")
    String domain,

    @Schema(description = "Review state of the domain")
    DomainStatus status,

    @Schema(description = "Operator's reason for rejecting the domain, null unless rejected")
    String rejectionReason,

    @Schema(description = "When the domain was first requested")
    OffsetDateTime createdDate,

    @Schema(description = "When the domain was approved or rejected, null while pending")
    OffsetDateTime reviewedAt
) {
}
