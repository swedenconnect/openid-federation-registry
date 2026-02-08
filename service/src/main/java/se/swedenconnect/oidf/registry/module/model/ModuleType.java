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

package se.swedenconnect.oidf.registry.module.model;

/**
 * Enum representing the type of a module in the system. Used to categorize modules into specific roles or
 * functionalities.
 *
 * INTERMEDIATE - Represents an intermediate module, typically used for subordinate purposes within a trust
 * hierarchy.
 *
 * TRUSTANCHOR - Represents a trust anchor module, a root entity within a trust hierarchy.
 *
 * @author Per Fredrik Plars
 */
public enum ModuleType {
  INTERMEDIATE,
  TRUSTANCHOR
}
