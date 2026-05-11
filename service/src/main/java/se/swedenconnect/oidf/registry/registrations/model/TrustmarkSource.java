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

package se.swedenconnect.oidf.registry.registrations.model;

import java.io.Serializable;
import java.util.List;

/**
 * Representation of Trustmarks
 *
 * @param trustMarkIssuer entity identifier of the trustmark issuer
 * @param trustmarkType   list of trustmark type URIs issued by this issuer
 * @author Per Fredrik Plars
 */
public record TrustmarkSource(String trustMarkIssuer, List<String> trustmarkType) implements Serializable {

}
