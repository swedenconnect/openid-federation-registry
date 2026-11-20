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
 * A flow assignment as it is written to the audit log. The three kinds of assignment share one record, so the
 * target is named by its type and its ID.
 *
 * @param assignId the unique assignment ID
 * @param flowId the assigned flow ID
 * @param flowName the flow display name
 * @param targetType what the flow was assigned to
 * @param targetId the ID of the intermediate, trust mark issuer or trust mark
 * @author Per Fredrik Plars
 */
public record FlowAssignmentAuditDto(
    UUID assignId,
    UUID flowId,
    String flowName,
    TargetType targetType,
    UUID targetId
) {

  /** What a flow can be assigned to. */
  public enum TargetType {
    /** A trust anchor or intermediate module. */
    INTERMEDIATE,
    /** A trust mark issuer module. */
    TRUSTMARK_ISSUER,
    /** A single trust mark of a trust mark issuer. */
    TRUSTMARK
  }
}
