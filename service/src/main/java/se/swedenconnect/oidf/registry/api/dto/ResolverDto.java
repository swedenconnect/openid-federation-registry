/*
 * Copyright 2025 Sweden Connect
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
package se.swedenconnect.oidf.registry.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

/**
 * Output DTO for Resolver module.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "Resolver")
public class ResolverDto {

  @Schema(description = "Module ID")
  private UUID moduleId;

  @Schema(description = "Entity identifier for this resolver")
  private String entityId;

  @Schema(description = "If this resolver instance is active")
  private Boolean active;

  @Schema(description = "Response duration (ISO-8601 duration, e.g. PT1H)")
  private String resolveResponseDuration;

  @Schema(description = "Trust anchor entity identifier")
  private String trustAnchor;

  @Schema(description = "Trusted keys as JWKS JSON")
  private String trustedKeys;

  @Schema(description = "Step retry duration (ISO-8601 duration, e.g. PT1H)")
  private String stepRetryDuration;
}

