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

import java.util.Map;

/**
 * Request body for updating metadata of an existing hosted entity.
 * Only metadata can be updated; entity_id is immutable.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "HostedUpdate")
public class HostedUpdateDto {

  @Schema(description = "Updated metadata in OIDF format, keyed by metadata type",
      example = "{\"openid_relying_party\": {}}")
  private Map<String, Object> metadata;
}
