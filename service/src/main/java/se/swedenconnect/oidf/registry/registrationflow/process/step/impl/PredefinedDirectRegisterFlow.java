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
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.util.UUID;

/**
 * Pre step that handle the registration state in database.
 *
 * @author Per Fredrik Plars
 */
@Component
public class PredefinedDirectRegisterFlow extends NoConfigStepAdapter {

  final PublishSubordinateStatementStep publishSubordinateStatementStep;
  final LoadEntityConfigurationStep loadEntityConfigurationStep;

  /**
   * Creates a combined step
   * @param publishSubordinateStatementStep subordinateStatmentPublicher
   * @param loadEntityConfigurationStep EC loader
   */
  public PredefinedDirectRegisterFlow(final PublishSubordinateStatementStep publishSubordinateStatementStep,
      final LoadEntityConfigurationStep loadEntityConfigurationStep) {
    this.publishSubordinateStatementStep = publishSubordinateStatementStep;
    this.loadEntityConfigurationStep = loadEntityConfigurationStep;
  }

  @Override
  public String getDescription() {
    return """
        Pre defied registration flow that will insert this registration to registration storage. 
        Load entityconfiguration, extract jwks and create a subordinate statement.". 
        """;

  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final StepResult srFirst = this.loadEntityConfigurationStep.execute(ctx,config);
    final StepResult srSecond = this.publishSubordinateStatementStep.execute(ctx,config);
    return srFirst.merge(srSecond);
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("AE67B1D8-2DCF-4A8C-9E6B-FC972CC65DEA");
  }

}
