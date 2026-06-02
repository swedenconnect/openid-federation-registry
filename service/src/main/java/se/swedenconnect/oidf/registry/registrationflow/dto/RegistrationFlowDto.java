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

import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;

import java.util.List;
import java.util.UUID;

/**
 * Defines a complete execution flow.
 *
 * @param flowId unique flow identifier
 * @param name display name
 * @param description human-readable description
 * @param descriptionSv Swedish description
 * @param technology protocol technology (OIDC or SAML)
 * @param entityType entity type as defined in OpenID Federation
 * @param steps ordered list of step definitions
 * @param flowType the type of flow (INTERMEDIATE or TRUST_MARK_ISSUER)
 * @author Per Fredrik Plars
 */
public record RegistrationFlowDto(
    UUID flowId,
    String name,
    String description,
    String descriptionSv,
    Technology technology,
    String entityType,
    List<StepDto> steps,
    Step.FlowType flowType
) {
}
