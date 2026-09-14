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
 * An organization's registry record, as seen by that organization.
 *
 * @param orgNumber the organization number
 * @param orgName the organization name taken from the caller's token claim, may be {@code null}
 * @param legalName the legal name posted at bootstrap, may be {@code null}
 * @param tenant the tenant slug the organization is placed on
 * @param domains the organization's domains, in every status
 * @param preValidatedTrustMarks trust mark types the tenant operator has pre-approved for the organization
 * @author Felix Hellman
 */
@Schema(name = "Organization", description = "An organization's registry record")
public record OrganizationDto(

    @Schema(description = "Organization number", example = "5520012229")
    String orgNumber,

    @Schema(description = "Organization name from the token claim, may be null", example = "TestOrg1")
    String orgName,

    @Schema(description = "Legal name posted at bootstrap, may be null", example = "TestOrg1 AB")
    String legalName,

    @Schema(description = "Tenant slug the organization is placed on", example = "swedenconnect")
    String tenant,

    @Schema(description = "The organization's domains, in every status")
    List<DomainDto> domains,

    @Schema(description = "Trust mark types pre-approved for this organization by the tenant operator")
    List<String> preValidatedTrustMarks
) {
}
