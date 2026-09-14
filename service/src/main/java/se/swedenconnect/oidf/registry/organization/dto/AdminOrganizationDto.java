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

import java.util.List;

/**
 * An organization as seen by the tenant operator administering it: the identity of the organization, how many
 * domains it has claimed and how many of those are still awaiting review, and the trust mark types it is
 * pre-approved for. The domains themselves are not carried here — the operator fetches a single organization to
 * see those, so a tenant-wide listing stays one row per organization.
 *
 * @param orgNumber the organization number
 * @param orgName the organization name taken from the caller's token claim, may be {@code null}
 * @param legalName the legal name posted at bootstrap, may be {@code null}
 * @param domainCount the number of domains the organization has claimed, in every status
 * @param pendingDomainCount how many of those domains are still awaiting review
 * @param preValidatedTrustMarks trust mark types the tenant operator has pre-approved for the organization
 * @author Felix Hellman
 */
@Schema(name = "AdminOrganization", description = "An organization as seen by the tenant operator")
public record AdminOrganizationDto(

    @Schema(description = "Organization number", example = "5520012229")
    String orgNumber,

    @Schema(description = "Organization name from the token claim, may be null", example = "TestOrg1")
    String orgName,

    @Schema(description = "Legal name posted at bootstrap, may be null", example = "TestOrg1 AB")
    String legalName,

    @Schema(description = "Number of domains claimed by the organization, in every status", example = "3")
    long domainCount,

    @Schema(description = "Number of the organization's domains still awaiting review", example = "1")
    long pendingDomainCount,

    @Schema(description = "Trust mark types pre-approved for this organization by the tenant operator")
    List<String> preValidatedTrustMarks
) {
}
