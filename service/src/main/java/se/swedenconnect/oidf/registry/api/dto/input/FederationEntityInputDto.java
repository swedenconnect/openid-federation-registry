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

package se.swedenconnect.oidf.registry.api.dto.input;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * Incoming DTO for creating or updating a Federation Entity.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "FederationEntityInput")
public class FederationEntityInputDto {

  @Schema(description = "Subject (entity identifier)", example = "https://rp.example.se")
  private String subject;

  @Schema(description = "Issuer (entity identifier)", example = "https://ta.example.se")
  private String issuer;

  @Schema(description = "Federation entity metadata", example = "{\"federation_entity\":{}}")
  private Map<String, Object> metadata;
}


