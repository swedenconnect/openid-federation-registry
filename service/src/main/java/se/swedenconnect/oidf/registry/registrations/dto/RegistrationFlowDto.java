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
import lombok.Data;

import java.util.UUID;

/**
 * A registration flow available for joining the federation.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "RegistrationFlow")
public class RegistrationFlowDto {

  @Schema(description = "ID to be used when making a join request to this registrationflow",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID joinId;

  @Schema(description = "Intermidiate EntityID", example = "https://fed.swedenconnect.se/im")
  private String intermidiateEntityId;

  @Schema(description = "Human-readable name of the flow", example = "Sweden Connect QA")
  private String name;

  @Schema(description = "Description of the flow and its purpose",
      example = "QA-miljö för Sweden Connect")
  private String description;
}
