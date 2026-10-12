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
package se.swedenconnect.oidf.registry.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * A trust mark type of a trust anchor with the issuers outside the organization that are trusted to issue it. It is
 * one member of trust_mark_issuers, with the trust mark type as the key.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustAnchorExternalTrustMark",
    description = "A trust mark type with the issuers outside the organization that are trusted to issue it. With "
        + "allowAll set anyone may issue trust marks of the type, and it is exported with an empty list of issuers.")
public class TrustAnchorExternalTrustMarkDto {

  @Schema(description = "The trust mark type identifier", example = "https://ta.example.se/trustmarks/certified-op")
  private String trustMarkType;

  @Schema(description = "Anyone may issue trust marks of the type. issuers has to be empty.", defaultValue = "false")
  private boolean allowAll;

  @Schema(description = "Entity identifiers of the issuers that are trusted to issue the type. Required, and at least "
      + "one, unless allowAll is set.", example = "[\"https://tmi.example.org\"]")
  private List<String> issuers = new ArrayList<>();
}
