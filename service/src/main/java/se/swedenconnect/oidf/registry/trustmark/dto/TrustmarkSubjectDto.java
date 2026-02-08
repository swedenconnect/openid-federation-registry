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
package se.swedenconnect.oidf.registry.trustmark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for trust mark subject configuration. Used for both input (create/update) and output (get).
 * The trustmarksubjectId field is read-only and will be ignored when deserializing from JSON input.
 * The granted and expires fields accept ISO-8601 datetime strings in input and return LocalDateTime in output.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustmarkSubject")
public class TrustmarkSubjectDto {

  @Schema(description = "Trustmark subject ID", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID trustmarksubjectId;

  @Schema(description = "Trustmarkid that this subject belongs to")
  private UUID trustmarkId;

  @Schema(description = "Subject entity id")
  private String subject;

  @Schema(description = "If the trust mark is revoked for this subject")
  private Boolean revoked;

  @Schema(
      description = "Granted time in ISO-8601-format with offset, ex. 2025-03-12T14:37:00Z",
      example = "2025-03-12T14:37:00Z",
      format = "date-time"
  )
  private OffsetDateTime granted;

  @Schema(
      description = "Expires time in ISO-8601-format with offset, ex. 2025-03-12T14:37:00Z",
      example = "2025-03-12T14:37:00Z",
      format = "date-time"
  )
  private OffsetDateTime expires;
}
