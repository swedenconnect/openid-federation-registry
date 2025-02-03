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
 * Controller class for managing operations on options-related configurations.
 * Provides endpoints to retrieve, create, update, and delete configurations
 * associated with specific option groups and identifiers.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequestMapping("/registry/v1/options")
public class OptionsApiController {
  private final JpaOptionsService service;

  /**
   * Constructs an OptionsApiController instance with the specified service. This controller manages operations on
   * option-related configurations using the provided JpaOptionsService for data access and processing.
   *
   * @param service The JpaOptionsService instance used to manage configurations.
   */
  public OptionsApiController(final JpaOptionsService service) {
    this.service = service;
  }

  /**
   * Retrieves optional data based on the specified option group and identifier.
   *
   * @param optionsgroup The option group to retrieve the data for.
   * @param identifyer The identifier used to fetch the relevant options record.
   * @return A {@link ResponseEntity} containing the retrieved options record or an
   * appropriate response in case of an error.
   */
  @GetMapping("/{optionsgroup}/{identifyer}")
  public ResponseEntity<?> getOptionalData(
      @PathVariable("optionsgroup") final String optionsgroup,
      @PathVariable(name = "identifyer") final String identifyer) {

    final OptionsRecord optionsRecord = this.service.get(FkKeyType.valueOf(optionsgroup.toUpperCase()), identifyer);

    return ResponseEntity.ok(optionsRecord);
  }

  /**
   * Retrieves the configuration template for the specified options group.
   *
   * @param optionsgroup The options group for which the template configuration is to be retrieved.
   * @return A {@link ResponseEntity} containing the template configuration as an {@link OptionsRecord}
   *         if found, or a NOT_FOUND status with an error message if the configuration does not exist.
   */
  @GetMapping("/{optionsgroup}")
  public ResponseEntity<?> getTemplateConfig(
      @PathVariable("optionsgroup") final String optionsgroup) {

    final OptionsRecord optionsRecord = this.service.getTemplate(FkKeyType.valueOf(optionsgroup.toUpperCase()));
    if (optionsRecord == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Configuration not found.");
    }
    return ResponseEntity.ok(optionsRecord);
  }

  /**
   * Updates the configuration for a specific options group and identifier.
   * Creates or updates an {@link OptionsRecord} based on the provided data.
   *
   * @param optionsgroup The options group to which the configuration belongs.
   * @param identifyer The identifier of the specific configuration to update.
   * @param record The {@link OptionsRecord} containing the new or updated configuration data.
   * @return A {@link ResponseEntity} containing the updated {@link OptionsRecord} and a CREATED status if successful.
   */
  @PostMapping("/{optionsgroup}/{identifyer}")
  public ResponseEntity<?> updateConfig(
      @PathVariable("optionsgroup") final String optionsgroup,
      @PathVariable(name = "identifyer") final String identifyer,
      @RequestBody final OptionsRecord record) {

    final OptionsRecord optionsRecord = this.service.create(
        FkKeyType.valueOf(optionsgroup.toUpperCase()), identifyer, record);
    return ResponseEntity.status(HttpStatus.CREATED).body(optionsRecord);

  }

  /**
   * DELETE: Deletes a specific configuration.
   *
   * @param optionsgroup The configuration group.
   * @param identifyer The identifier of the configuration.
   * @return Success message.
   */
  @DeleteMapping("/{configgroup}/{identifyer}")
  public ResponseEntity<?> deleteConfig(
      @PathVariable final String optionsgroup,
      @PathVariable final String identifyer) {
    this.service.delete(FkKeyType.valueOf(optionsgroup.toUpperCase()), identifyer);
    return ResponseEntity.ok("Configuration deleted successfully.");
  }

}

