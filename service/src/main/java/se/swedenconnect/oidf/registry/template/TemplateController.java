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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.infrastructure.config.TemplateProperties;

import java.util.List;

/**
 * Lists the templates loaded from configuration, for an operator to start a step's rules or a module's policy from.
 *
 * @author Felix Hellman
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/registry/v1/{tenant}/{orgNumber}/templates")
@Tag(name = "Templates", description = "Templates loaded from configuration")
public class TemplateController {

  private final TemplateCatalog templateCatalog;

  /**
   * Lists the configured templates.
   *
   * @param tenant the tenant identifier
   * @param orgNumber the organization number
   * @param type optional type filter, {@code STEP} or {@code POLICY}
   * @return the templates, in configured order
   */
  @GetMapping
  @PreAuthorize("@orgRightsService.canRead(authentication, #orgNumber, #tenant)")
  @Operation(summary = "List templates", description = "Lists the templates loaded from configuration, optionally "
      + "of one type")
  public ResponseEntity<List<TemplateDto>> listTemplates(
      @PathVariable("tenant") @P("tenant") final String tenant,
      @PathVariable("orgNumber") @P("orgNumber") final String orgNumber,
      @RequestParam(name = "type", required = false) final TemplateProperties.Type type) {
    return ResponseEntity.ok(this.templateCatalog.list(type));
  }
}
