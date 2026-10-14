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
package se.swedenconnect.oidf.registry.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Templates the registry loads from configuration, so a deployment can offer its own templates without the code
 * naming them.
 *
 * @param templates the templates; empty or unset for none
 * @author Felix Hellman
 */
@ConfigurationProperties("openid.federation.registry")
public record TemplateProperties(List<Template> templates) {

  /**
   * Returns the configured templates.
   *
   * @return the templates, empty if none are configured
   */
  public List<Template> templatesOrEmpty() {
    return this.templates == null ? List.of() : this.templates;
  }

  /**
   * What a template is used for.
   */
  public enum Type {
    /** Rules for the Metadata Policy Creation step, see {@code docs/metadata_policy.md}. */
    STEP,
    /** A metadata policy a Trust Anchor or Intermediate applies to the subordinate statements it issues. */
    POLICY
  }

  /**
   * One template.
   *
   * @param name unique name, shown when choosing a template
   * @param description what the template is for, shown when choosing a template; optional
   * @param type what the template is used for
   * @param location where the template is read from, as a Spring resource location such as
   *     {@code file:/etc/registry/policy.json} or {@code classpath:templates/policy.json}
   */
  public record Template(String name, String description, Type type, String location) {
  }
}
