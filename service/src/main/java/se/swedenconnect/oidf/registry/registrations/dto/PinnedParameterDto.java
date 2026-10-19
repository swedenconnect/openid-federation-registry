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

/**
 * A metadata parameter the published subordinate statement pins.
 *
 * @param entityType the entity type the parameter belongs to
 * @param parameter the parameter, with language tag if it has one
 * @param value the pinned value
 * @param claim the claim that pins it: {@code metadata} (set) or {@code metadata_policy} ({@code value} operator)
 * @param source where the value comes from: {@code entity_configuration} or {@code registry}
 * @param entityParameters the parameters of the entity configuration the value is taken from, the first one the
 *     entity declares being used; empty when the registry sets the value
 * @author Felix Hellman
 */
@Schema(name = "PinnedParameter")
public record PinnedParameterDto(

    @Schema(description = "Entity type the parameter belongs to", example = "openid_relying_party")
    String entityType,

    @Schema(description = "Metadata parameter, with language tag if it has one", example = "logo_uri")
    String parameter,

    @Schema(description = "The pinned value")
    Object value,

    @Schema(description = "Claim of the subordinate statement that pins the value: metadata (set by the claim) or "
        + "metadata_policy (the value operator)", example = "metadata_policy")
    String claim,

    @Schema(description = "Where the value comes from: entity_configuration (the entity's own metadata, so a change "
        + "there changes the pinned value) or registry (the registry's organization data or a fixed value)",
        example = "entity_configuration")
    String source,

    @Schema(description = "Parameters of the entity configuration the value is taken from, the first one the entity "
        + "declares being used; empty when the source is registry", example = "[\"logo_uri\"]")
    List<String> entityParameters
) {

  /** The value comes from the entity's own metadata. */
  public static final String ENTITY_CONFIGURATION = "entity_configuration";

  /** The registry sets the value. */
  public static final String REGISTRY = "registry";
}
