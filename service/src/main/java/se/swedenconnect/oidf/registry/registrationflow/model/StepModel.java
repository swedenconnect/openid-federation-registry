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
package se.swedenconnect.oidf.registry.registrationflow.domain;

import se.swedenconnect.oidf.registry.registrationflow.dto.ConfigValueDto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a single pipeline step — its name, base settings and full configuration.
 *
 * @param stepId unique step identifier
 * @param name display name
 * @param description human-readable description
 * @param config list of configurable values for this step
 * @author Per Fredrik Plars
 */
public record Step(
    UUID stepId,
    List<ConfigValue> config
) {
}
