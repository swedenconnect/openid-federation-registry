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
package se.swedenconnect.oidf.registry.registrationflow.dto;

import java.util.UUID;

/**
 * Summary of a flow assignment for a specific trust mark.
 *
 * @param assignId unique assignment ID (used for unassign)
 * @param trustmarkId the trust mark this assignment belongs to
 * @param trustmarkType the trust mark type (display)
 * @param flowId the assigned flow ID
 * @param name the flow display name
 * @param description the flow description
 * @author Felix Hellman
 */
public record TrustMarkFlowAssignmentDto(
    UUID assignId,
    UUID trustmarkId,
    String trustmarkType,
    UUID flowId,
    String name,
    String description
) {
}
