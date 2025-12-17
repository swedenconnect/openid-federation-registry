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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for representing all modules in the module list response. Contains separate lists for each module type.
 *
 * @author Per Fredrik Plars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Modules", description = "All modules grouped by type")
public class ModuleDto {

  @Schema(description = "List of trust anchor modules")
  private List<TrustAnchorDto> trustAnchors = new ArrayList<>();

  @Schema(description = "List of intermediate modules")
  private List<IntermediateDto> intermediates = new ArrayList<>();

  @Schema(description = "List of resolver modules")
  private List<ResolverDto> resolvers = new ArrayList<>();

  @Schema(description = "List of trustmark issuer modules")
  private List<TrustmarkIssuerDto> trustmarkIssuers = new ArrayList<>();
}
