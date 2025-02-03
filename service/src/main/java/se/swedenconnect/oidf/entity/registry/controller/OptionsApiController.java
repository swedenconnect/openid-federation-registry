/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.entity.registry.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.service.JpaOptionsService;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequestMapping("/registry/v1/options")
public class OptionsApiController {
  private final JpaOptionsService service;

  public OptionsApiController(final JpaOptionsService service) {
    this.service = service;
  }

  @GetMapping("/{optionsgroup}/{identifyer}")
  public ResponseEntity<?> getOptionalData(
      @PathVariable("optionsgroup") String optionsgroup,
      @PathVariable(name = "identifyer") String identifyer) {

    final OptionsRecord optionsRecord = service.get(FkKeyType.valueOf(optionsgroup.toUpperCase()), identifyer);

    return ResponseEntity.ok(optionsRecord);
  }

  @GetMapping("/{optionsgroup}")
  public ResponseEntity<?> getTemplateConfig(
      @PathVariable("optionsgroup") String optionsgroup) {

    final OptionsRecord optionsRecord = service.getTemplate(FkKeyType.valueOf(optionsgroup.toUpperCase()));
    if (optionsRecord == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Configuration not found.");
    }
    return ResponseEntity.ok(optionsRecord);
  }

  @PostMapping("/{optionsgroup}/{identifyer}")
  public ResponseEntity<?> updateConfig(
      @PathVariable("optionsgroup") String optionsgroup,
      @PathVariable(name = "identifyer") String identifyer,
      @RequestBody OptionsRecord record) {

    final OptionsRecord optionsRecord = this.service.create(
        FkKeyType.valueOf(optionsgroup.toUpperCase()), identifyer, record);
    return ResponseEntity.status(HttpStatus.CREATED).body(optionsRecord);

  }

  /**
   * DELETE: Deletes a specific configuration.
   *
   * @param configgroup The configuration group.
   * @param identifyer The identifier of the configuration.
   * @return Success message.
   */
  @DeleteMapping("/{configgroup}/{identifyer}")
  public ResponseEntity<?> deleteConfig(
      @PathVariable String configgroup,
      @PathVariable String identifyer) {

    return ResponseEntity.ok("Configuration deleted successfully.");
  }

}

