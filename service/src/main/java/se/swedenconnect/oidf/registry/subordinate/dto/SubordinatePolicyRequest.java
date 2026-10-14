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

import java.util.Map;
import java.util.UUID;

/**
 * A metadata policy to set on every subordinate of a Trust Anchor or Intermediate, replacing their own.
 *
 * @param taImId the Trust Anchor or Intermediate whose subordinates get the policy
 * @param metadataPolicy the policy, with the root key metadata_policy; empty or {@code null} removes their policies
 * @author Felix Hellman
 */
@Schema(name = "SubordinatePolicyRequest")
public record SubordinatePolicyRequest(
    @Schema(description = "Trust Anchor or Intermediate whose subordinates get the policy")
    UUID taImId,
    @Schema(description = "Metadata policy with the root key metadata_policy, replacing each subordinate's own; "
        + "empty removes them")
    Map<String, Object> metadataPolicy) {
}
