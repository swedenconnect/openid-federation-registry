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

import java.util.List;

/**
 * Incoming DTO for Trust Anchor module.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustAnchorInput")
public class TrustAnchorInputDto {

  @Schema(description = "Entity identifier for this trust anchor")
  private String entityId;

  @Schema(description = "If this trust anchor is active")
  private Boolean active;

  @Schema(description = "Entity identifiers for trust mark issuers")
  private List<String> trustMarkIssuers;
}


