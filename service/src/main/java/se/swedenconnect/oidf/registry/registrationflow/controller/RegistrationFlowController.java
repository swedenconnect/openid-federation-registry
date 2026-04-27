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
package se.swedenconnect.oidf.registry.registrationflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.registrationflow.dto.StepInfoDto;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;

import java.util.List;

/**
 * Exposes the configured registration flow pipeline.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequestMapping("/registration-flow")
@Tag(name = "Registration Flow", description = "Configured pipeline steps for entity registration")
public class RegistrationFlowController {

  private final List<StepDefinition<?>> steps;

  public RegistrationFlowController(
      @Qualifier("registrationFlowSteps") final List<StepDefinition<?>> steps) {
    this.steps = steps;
  }

  @GetMapping("/steps")
  @Operation(summary = "List all configured pipeline steps with their settings")
  public ResponseEntity<List<StepInfoDto>> getSteps() {
    final List<StepInfoDto> result = steps.stream()
        .map(def -> new StepInfoDto(def.name(), def.failOnError(), def.enabled(), def.config()))
        .toList();
    return ResponseEntity.ok(result);
  }
}
