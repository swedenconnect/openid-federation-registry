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
package se.swedenconnect.oidf.registry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for Trustmark Issuer. Used for both input (create/update) and output (get). The trustmarkIssuerId field is
 * read-only and will be ignored when deserializing from JSON input.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustmarkIssuer")
public class TrustmarkIssuerDto {

  @Schema(description = "Trustmark issuer ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID trustmarkIssuerId;

  @Schema(description = "Entity identifier that this trustmark issuer belongs to")
  @NotNull
  private UUID entityId;

  @Schema(description = "If this trustmark issuer is active")
  @NotNull
  private Boolean active;

  @Schema(description = "Trust mark token validity duration (ISO-8601 duration, e.g. PT1H)")
  @NotNull
  private String trustMarkTokenValidityDuration;
}
