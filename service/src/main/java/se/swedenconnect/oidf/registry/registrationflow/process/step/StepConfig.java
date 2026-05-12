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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base configuration for all pipeline steps.
 * <p>
 * Subclasses add domain-specific settings.
 *
 * @author Per Fredrik Plars
 */
public abstract class StepConfig implements Serializable {

  final Map<String, Object> dataValues;

  /**
   * Constructs a StepConfig with the given data values.
   *
   * @param dataValues map of configuration key-value pairs
   */
  public StepConfig(final Map<String, Object> dataValues) {
    this.dataValues = dataValues;
  }

  /**
   * Returns the raw value for the given key.
   *
   * @param name configuration key
   * @return raw value
   */
  public Object getValue(final String name) {
    return this.dataValues.get(name);
  }

  /**
   * Returns the boolean value for the given key.
   *
   * @param name configuration key
   * @return parsed boolean value
   */
  public Boolean getBoolean(final String name) {
    return Boolean.valueOf(Objects.toString(this.dataValues.get(name)));
  }

  /**
   * Returns the string value for the given key.
   *
   * @param name configuration key
   * @return string value
   */
  public String getString(final String name) {
    return this.dataValues.get(name).toString();
  }

  /**
   * Returns the integer value for the given key.
   *
   * @param name configuration key
   * @return parsed integer value
   */
  public int getInt(final String name) {
    return Integer.parseInt(Objects.toString(this.dataValues.get(name)));
  }

  /**
   * Returns the list value for the given key.
   *
   * @param name configuration key
   * @return list of strings
   */
  public List<String> getList(final String name) {
    return (List<String>) this.dataValues.get(name);
  }

  /**
   * Returns all declared configuration values for this step.
   *
   * @return list of configuration value descriptors
   */
  public abstract List<StepConfigurationValue> getStepConfigurationValues();
}
