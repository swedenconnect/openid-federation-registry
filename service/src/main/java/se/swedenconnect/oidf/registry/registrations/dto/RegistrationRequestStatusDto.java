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

import java.util.List;
import java.util.UUID;

/**
 * Represents a registration request status
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "RegistrationRequestStatus")
public class RegistrationRequestStatusDto {

  @Schema(description = "RegistrationID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID registrationId;

  @Schema(description = "Status on registration", example = "PENDING_APPROVAL")
  private String status;

  @Schema(description = "Entity identifier of the applicant", example = "https://example.com/entity")
  private String entityIdentifyer;

  @Schema(description = "Trustmarks requested in the application")
  private List<String> trustmarksRequested;

  // TODO, some type of status message from the executed steps.
  // EntityStatmentLoader:error - Unable to load entitystatement

}