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

import java.util.Map;
import java.util.UUID;

/**
 * Request body for creating a hosted entity.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "HostedRequest")
public class HostedRequestDto {

  @Schema(description = "Hosted entity ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID hostedId;

  @Schema(description = "Entity identifier (URI) of the entity to host",
      example = "https://example.com/entity")
  private String entityId;

  @Schema(description = "Metadata in OIDF format, keyed by metadata type",
      example = "{\"openid_relying_party\": {}}")
  private Map<String, Object> metadata;
}
