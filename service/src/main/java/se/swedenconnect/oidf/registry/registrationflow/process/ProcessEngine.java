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
package se.swedenconnect.oidf.registry.registrationflow.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepStatus;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationRequestDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executes a sequential pipeline of {@link StepDefinition} instances against a shared {@link ProcessContext}.
 *
 * @author Per Fredrik Plars
 */
@Component
public class ProcessEngine {

  private static final Logger log = LoggerFactory.getLogger(ProcessEngine.class);


  /**
   * Registers a new registration request by executing the configured process flow.
   *
   * @param dto the registration request
   */
  public void register(final RegistrationRequestDto dto) {
    //TODO
    // In data validation, make sure that the data exist that is expected entityid.
    // Load assignment, if not found return notfound
    // Load refistrationflow with its configuration.
    // Execute the flow.
    // Collect the info or error messages and convertthem into the right format
    throw new IllegalArgumentException("Not supported yet.");
  }

  /**
   * Executes the pipeline sequentially, stopping on the first failure.
   *
   * @param steps ordered list of step definitions
   * @param ctx shared pipeline context
   * @return aggregated report
   */
  public ProcessReport run(final List<StepDefinition> steps, final ProcessContext ctx) {
    final List<StepExecutionRecord> records = new ArrayList<>();

    for (final StepDefinition def : steps) {
      if (!def.enabled()) {
        log.debug("Skipping step: {}", def.name());
        continue;
      }

      log.info("Running step: {}", def.name());
      final StepResult result = def.run(ctx);
      records.add(new StepExecutionRecord(def.name(), result));

      if (result.status() == StepStatus.FAILURE) {
        log.error("Step {} failed — aborting pipeline", def.name());
        return ProcessReport.skipped(records);
      }
    }

    return ProcessReport.completed(records);
  }
}
