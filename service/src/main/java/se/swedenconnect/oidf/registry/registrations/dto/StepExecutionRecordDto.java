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
package se.swedenconnect.oidf.registry.registrations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Execution outcome of a single pipeline step.
 *
 * @param stepName display name of the step
 * @param status step outcome (SUCCESS, WARNING, FAILURE)
 * @param message optional human-readable summary
 * @param issues validation issues found during this step
 * @author Per Fredrik Plars
 */
@Schema(name = "StepExecutionRecord")
public record StepExecutionRecordDto(

    @Schema(description = "Display name of the step", example = "LoadEntityConfigurationStep")
    String stepName,

    @Schema(description = "Step execution outcome", example = "SUCCESS")
    String status,

    @Schema(description = "Human-readable summary of the step result")
    String message,

    @Schema(description = "Validation issues found during this step")
    List<StepIssueDto> issues
) {
}
