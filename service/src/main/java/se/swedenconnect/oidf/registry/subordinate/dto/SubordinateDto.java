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

package se.swedenconnect.oidf.registry.subordinate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Subordinate. Used for both input (create/update) and output (get). The subordinateId field is read-only and
 * will be ignored when deserializing from JSON input.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "Subordinate")
public class SubordinateDto {

  @Schema(description = "Subordinate ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID subordinateId;

  @Schema(description = "TaIm ID that this subordinate belongs to")
  private UUID taImId;

  @Schema(description = "Policy ID", example = "Optional policy reference")
  private UUID policyId;

  @Schema(description = "JWKSet for Subordinate", example = "JSON Web Key Set as string")
  private String jwks;

  @Schema(description = "Subject (entity identifier)", example = "https://subordinate.example.se")
  private String entityIdentifier;

  @Schema(description = "List of crit claims", example = "[\"claim1\", \"claim2\"]")
  private List<String> crit;

  @Schema(description = "List of metadata_policy_crit", example = "[\"operator1\", \"operator2\"]")
  private List<String> metadataPolicyCrit;

  @Schema(description = "Policy", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Map<String, Object> policy;

  @Schema(description = "ecLocation - location where the actual entity statement is placed. Expressed as a url. "
      + "Ex https://my.company.se/entitystatement")
  private String ecLocation;

  @Schema(description = "When true, eclocation will be loaded from the hosted entity with the same issuer entityid")
  private Boolean ecLocationAutomaticResolve;

  @Schema(description = "Effective EcLocation that is calculated serverside")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String effectiveEcLocation;
}
