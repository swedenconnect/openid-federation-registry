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
package se.swedenconnect.oidf.registry.registrationflow.process.step;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Base configuration for all pipeline steps.
 * <p>
 * Subclasses add domain-specific settings.
 *
 * @author Per Fredrik Plars
 */
public abstract class StepConfig implements Serializable {

  final Map<String, Object> dataValues;

  public StepConfig(final Map<String, Object> dataValues) {
    this.dataValues = dataValues;
  }

  public Object getValue(String name) {
    return this.dataValues.get(name);
  }

  public Boolean getBoolean(String name) {
    return Boolean.parseBoolean(this.dataValues.get(name).toString());
  }

  public String getString(String name) {
    return this.dataValues.get(name).toString();
  }

  public int getInt(String name) {
    return Integer.parseInt(this.dataValues.get(name).toString());
  }

  public List<String> getList(String name) {
    return (List<String>) this.dataValues.get(name);
  }

  public abstract List<StepConfigurationValue> getStepConfigurationValues();
}
