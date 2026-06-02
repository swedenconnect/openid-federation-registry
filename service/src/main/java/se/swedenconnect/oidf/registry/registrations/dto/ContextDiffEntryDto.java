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
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.registrations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes a single change to the pipeline context caused by a step execution.
 *
 * @param key context key that changed
 * @param changeType one of {@code ADDED}, {@code CHANGED}, or {@code REMOVED}
 * @param before string representation before execution, or {@code null} if added
 * @param after string representation after execution, or {@code null} if removed
 * @author Felix Hellman
 */
@Schema(name = "ContextDiffEntry")
public record ContextDiffEntryDto(

    @Schema(description = "Context key that changed")
    String key,

    @Schema(description = "Type of change: ADDED, CHANGED, or REMOVED")
    String changeType,

    @Schema(description = "Value before execution, null for ADDED entries")
    String before,

    @Schema(description = "Value after execution, null for REMOVED entries")
    String after
) {
}
