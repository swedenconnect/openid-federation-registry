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
 * Request body for replacing an organization's pre-validated trust mark types. The list is replaced wholesale:
 * types absent from it are removed.
 *
 * @param preValidatedTrustMarks the trust mark types to pre-approve for the organization
 * @author Felix Hellman
 */
@Schema(name = "PreValidatedTrustMarksRequest",
    description = "Replaces the pre-validated trust mark types of an organization")
public record PreValidatedTrustMarksDto(

    @Schema(description = "Trust mark types to pre-approve for the organization",
        example = "[\"https://tm.example.com/tm/x\"]")
    List<String> preValidatedTrustMarks
) {
}
