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
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a registration request visible to an operator.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "RegistrationRequest")
public class RegistrationRequestDto {

  @Schema(description = "JoinId - Id for the join flow to register on", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID joinId;

  @Schema(description = "Entity identifier of the applicant", example = "https://example.com/entity")
  private String entityId;

  @Schema(description = "Trustmarks requested in the application")
  private List<String> trustmarksRequested;

}