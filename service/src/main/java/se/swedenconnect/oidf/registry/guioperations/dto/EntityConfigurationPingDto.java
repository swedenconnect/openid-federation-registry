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
package se.swedenconnect.oidf.registry.guioperations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO that enable
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "EntityConfigurationPingDto")
public class EntityConfigurationPingDto {

  @Schema(description = "Is EntityConfiguration Accessible", example = "true/false")
  private boolean isEntityConfigurationAccessible;

  @Schema(description = "Error message explaining way entityconfiguration was not accessable",
      example = "Remote server reply 404 NotFound")
  private String errorMessage;

}


