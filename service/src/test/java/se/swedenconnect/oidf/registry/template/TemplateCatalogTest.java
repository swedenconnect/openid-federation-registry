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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import se.swedenconnect.oidf.registry.infrastructure.config.TemplateProperties;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.MetadataPolicyCreationStep;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the Sweden Connect registration template as a configured step template.
 */
class TemplateCatalogTest {

  @Test
  @DisplayName("The Sweden Connect registration template is loaded whole, with the signed_jwks_uri rule")
  void registrationTemplateIsLoadedWhole() {
    final JsonMapper jsonMapper = JsonMapper.builder().build();
    final TemplateProperties properties = new TemplateProperties(List.of(new TemplateProperties.Template(
        "sweden-connect-registration", "Registration Intermediate Metadata Assignment",
        TemplateProperties.Type.STEP,
        "file:" + registrationTemplate())));

    final TemplateCatalog catalog = new TemplateCatalog(properties, new DefaultResourceLoader(), jsonMapper,
        new MetadataPolicyCreationStep(jsonMapper, null, null));

    assertThat(catalog.list(TemplateProperties.Type.STEP)).singleElement().satisfies(template -> {
      assertThat(template.omitted()).isEmpty();
      assertThat(template.content()).contains("signed_jwks_uri");
    });
  }

  private static Path registrationTemplate() {
    final Path inProfile = Path.of("docs", "profiles", "sweden-connect", "registration-template.json");
    final Path path = Files.exists(inProfile) ? inProfile : Path.of("..").resolve(inProfile);
    assertThat(path).exists();
    return path;
  }
}
