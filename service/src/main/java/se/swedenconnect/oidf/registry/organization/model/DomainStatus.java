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
package se.swedenconnect.oidf.registry.organization.model;

/**
 * Review state of an {@link OrganizationDomain}.
 *
 * <p>A domain is created as {@link #PENDING} when the organization requests it, and is moved to
 * {@link #VALIDATED} or {@link #REJECTED} by the tenant operator. A {@link #REJECTED} domain may be requested
 * again, which re-opens the same row as {@link #PENDING}. Both {@link #PENDING} and {@link #VALIDATED} count as
 * registered for the purposes of registration-request domain enforcement.
 *
 * @author Felix Hellman
 */
public enum DomainStatus {

  /** Requested by the organization, awaiting operator review. */
  PENDING,

  /** Approved by the tenant operator. */
  VALIDATED,

  /** Rejected by the tenant operator, carrying a rejection reason. */
  REJECTED
}
