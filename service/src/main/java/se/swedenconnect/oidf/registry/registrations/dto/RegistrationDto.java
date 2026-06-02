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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a registered joiner with federation and trustmark status.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Data
@Schema(name = "Registration")
public class RegistrationDto {

  @Schema(description = "Id of this specific registration",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID registrationId;

  @Schema(description = "Join ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID joinId;

  @Schema(description = "EntityId for the requested registration",
      example = "https://example.com/service",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String entityIdentifier;

  @Schema(description = "EntityId of the Intermediate that the registration is made on",
      example = "https://example.com/entity",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String intermediateEntityId;

  @Schema(description = "True if the entity's metadata is hosted in this registry",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Boolean isHosted;

  @Schema(description = "Hosted metadata, present when isHosted is true",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Map<String, Object> metadata;

  @Schema(description = "Registration type: SUBORDINATE or TRUST_MARK_SUBORDINATE",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private RegistrationType registrationType;

  @Schema(description = "Current federation registration status",
      accessMode = Schema.AccessMode.READ_ONLY)
  private FedRegStatus statusFedreg;

  @Schema(description = "Reason for rejection, present when status_fedreg is DENY",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String rejectionReason;

  @Schema(description = "Trustmark status per requested trustmark",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private List<TrustmarkRegistrationDto> statusTrustmarks;

  @Schema(description = "Tags describing the entity type, e.g. OIDC, SAML, RP, OP, IDP, SP",
      example = "[\"OIDC\", \"RP\"]",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private List<RegistrationTagsDto> tags;

  @Schema(description = "Name of the organization that submitted the registration",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String organizationName;

  @Schema(description = "Entity ID of the subordinate subject, populated for TRUST_MARK_SUBORDINATE registrations",
      accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String subordinateEntityId;

  @Schema(description = "True if the pipeline completed without any step failures")
  private boolean successful;

  @Schema(description = "Ordered list of step execution records")
  private List<StepExecutionRecordDto> steps;


}
