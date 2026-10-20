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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * What the published subordinate statement of a self-managed entity pins. The entity re-publishes a change to any of
 * it by running its registration again, which then pauses for review; a change to anything else needs no new
 * subordinate statement.
 *
 * @param parameters the pinned metadata parameters
 * @param jwks the entity's federation keys, as pinned by the subordinate statement's {@code jwks} claim
 * @param reviewOnUpdate whether a resubmission that changes no pinned value or key is also reviewed, because a step of
 *     the registration flow sets {@code manualreviewforupdate}
 * @author Felix Hellman
 */
@Schema(name = "PinnedMetadata")
public record PinnedMetadataDto(

    @Schema(description = "Metadata parameters pinned by the subordinate statement's metadata claim or by the value "
        + "operator of its metadata_policy claim")
    List<PinnedParameterDto> parameters,

    @Schema(description = "The entity's federation keys (JWK set) pinned by the subordinate statement's jwks claim")
    Map<String, Object> jwks,

    @Schema(description = "True if a resubmission that changes no pinned value or key is also reviewed by an operator; "
        + "otherwise it is published directly")
    boolean reviewOnUpdate
) {
}
