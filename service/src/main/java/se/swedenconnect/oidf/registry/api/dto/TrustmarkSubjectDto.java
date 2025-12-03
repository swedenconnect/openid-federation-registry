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
package se.swedenconnect.oidf.registry.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
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

  @Schema(description = "Trust mark identifier (entity id)")
  private String trustmarkId;

  @Schema(description = "Subject entity id")
  private String subject;

  @Schema(description = "If the trust mark is revoked for this subject")
  private Boolean revoked;

  @Schema(description = "Granted time (ISO-8601, e.g. 2025-01-01T00:00:00)")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime granted;

  @Schema(description = "Expires time (ISO-8601, e.g. 2025-01-01T00:00:00)")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime expires;
}

