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
package se.swedenconnect.oidf.registry.module.model;

import java.util.List;

/**
 * A trust mark issuer of a trust anchor, stored as JSON in the trust_mark_issuers column of the module. It is what the
 * trust anchor exports as trust_mark_issuers: trust mark types and the issuers that are trusted to issue them.
 * <p>
 * With {@code auto} set, the issuer is a trust mark issuer of the same organization and every trust mark of it is
 * included as it is when the configuration is fetched, so {@code trustMarkTypes} is empty. Otherwise the trust mark
 * types are listed, and the issuer can be any entity.
 *
 * @param issuer entity identifier of the trust mark issuer
 * @param auto whether every trust mark of the issuer is included automatically
 * @param trustMarkTypes the trust mark types that the issuer is trusted to issue, empty with auto
 * @author Per Fredrik Plars
 */
public record TrustAnchorIssuer(String issuer, boolean auto, List<String> trustMarkTypes) {

  /**
   * Constructor. A missing list of trust mark types is an empty list.
   */
  public TrustAnchorIssuer {
    trustMarkTypes = trustMarkTypes == null ? List.of() : List.copyOf(trustMarkTypes);
  }
}
