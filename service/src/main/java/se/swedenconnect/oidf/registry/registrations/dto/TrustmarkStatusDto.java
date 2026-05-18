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

/**
 * Status for a single trustmark on a registered entity.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustmarkStatus")
public class TrustmarkStatusDto {
  /**
   * Constructor
   * @param trustmarkType trustmarkType
   * @param trustmarkStatus trustmarkStatus
   */
  public TrustmarkStatusDto(final String trustmarkType, final FedRegStatus trustmarkStatus) {
    this.trustmarkType = trustmarkType;
    this.status = trustmarkStatus;
  }
  @Schema(description = "Trustmark type identifier (URI)", example = "https://trust.example.com/tm/loa3")
  private String trustmarkType;

  @Schema(description = "Current status of this trustmark")
  private FedRegStatus status;


}
