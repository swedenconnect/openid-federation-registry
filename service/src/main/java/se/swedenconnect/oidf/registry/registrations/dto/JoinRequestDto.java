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

import java.util.List;
import java.util.UUID;

/**
 * Request body for applying to join the federation.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "JoinRequest")
public class JoinRequestDto {

  @Schema(description = "Entity identifier (URI) of the entity applying to join",
      example = "https://example.com/entity")
  private String entityId;

  @Schema(description = "ID of the registration flow (linked to an intermediate)",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID registrationFlowId;

  @Schema(description = "Trustmarks requested as part of this join application")
  private List<TrustmarkRequestDto> trustmarks;
}
