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

import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;

import java.util.List;
import java.util.Map;

/**
 * Configuration for {@link RpMetadataValidationStep}.
 *
 * @author Per Fredrik Plars
 */
public class RpMetadataValidationConfig extends StepConfig {

  private String validationProfileId;
  private List<String> requiredScopes = List.of();
  private boolean strictAcrValues = false;

  /**
   * Constructs a new RpMetadataValidationConfig.
   *
   * @param dataValues map of configuration key-value pairs
   */
  public RpMetadataValidationConfig(final Map<String, Object> dataValues) {
    super(dataValues);
  }

  /**
   * Returns the validation profile ID.
   *
   * @return validation profile ID
   */
  public String getValidationProfileId() {
    return super.getString("validationProfileId");
  }

  /**
   * Returns the list of required scopes.
   *
   * @return required scopes
   */
  public List<String> getRequiredScopes() {
    return super.getList("requiredScopes");
  }

  /**
   * Returns whether strict ACR values validation is enabled.
   *
   * @return true if strict ACR values is enabled
   */
  public Boolean isStrictAcrValues() {
    return super.getBoolean("isstrictAcrValues");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(
        new StepConfigurationValue("validationProfileId", StepConfigurationValue.DATA_TYPE.STRING, "Demo value", null),
        new StepConfigurationValue("requiredScopes", StepConfigurationValue.DATA_TYPE.LIST, "DemoValue", null),
        new StepConfigurationValue("isstrictAcrValues", StepConfigurationValue.DATA_TYPE.BOOLEAN, "DemoValue", null));
  }

}
