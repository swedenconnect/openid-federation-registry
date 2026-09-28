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
 * Rejection reasons the registry sets itself, as opposed to the free text an operator types when rejecting a
 * registration by hand. They are part of the API contract — the portal matches on them — so they are constants
 * rather than inline strings.
 *
 * @author Felix Hellman
 */
public final class RegistrationRejectionReasons {

  /**
   * Set on registrations rejected as a consequence of the tenant operator rejecting the domain their entity
   * identifier resolved under.
   */
  public static final String NOT_ACCEPTED_DOMAIN = "Not Accepted Domain";

  private RegistrationRejectionReasons() {
  }
}
