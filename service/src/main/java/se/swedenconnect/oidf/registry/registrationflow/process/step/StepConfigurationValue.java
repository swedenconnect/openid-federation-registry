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

/**
 * Describes a single configurable value for a pipeline step.
 *
 * @param name the configuration key name
 * @param dataType the expected data type
 * @param description human-readable description of this value
 * @param defaultValue optional default value
 * @author Per Fredrik Plars
 */
public record StepConfigurationValue(String name,
    DATA_TYPE dataType,
    String description,
    Object defaultValue) {

  /**
   * Supported data types for step configuration values.
   */
  public enum DATA_TYPE {STRING, INT, BOOLEAN, NUMERIC, LIST}
}
