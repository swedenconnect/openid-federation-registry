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

/**
 * Incoming DTO for trust mark subject configuration.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustmarkSubjectInput")
public class TrustmarkSubjectInputDto {

  @Schema(description = "Trust mark identifier (entity id)")
  private String trustmarkId;

  @Schema(description = "Subject entity id")
  private String subject;

  @Schema(description = "If the trust mark is revoked for this subject")
  private Boolean revoked;

  @Schema(description = "Granted time (ISO-8601, e.g. 2025-01-01T00:00:00Z)")
  private String granted;

  @Schema(description = "Expires time (ISO-8601)")
  private String expires;
}


