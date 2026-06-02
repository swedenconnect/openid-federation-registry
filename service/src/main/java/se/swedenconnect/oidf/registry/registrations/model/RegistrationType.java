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
package se.swedenconnect.oidf.registry.registrations.model;

/**
 * Distinguishes the kind of registration being processed.
 *
 * @author Felix Hellman
 */
public enum RegistrationType {

  /** Standard subordinate registration. */
  SUBORDINATE,

  /**
   * Subordinate registration that also requests trust marks.
   * Uses the same API as {@link #SUBORDINATE}; the trust mark flow
   * executes after the subordinate step completes.
   */
  TRUST_MARK_SUBORDINATE
}
