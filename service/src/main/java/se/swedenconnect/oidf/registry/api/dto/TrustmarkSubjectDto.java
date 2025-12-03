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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Output DTO for trust mark subject configuration.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustmarkSubject")
public class TrustmarkSubjectDto {

  @Schema(description = "Trustmark subject ID")
  private UUID trustmarksubjectId;

  @Schema(description = "Trust mark identifier (entity id)")
  private String trustmarkId;

  @Schema(description = "Subject entity id")
  private String subject;

  @Schema(description = "If the trust mark is revoked for this subject")
  private Boolean revoked;

  @Schema(description = "Granted time")
  private LocalDateTime granted;

  @Schema(description = "Expires time")
  private LocalDateTime expires;
}

