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
package se.swedenconnect.oidf.registry.template;

import io.swagger.v3.oas.annotations.media.Schema;
import se.swedenconnect.oidf.registry.infrastructure.config.TemplateProperties;

import java.util.List;

/**
 * A template loaded from configuration.
 *
 * @param name unique name
 * @param description what the template is for, may be {@code null}
 * @param type what the template is used for
 * @param content the template, as JSON text, without the entries left out
 * @param omitted the entries left out when the template was loaded, because the registry does not support them yet
 * @author Felix Hellman
 */
@Schema(name = "Template")
public record TemplateDto(
    @Schema(description = "Unique name of the template", example = "federation-policy")
    String name,
    @Schema(description = "What the template is for")
    String description,
    @Schema(description = "STEP: rules for the Metadata Policy Creation step. POLICY: a metadata policy for a Trust "
        + "Anchor or Intermediate")
    TemplateProperties.Type type,
    @Schema(description = "The template, as JSON text, without the entries left out")
    String content,
    @Schema(description = "Entries left out when the template was loaded, because the registry does not support them "
        + "yet: entity type, parameter and why")
    List<String> omitted) {
}
