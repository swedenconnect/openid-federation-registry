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
package se.swedenconnect.oidf.registry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Hosted Entity. Used for both input (create/update) and output (get).
 * The entityId field is read-only and will be ignored when deserializing from JSON input.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "HostedEntity")
public class HostedEntityDto {

  @Schema(description = "Entity ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID entityId;

  @Schema(description = "EntityIdentifier")
  private String entityIdentifier;

  @Schema(description = "FederationMetadata")
  private Map<String, Object> metadata;

  @Schema(description = "Effective EcLocation that is calculated serverside")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String effectiveEcLocation;

  @Schema(description = "Trustmark sources that can be used to include trustmarks.")
  private List<TrustmarkSourceDto> trustMarkSources;

  @Schema(description = "crit ", example = "The crit (critical) Claim indicates that extensions to "
      + "the set of Claims specified for use in this type of JWT")
  private List<String> crit;

  @Schema(description = "authorityhints", example = "An array of strings representing the Entity Identifiers of "
      + "Intermediate Entities or Trust Anchors that are Immediate Superiors of the Entity. "
      + "the set of Claims specified for use in this type of JWT")
  private List<String> authorityhints;


}

