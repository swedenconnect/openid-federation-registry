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
package se.swedenconnect.oidf.registry.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for claiming a domain for an organization.
 *
 * @param domain the bare hostname to claim — no scheme, path, port or wildcard; stored lower-cased
 * @author Felix Hellman
 */
@Schema(name = "DomainRequest", description = "A domain an organization claims")
public record DomainRequestDto(

    @Schema(description = "Bare hostname, no scheme, path, port or wildcard", example = "example.com")
    String domain
) {
}
