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
package se.swedenconnect.oidf.registry.subordinate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The outcome of setting a metadata policy on every subordinate of a Trust Anchor or Intermediate.
 *
 * @param updated how many subordinates got the policy
 * @author Felix Hellman
 */
@Schema(name = "SubordinatePolicyResult")
public record SubordinatePolicyResult(
    @Schema(description = "How many subordinates got the policy")
    int updated) {
}
