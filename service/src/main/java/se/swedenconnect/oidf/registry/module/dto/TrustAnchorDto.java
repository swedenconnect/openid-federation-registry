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
package se.swedenconnect.oidf.registry.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for Trust Anchor module. Used for both input (create/update) and output (get).
 * The taImId field is read-only and will be ignored when deserializing from JSON input.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustAnchor")
public class TrustAnchorDto {

  @Schema(description = "TaIm ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID trustAnchorId;

  @Schema(description = "Entity identifier that this trust anchor belongs to. ")
  private UUID entityId;

  @Schema(description = "If this trust anchor is active")
  private Boolean active;

  @Schema(description = "Entity identifiers for trust mark issuers")
  private List<String> trustMarkIssuers;

  @Schema(description = "List of subordinates for this trust anchor")
  private List<SubordinateDto> subordinates;

}
