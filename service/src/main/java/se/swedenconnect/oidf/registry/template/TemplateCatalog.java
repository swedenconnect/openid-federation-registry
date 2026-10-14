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

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.infrastructure.config.TemplateProperties;
import se.swedenconnect.oidf.registry.policy.MetadataPolicies;
import se.swedenconnect.oidf.registry.registrationflow.process.step.impl.MetadataPolicyCreationStep;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The templates configured under {@code openid.federation.registry.templates}, read and checked when the registry
 * starts. A template that cannot be read or used stops the start, with a message naming it.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class TemplateCatalog {

  private final List<TemplateDto> templates;

  /**
   * Constructor.
   *
   * @param properties the configured templates
   * @param resourceLoader loader for the template locations
   * @param jsonMapper mapper used to read policy templates
   * @param metadataPolicyCreationStep the step whose templates are checked as it checks its own
   * @throws IllegalStateException if a template cannot be read or used
   */
  public TemplateCatalog(final TemplateProperties properties, final ResourceLoader resourceLoader,
      final JsonMapper jsonMapper, final MetadataPolicyCreationStep metadataPolicyCreationStep) {
    final List<TemplateDto> loaded = new ArrayList<>();
    final Set<String> names = new HashSet<>();
    for (final TemplateProperties.Template template : properties.templatesOrEmpty()) {
      final String at = "openid.federation.registry.templates[%s]".formatted(template.name());
      if (template.name() == null || template.name().isBlank()) {
        throw new IllegalStateException("openid.federation.registry.templates[].name must be given");
      }
      if (!names.add(template.name())) {
        throw new IllegalStateException(at + ": the name is used by more than one template");
      }
      if (template.type() == null) {
        throw new IllegalStateException(at + ".type must be step or policy");
      }
      if (template.location() == null || template.location().isBlank()) {
        throw new IllegalStateException(at + ".location must be given");
      }
      final String read = read(at, resourceLoader.getResource(template.location()));
      final Supported supported = template.type() == TemplateProperties.Type.POLICY
          ? supportedPolicy(template.name(), read, jsonMapper)
          : supportedStep(template.name(), read, jsonMapper);
      final String content = supported.content();
      final List<String> problems = switch (template.type()) {
        case STEP -> metadataPolicyCreationStep.validateConfiguration(
            Map.of(MetadataPolicyCreationStep.TEMPLATE, content));
        case POLICY -> policyProblems(content, jsonMapper);
      };
      if (!problems.isEmpty()) {
        throw new IllegalStateException(at + " cannot be used: " + String.join("; ", problems));
      }
      loaded.add(new TemplateDto(template.name(), template.description(), template.type(), content,
          supported.omitted()));
      log.info("Loaded {} template '{}' from {}", template.type(), template.name(), template.location());
    }
    this.templates = List.copyOf(loaded);
  }

  /**
   * Returns the templates of a type, in configured order.
   *
   * @param type the type; {@code null} for all
   * @return the templates
   */
  public List<TemplateDto> list(final TemplateProperties.Type type) {
    return this.templates.stream().filter(template -> type == null || template.type() == type).toList();
  }

  private static String read(final String at, final Resource resource) {
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    catch (final IOException e) {
      throw new IllegalStateException(at + ".location cannot be read: " + e.getMessage(), e);
    }
  }

  /**
   * Leaves out the parameters of a policy template the policy library cannot read yet, with a warning naming each. The
   * template is assumed to follow the specification. Text that is not JSON is returned as it is, for the check to
   * report.
   */
  @SuppressWarnings("unchecked")
  private static Supported supportedPolicy(final String name, final String content, final JsonMapper jsonMapper) {
    final Map<String, Object> policy;
    try {
      policy = jsonMapper.readValue(content, Map.class);
    }
    catch (final JacksonException e) {
      return new Supported(content, List.of());
    }
    final MetadataPolicies.Supported supported = MetadataPolicies.supportedOnly(policy);
    if (supported.omitted().isEmpty()) {
      return new Supported(content, List.of());
    }
    supported.omitted().forEach(omitted ->
        log.warn("Policy template '{}': left out {}, not supported yet", name, omitted));
    return new Supported(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(supported.policy()),
        supported.omitted());
  }

  /**
   * Leaves out the rules of a step template the registry cannot apply yet: a rule that removes a parameter with
   * {@code value: null}. The template is assumed to follow the specification. Text that is not JSON is returned as it
   * is, for the check to report.
   */
  @SuppressWarnings("unchecked")
  private static Supported supportedStep(final String name, final String content, final JsonMapper jsonMapper) {
    final Map<String, Object> template;
    try {
      template = jsonMapper.readValue(content, Map.class);
    }
    catch (final JacksonException e) {
      return new Supported(content, List.of());
    }
    if (!(template.get("entity_types") instanceof final Map<?, ?> entityTypes)) {
      return new Supported(content, List.of());
    }
    final List<String> omitted = new ArrayList<>();
    entityTypes.forEach((type, rules) -> {
      if (rules instanceof final List<?> list) {
        ((List<Object>) list).removeIf(rule -> {
          final boolean removesWithNull = rule instanceof final Map<?, ?> map
              && map.containsKey("static_value") && map.get("static_value") == null;
          if (removesWithNull) {
            omitted.add("%s.%s (value null)".formatted(type, ((Map<?, ?>) rule).get("parameter")));
          }
          return removesWithNull;
        });
      }
    });
    if (omitted.isEmpty()) {
      return new Supported(content, List.of());
    }
    omitted.forEach(rule -> log.warn("Step template '{}': left out {}, not supported yet", name, rule));
    return new Supported(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(template), omitted);
  }

  /**
   * A template as loaded, and what was left out of it.
   *
   * @param content the template, as JSON text
   * @param omitted one message per entry left out
   */
  private record Supported(String content, List<String> omitted) {
  }

  @SuppressWarnings("unchecked")
  private static List<String> policyProblems(final String content, final JsonMapper jsonMapper) {
    try {
      final Map<String, Object> policy = jsonMapper.readValue(content, Map.class);
      if (policy == null || policy.isEmpty()) {
        return List.of("the policy is empty");
      }
      return MetadataPolicies.problems(policy);
    }
    catch (final JacksonException e) {
      return List.of("not JSON: " + e.getOriginalMessage());
    }
  }
}
