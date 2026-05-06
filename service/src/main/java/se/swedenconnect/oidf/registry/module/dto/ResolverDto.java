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

import java.util.Map;
import java.util.UUID;

/**
 * DTO for Resolver. Used for both input (create/update) and output (get).
 * The resolverId field is read-only and will be ignored when deserializing from JSON input.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "Resolver")
public class ResolverDto {

  @Schema(description = "Resolver ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID resolverId;

  @Schema(description = "Entity identifier that this module belongs to")
  private UUID entityId;

  @Schema(description = "If this resolver instance is active")
  private Boolean active;

  @Schema(description = "Response duration (ISO-8601 duration, e.g. PT1H)")
  private String resolveResponseDuration;

  @Schema(description = "Trust anchor entityId that this resolver is connected to")
  private String trustAnchor;

  @Schema(description = "Trusted keys as JWKS JSON of the trust anchor")
  private Map<String,Object> trustedKeys;

  @Schema(description = "Step retry duration (ISO-8601 duration, e.g. PT1H)")
  private String stepRetryDuration;

  @Schema(description = "Step Cached Value Threshold")
  private Integer stepCachedValueThreshold;
}
