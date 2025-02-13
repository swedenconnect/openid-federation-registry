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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.service.OptionsCRUDSelector;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.UUID;

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
  private final OptionsCRUDSelector optionsCRUDSelector;

  /**
   * Constructor for the OptionsApiController.
   *
   * @param optionsCRUDSelector    the selector used for determining CRUD operations-related logic for options.
   */
  public OptionsApiController(final OptionsCRUDSelector optionsCRUDSelector) {
    this.optionsCRUDSelector = optionsCRUDSelector;
  }

  /**
   * Retrieves optional data based on the specified option group and identifier.
   *
   * @param optionsgroup The option group to retrieve the data for.
   * @param identifier The identifier used to fetch the relevant options record.
   * @return A {@link ResponseEntity} containing the retrieved options record or an
   * appropriate response in case of an error.
   */
  @GetMapping("/{optionsgroup}/{identifier}")
  public ResponseEntity<?> getOptionalData(
      @PathVariable("optionsgroup") final String optionsgroup,
      @PathVariable(name = "identifier") final UUID identifier) {

    final OptionsRecord optionsRecord = this.optionsCRUDSelector.get(
        FkKeyType.valueOf(optionsgroup.toUpperCase()), identifier);

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
    final OptionsRecord optionsRecord = this.optionsCRUDSelector.template(
        FkKeyType.valueOf(optionsgroup.toUpperCase()));
    return ResponseEntity.ok(optionsRecord);
  }

  /**
   * Retrieves a list of options based on the specified options group.
   *
   * @param optionsgroup the name of the options group used to filter the list; must be provided as a path variable
   * @return a ResponseEntity containing the list of options for the specified group
   */
  @GetMapping("list/{optionsgroup}")
  public ResponseEntity<?> list(
      @PathVariable("optionsgroup") final String optionsgroup) {
    return ResponseEntity.ok(this.optionsCRUDSelector.list(FkKeyType.valueOf(optionsgroup.toUpperCase())));
  }

  /**
   * Updates the configuration for a specific options group and identifier.
   * Creates or updates an {@link OptionsRecord} based on the provided data.
   *
   * @param optionsgroup The options group to which the configuration belongs.
   * @param identifier The identifier of the specific configuration to update.
   * @param record The {@link OptionsRecord} containing the new or updated configuration data.
   * @return A {@link ResponseEntity} containing the updated {@link OptionsRecord} and a CREATED status if successful.
   */
  @PostMapping("/{optionsgroup}/{identifier}")
  public ResponseEntity<?> createConfig(
      @PathVariable("optionsgroup") final String optionsgroup,
      @PathVariable(name = "identifier") final UUID identifier,
      @RequestBody final OptionsRecord record) {

    final OptionsRecord optionsRecord = this.optionsCRUDSelector.create(
        FkKeyType.valueOf(optionsgroup.toUpperCase()), identifier, record);
    return ResponseEntity.status(HttpStatus.CREATED).body(optionsRecord);
  }

  /**
   * Updates the configuration based on the provided options group, identifier, and configuration details.
   *
   * @param optionsgroup the name of the options group used to categorize the configuration
   * @param identifier the unique identifier for the configuration to be updated
   * @param record the details of the configuration to be updated, encapsulated in an OptionsRecord object
   * @return a ResponseEntity containing the updated configuration details and HTTP status
   */
  @PutMapping("/{optionsgroup}/{identifier}")
  public ResponseEntity<?> updateConfig(
      @PathVariable("optionsgroup") final String optionsgroup,
      @PathVariable(name = "identifier") final UUID identifier,
      @RequestBody final OptionsRecord record) {

    final OptionsRecord optionsRecord = this.optionsCRUDSelector.update(
        FkKeyType.valueOf(optionsgroup.toUpperCase()), identifier, record);
    return ResponseEntity.status(HttpStatus.CREATED).body(optionsRecord);
  }

  /**
   * DELETE: Deletes a specific configuration.
   *
   * @param optionsgroup The configuration group.
   * @param identifier The identifier of the configuration.
   * @return Success message.
   */
  @DeleteMapping("/{optionsgroup}/{identifier}")
  public ResponseEntity<?> deleteConfig(
      @PathVariable("optionsgroup") final String optionsgroup,
      @PathVariable(name = "identifier") final UUID identifier) {
    this.optionsCRUDSelector.delete(FkKeyType.valueOf(optionsgroup.toUpperCase()), identifier);
    return ResponseEntity.ok("Configuration deleted successfully.");
  }

  /**
   * Handles GET requests to the "/list" endpoint to retrieve a list of options based on the provided query parameter.
   *
   * @param query the optional query string to filter the list of options. If not provided, all options will be
   *     returned.
   * @return a ResponseEntity containing the filtered or complete list of options.
   */
  @GetMapping("/list")
  public ResponseEntity<?> query(
      @RequestParam(value = "q", required = false) final String query) {
    return ResponseEntity.ok(this.optionsCRUDSelector.listAll(query));
  }

}

