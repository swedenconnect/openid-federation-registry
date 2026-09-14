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

/**
 * Request body for bootstrapping or updating an organization's registry record. The organization number and
 * tenant come from the request path, and the organization name from the caller's token claim — only the legal
 * name is supplied by the portal.
 *
 * @param legalName the organization's legal name, required, 1–255 characters
 * @author Felix Hellman
 */
@Schema(name = "CreateOrganizationRequest", description = "Organization data posted by the portal")
public record CreateOrganizationDto(

    @Schema(description = "The organization's legal name", example = "TestOrg1 AB")
    String legalName
) {
}
