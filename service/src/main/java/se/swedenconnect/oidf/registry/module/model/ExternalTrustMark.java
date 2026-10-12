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
 * A trust mark type of a trust anchor together with the issuers outside the organization that are trusted to issue it,
 * stored as JSON in the external_trust_marks column of the module. It is one member of the trust_mark_issuers that the
 * trust anchor exports, with the trust mark type as the key.
 * <p>
 * With {@code allowAll} set no issuers are listed, and the trust mark type is exported with an empty list, which
 * OpenID Federation 1.0 section 3.1.2 defines as that anyone may issue trust marks of the type.
 *
 * @param trustMarkType the trust mark type identifier
 * @param allowAll whether anyone may issue trust marks of the type
 * @param issuers entity identifiers of the issuers that are trusted to issue the type, empty with allowAll
 * @author Per Fredrik Plars
 */
public record ExternalTrustMark(String trustMarkType, boolean allowAll, List<String> issuers) {

  /**
   * Constructor. A missing list of issuers is an empty list.
   */
  public ExternalTrustMark {
    issuers = issuers == null ? List.of() : List.copyOf(issuers);
  }
}
