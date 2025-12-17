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
package se.swedenconnect.oidf.registry.validation;

/**
 * Validates specific parts of a metadata section
 *
 * @author Per Fredrik Plars
 */
public interface MetadataValidator {
  /**
   * Tests if this validator supports this specific node.
   *
   * @param metadataNodeName nodename ex openid_relyingparty
   * @return true if supported
   */
  boolean supports(final String metadataNodeName);

}
