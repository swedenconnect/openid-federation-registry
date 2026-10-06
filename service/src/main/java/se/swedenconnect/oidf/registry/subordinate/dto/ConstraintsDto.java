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

package se.swedenconnect.oidf.registry.subordinate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Constraints for a subordinate statement, see OpenID Federation 1.0 section 6.2. Everything is optional.
 *
 * @author Per Fredrik Plars
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Constraints",
    description = "Constraints for a subordinate statement, see OpenID Federation 1.0 section 6.2. "
        + "Everything is optional.")
public class ConstraintsDto {

  @JsonProperty("max_path_length")
  @Schema(description = "Maximum number of intermediate entities between the subordinate and a leaf. "
      + "A non-negative integer.", example = "2", minimum = "0")
  private Integer maxPathLength;

  @JsonProperty("naming_constraints")
  @Schema(description = "Constraints on the entity identifiers below the subordinate.")
  private NamingConstraintsDto namingConstraints;

  @JsonProperty("allowed_entity_types")
  @Schema(description = "Entity types that are allowed below the subordinate. Strings that are not empty.",
      example = "[\"openid_provider\", \"openid_relying_party\"]")
  private List<String> allowedEntityTypes;
}
