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
 * Summary of a flow assignment for an intermediate, including the assign ID needed to remove it.
 *
 * @param assignId the unique assignment ID (used for unassign)
 * @param flowId the assigned flow ID
 * @param name the flow display name
 * @param description the flow description
 * @author Per Fredrik Plars
 */
public record IntermediateFlowAssignmentDto(
    UUID assignId,
    UUID flowId,
    String name,
    String description
) {
}
