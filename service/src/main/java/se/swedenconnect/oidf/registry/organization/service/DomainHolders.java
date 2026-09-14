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
package se.swedenconnect.oidf.registry.organization.service;

import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Decides whether a domain is already held by somebody else on the tenant.
 *
 * <p>Only one organization on an instance holds a given domain at a time. "Holds" means a claim in status
 * {@code PENDING} or {@code VALIDATED}; a rejected or deleted claim releases the domain. The decision is on the
 * exact domain string only -- another organization may hold {@code a.example.se} while this one holds
 * {@code example.se}, so nothing here walks the name hierarchy.
 *
 * <p>Pure and side-effect free: the caller supplies the claims standing on the domain, so the same rule can be
 * applied when an organization claims a domain and when the operator approves one.
 *
 * @author Felix Hellman
 */
public final class DomainHolders {

  private DomainHolders() {
  }

  /**
   * Tests whether any of the given claims on a domain belongs to an organization other than the one asking.
   *
   * @param holders the claims standing on the domain, on one instance, in the holding statuses
   * @param organizationId the organization asking for the domain
   * @return true if at least one claim belongs to another organization
   */
  public static boolean heldByAnotherOrganization(final Collection<OrganizationDomain> holders,
      final UUID organizationId) {
    if (holders == null || organizationId == null) {
      return false;
    }
    return holders.stream()
        .map(OrganizationDomain::getOrganization)
        .filter(Objects::nonNull)
        .map(Organization::getOrganizationId)
        .filter(Objects::nonNull)
        .anyMatch(holder -> !holder.equals(organizationId));
  }

  /**
   * The message reported when a domain is already held by another organization on the tenant. Kept in one place
   * so the claim path, the approval path and the unique-key race path all report the same conflict.
   *
   * @param domain the domain that is already held
   * @return the conflict message
   */
  public static String alreadyHeldMessage(final String domain) {
    return "Domain %s is already held by another organization".formatted(domain);
  }
}
