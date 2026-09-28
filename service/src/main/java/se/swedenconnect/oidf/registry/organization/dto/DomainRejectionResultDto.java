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
import java.util.List;
import java.util.UUID;

/**
 * Result of rejecting a domain: the updated {@link AdminDomainDto} fields, flattened, plus the registrations
 * the rejection cascaded to. The fields are flattened rather than nested so the response stays a superset of
 * {@code AdminDomainDto} for consumers that only care about the domain.
 *
 * @param domainId the domain's identifier
 * @param domain the bare, lower-cased hostname
 * @param status the review state of the domain, always {@link DomainStatus#REJECTED} here
 * @param rejectionReason the operator's reason for rejecting the domain
 * @param createdDate when the domain was first requested
 * @param reviewedAt when the domain was rejected
 * @param orgNumber the owning organization's number
 * @param orgName the owning organization's name from its token claim, may be {@code null}
 * @param legalName the owning organization's legal name, may be {@code null}
 * @param cascadedRegistrationIds identifiers of the registrations rejected as a consequence of this rejection
 * @author Felix Hellman
 */
@Schema(name = "DomainRejectionResult",
    description = "The rejected domain plus the registrations the rejection cascaded to")
public record DomainRejectionResultDto(

    @Schema(description = "Domain identifier", example = "6f1b4f7e-1f4c-4e3f-8c7f-2d43b0f6a0f1")
    UUID domainId,

    @Schema(description = "Bare, lower-cased hostname", example = "example.com")
    String domain,

    @Schema(description = "Review state of the domain")
    DomainStatus status,

    @Schema(description = "Operator's reason for rejecting the domain")
    String rejectionReason,

    @Schema(description = "When the domain was first requested")
    OffsetDateTime createdDate,

    @Schema(description = "When the domain was rejected")
    OffsetDateTime reviewedAt,

    @Schema(description = "Owning organization's number", example = "5520012229")
    String orgNumber,

    @Schema(description = "Owning organization's name from its token claim", example = "TestOrg1")
    String orgName,

    @Schema(description = "Owning organization's legal name", example = "TestOrg1 AB")
    String legalName,

    @Schema(description = "Registrations rejected as a consequence of this domain rejection")
    List<UUID> cascadedRegistrationIds
) {
}
