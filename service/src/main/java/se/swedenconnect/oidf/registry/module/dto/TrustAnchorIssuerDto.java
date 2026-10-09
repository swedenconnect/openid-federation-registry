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
 * A trust mark issuer of a trust anchor. It is exported as trust_mark_issuers, which maps trust mark types to the
 * issuers that are trusted to issue them.
 *
 * @author Per Fredrik Plars
 */
@Data
@Schema(name = "TrustAnchorIssuer",
    description = "A trust mark issuer of a trust anchor. With auto set, all trust marks of the issuer are included "
        + "as they are when the configuration is fetched, and the issuer has to be a trust mark issuer of the same "
        + "organization. Otherwise the trust mark types are listed in trustMarkTypes, and the issuer can be any "
        + "entity.")
public class TrustAnchorIssuerDto {

  @Schema(description = "Entity identifier of the trust mark issuer", example = "https://tmi.example.se")
  private String issuer;

  @Schema(description = "Include every trust mark of the issuer automatically, trustMarkTypes has to be empty.",
      defaultValue = "false")
  private boolean auto;

  @Schema(description = "The trust mark types that the issuer is trusted to issue. Required, and at least one, "
      + "unless auto is set.", example = "[\"https://ta.example.se/trustmarks/certified-op\"]")
  private List<String> trustMarkTypes = new ArrayList<>();
}
