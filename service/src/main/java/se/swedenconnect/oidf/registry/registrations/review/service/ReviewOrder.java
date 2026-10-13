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
package se.swedenconnect.oidf.registry.registrations.review.service;

import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationDomainRepository;
import se.swedenconnect.oidf.registry.organization.service.DomainMatcher;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.util.List;
import java.util.Optional;

/**
 * The order an entity is reviewed in: its domain first, then the trust marks it asked for, then its own registration.
 * Both the entity review and the single-step approval endpoint ask this before approving, so the order holds
 * whichever way an operator decides.
 *
 * @author Felix Hellman
 */
@Component
public class ReviewOrder {

  private final OrganizationDomainRepository organizationDomainRepository;
  private final RegistrationRepository registrationRepository;

  /**
   * Constructor.
   *
   * @param organizationDomainRepository repository of the domains organizations have claimed
   * @param registrationRepository repository of registrations
   */
  public ReviewOrder(final OrganizationDomainRepository organizationDomainRepository,
      final RegistrationRepository registrationRepository) {
    this.organizationDomainRepository = organizationDomainRepository;
    this.registrationRepository = registrationRepository;
  }

  /**
   * Lists the domains of the entity's organization that cover the entity's host, in any state.
   *
   * @param entity the entity's own registration
   * @return the domains
   */
  public List<OrganizationDomain> domainsOf(final Registration entity) {
    final Optional<String> host = DomainMatcher.hostOf(entity.getEntityId());
    if (host.isEmpty() || entity.getOrganization() == null) {
      return List.of();
    }
    return this.organizationDomainRepository
        .findByOrganization_OrganizationIdAndStatusIn(entity.getOrganization().getOrganizationId(),
            List.of(DomainStatus.PENDING, DomainStatus.VALIDATED, DomainStatus.REJECTED))
        .stream()
        .filter(domain -> DomainMatcher.matches(host.get(), domain.getDomain()))
        .toList();
  }

  /**
   * Returns the entity a registration belongs to: itself, or the parent of a trust mark registration.
   *
   * @param registration the registration
   * @return the entity's own registration
   */
  public static Registration entityOf(final Registration registration) {
    return registration.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE
        && registration.getParentRegistration() != null
        ? registration.getParentRegistration()
        : registration;
  }

  /**
   * Tells why a registration may not be approved yet, if it may not.
   *
   * <ul>
   *   <li>A trust mark or entity step waits while a domain covering the entity is pending and none is validated.</li>
   *   <li>An entity step also waits while a trust mark it asked for is pending.</li>
   * </ul>
   *
   * @param registration the registration about to be approved
   * @return the reason, or empty if it may be approved
   */
  public Optional<String> waitingFor(final Registration registration) {
    final Registration entity = entityOf(registration);
    final List<OrganizationDomain> domains = this.domainsOf(entity);
    final boolean validated = domains.stream().anyMatch(domain -> domain.getStatus() == DomainStatus.VALIDATED);
    final Optional<OrganizationDomain> pending = domains.stream()
        .filter(domain -> domain.getStatus() == DomainStatus.PENDING)
        .findFirst();
    if (!validated && pending.isPresent()) {
      return Optional.of("Decide the domain %s first".formatted(pending.get().getDomain()));
    }
    if (registration.getRegistrationType() != RegistrationType.TRUST_MARK_SUBORDINATE
        && this.registrationRepository.findByParentRegistration_RegistrationId(registration.getRegistrationId())
        .stream()
        .anyMatch(child -> child.getStatus() == RegistrationStatus.PENDING_APPROVAL)) {
      return Optional.of("Decide the trust mark requests first");
    }
    return Optional.empty();
  }
}
