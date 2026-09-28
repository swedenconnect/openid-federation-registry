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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.organization.dto.CreateOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRequestDto;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.mapper.OrganizationMapper;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.model.OrganizationTrustMark;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationDomainRepository;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationTrustMarkRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Portal-facing organization API: an organization bootstrapping its own registry record, and claiming and
 * withdrawing the domains it registers entities under.
 *
 * <p>Nothing here creates an organization implicitly. A {@code GET} on an organization that has not bootstrapped
 * is a 404, which is what lets the portal tell "not onboarded yet" from "onboarded".
 *
 * @author Felix Hellman
 */
@Service
public class OrganizationApiService {

  private final OrganizationService organizationService;
  private final OrganizationRepository organizationRepository;
  private final OrganizationDomainRepository organizationDomainRepository;
  private final OrganizationTrustMarkRepository organizationTrustMarkRepository;
  private final InstancePlacementService instancePlacementService;
  private final RegistryProperties registryProperties;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param organizationService service resolving the calling organization
   * @param organizationRepository repository for organization records
   * @param organizationDomainRepository repository for the domains an organization has claimed
   * @param organizationTrustMarkRepository repository for pre-validated trust mark types
   * @param instancePlacementService service resolving the instance an organization is placed on
   * @param registryProperties the registry configuration, carrying the local-address-range switch
   * @param auditService the audit service organization lifecycle events are emitted through
   */
  public OrganizationApiService(final OrganizationService organizationService,
      final OrganizationRepository organizationRepository,
      final OrganizationDomainRepository organizationDomainRepository,
      final OrganizationTrustMarkRepository organizationTrustMarkRepository,
      final InstancePlacementService instancePlacementService,
      final RegistryProperties registryProperties,
      final RegistryAuditService auditService) {
    this.organizationService = organizationService;
    this.organizationRepository = organizationRepository;
    this.organizationDomainRepository = organizationDomainRepository;
    this.organizationTrustMarkRepository = organizationTrustMarkRepository;
    this.instancePlacementService = instancePlacementService;
    this.registryProperties = registryProperties;
    this.auditService = auditService;
  }

  /**
   * Returns the calling organization's registry record.
   *
   * @param organizationRecord the calling organization
   * @return the organization with its domains and pre-validated trust marks
   * @throws RegistryServerException with {@link ErrorTypes#NOT_FOUND} if the organization has not bootstrapped
   */
  @Transactional(readOnly = true)
  public OrganizationDto getOrganization(final OrganizationRecord organizationRecord) {
    return this.toDto(this.requireOrganization(organizationRecord), organizationRecord);
  }

  /**
   * Bootstraps the calling organization's registry record on the tenant's instance. The organization name comes
   * from the caller's token claim, the legal name from the request body.
   *
   * @param organizationRecord the calling organization
   * @param request the legal name to register
   * @return the created organization
   * @throws RegistryServerException with {@link ErrorTypes#CONFLICT} if the organization already exists
   */
  @Transactional
  public OrganizationDto createOrganization(final OrganizationRecord organizationRecord,
      final CreateOrganizationDto request) {
    ValidateDto.init(organizationRecord).validate(request);

    this.organizationService.find(organizationRecord).ifPresent(existing -> {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Organization %s already exists on tenant %s"
              .formatted(organizationRecord.orgNumber(), organizationRecord.tenant()));
    });

    final Instance instance = this.instancePlacementService.resolveInstance(organizationRecord)
        .orElseThrow(() -> new IllegalArgumentException("No instance was found for the given matcher config"));

    final Organization organization = new Organization();
    organization.setOrganizationId(UUID.randomUUID());
    organization.setOrgNumber(organizationRecord.orgNumber());
    organization.setOrgName(organizationRecord.orgName());
    organization.setLegalName(request.legalName());
    organization.setInstance(instance);

    final Organization saved = this.organizationRepository.save(organization);
    final OrganizationDto dto = this.toDto(saved, organizationRecord);
    this.auditService.organizationCreated(saved.getOrganizationId(), instance.getInstanceId(),
        saved.getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates the calling organization's legal name. Nothing else on the record is settable by the organization
   * itself — the organization number and tenant are path-derived, and the organization name comes from the token.
   *
   * @param organizationRecord the calling organization
   * @param request the legal name to set
   * @return the updated organization
   * @throws RegistryServerException with {@link ErrorTypes#NOT_FOUND} if the organization has not bootstrapped
   */
  @Transactional
  public OrganizationDto updateOrganization(final OrganizationRecord organizationRecord,
      final CreateOrganizationDto request) {
    ValidateDto.init(organizationRecord).validate(request);

    final Organization organization = this.requireOrganization(organizationRecord);
    final OrganizationDto oldData = this.toDto(organization, organizationRecord);
    organization.setLegalName(request.legalName());
    final Organization saved = this.organizationRepository.save(organization);

    final OrganizationDto newData = this.toDto(saved, organizationRecord);
    this.auditService.organizationUpdated(saved.getOrganizationId(),
        saved.getInstance().getInstanceId(), saved.getOrganizationId(), oldData, newData);
    return newData;
  }

  /**
   * Lists every domain the calling organization has claimed, in every status.
   *
   * @param organizationRecord the calling organization
   * @return the organization's domains
   * @throws RegistryServerException with {@link ErrorTypes#NOT_FOUND} if the organization has not bootstrapped
   */
  @Transactional(readOnly = true)
  public List<DomainDto> listDomains(final OrganizationRecord organizationRecord) {
    final Organization organization = this.requireOrganization(organizationRecord);
    return this.organizationDomainRepository
        .findByOrganization_OrganizationId(organization.getOrganizationId())
        .stream()
        .map(OrganizationMapper::toDomainDto)
        .toList();
  }

  /**
   * Claims a domain for the calling organization. A domain the organization has already claimed is a conflict,
   * unless it was rejected — a rejected domain is re-opened as {@link DomainStatus#PENDING} with its review
   * outcome cleared, so an organization can correct and re-submit without the operator having to delete the row.
   *
   * <p>A domain is held by one organization at a time on a tenant: a claim another organization on the same
   * instance holds in {@link DomainStatus#PENDING} or {@link DomainStatus#VALIDATED} makes this one a conflict.
   * The check is on the exact domain string, so a subdomain of somebody else's domain is still claimable. The
   * database enforces the same rule through {@code uk_instance_held_domain}, which is what catches two
   * concurrent claims on the same domain.
   *
   * @param organizationRecord the calling organization
   * @param request the domain to claim
   * @return the created, or re-opened, domain
   * @throws RegistryServerException with {@link ErrorTypes#NOT_FOUND} if the organization has not bootstrapped, or
   *     {@link ErrorTypes#CONFLICT} if the organization already claims the domain, or another organization on the
   *     tenant holds it
   */
  @Transactional
  public DomainDto requestDomain(final OrganizationRecord organizationRecord, final DomainRequestDto request) {
    final Organization organization = this.requireOrganization(organizationRecord);
    final String domainName = normalizeDomain(request.domain());
    ValidateDto.init(organizationRecord)
        .validate(new DomainRequestDto(domainName), this.registryProperties.localAddressRangesEnabled());

    final OrganizationDomain existing = this.organizationDomainRepository
        .findByOrganization_OrganizationIdAndDomain(organization.getOrganizationId(), domainName)
        .orElse(null);

    if (existing != null && existing.getStatus() != DomainStatus.REJECTED) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Domain '%s' is already registered for organization %s"
              .formatted(domainName, organizationRecord.orgNumber()));
    }

    final UUID instanceId = organization.getInstance().getInstanceId();
    if (DomainHolders.heldByAnotherOrganization(
        this.organizationDomainRepository.findByInstance_InstanceIdAndDomainAndStatusIn(instanceId, domainName,
            OrganizationService.REGISTERED_DOMAIN_STATUSES),
        organization.getOrganizationId())) {
      throw new RegistryServerException(ErrorTypes.CONFLICT, DomainHolders.alreadyHeldMessage(domainName));
    }

    final DomainDto oldData = existing == null ? null : OrganizationMapper.toDomainDto(existing);
    final OrganizationDomain domain = existing == null ? new OrganizationDomain() : existing;
    if (existing == null) {
      domain.setDomainId(UUID.randomUUID());
      domain.setOrganization(organization);
      domain.setDomain(domainName);
    }
    domain.setInstance(organization.getInstance());
    domain.setStatus(DomainStatus.PENDING);
    domain.setRejectionReason(null);
    domain.setReviewedAt(null);
    domain.setReviewedBy(null);

    final OrganizationDomain saved = this.saveHeldDomain(domain, domainName);
    final DomainDto newData = OrganizationMapper.toDomainDto(saved);
    this.auditService.organizationDomainRequested(saved.getDomainId(), instanceId,
        organization.getOrganizationId(), oldData, newData);
    return newData;
  }

  /**
   * Withdraws one of the calling organization's domains, whatever its status. A domain ID belonging to another
   * organization is reported as not found, the same way a nonexistent one is.
   *
   * @param organizationRecord the calling organization
   * @param domainId the domain to withdraw
   * @throws RegistryServerException with {@link ErrorTypes#NOT_FOUND} if the organization does not own the domain
   */
  @Transactional
  public void deleteDomain(final OrganizationRecord organizationRecord, final UUID domainId) {
    final Organization organization = this.requireOrganization(organizationRecord);
    final OrganizationDomain domain = this.organizationDomainRepository
        .findByDomainIdAndOrganization_OrganizationId(domainId, organization.getOrganizationId())
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Domain not found: %s".formatted(domainId)));

    final DomainDto deletedData = OrganizationMapper.toDomainDto(domain);
    this.organizationDomainRepository.delete(domain);
    this.auditService.organizationDomainDeleted(domainId, organization.getInstance().getInstanceId(),
        organization.getOrganizationId(), deletedData);
  }

  /**
   * Persists a claim, translating the tenant-wide unique key into the same conflict the pre-check reports. Two
   * organizations claiming the same domain at the same time both pass the pre-check; the database decides, and
   * the loser must be told it lost the domain rather than be handed a generic constraint error.
   *
   * @param domain the claim to persist
   * @param domainName the domain being claimed, for the conflict message
   * @return the persisted claim
   * @throws RegistryServerException with {@link ErrorTypes#CONFLICT} if another organization won the domain
   */
  private OrganizationDomain saveHeldDomain(final OrganizationDomain domain, final String domainName) {
    try {
      return this.organizationDomainRepository.saveAndFlush(domain);
    }
    catch (final DataIntegrityViolationException e) {
      throw new RegistryServerException(ErrorTypes.CONFLICT, DomainHolders.alreadyHeldMessage(domainName));
    }
  }

  /**
   * Normalizes a domain to the form it is stored and compared in: trimmed, lower-cased and without a trailing
   * root dot.
   *
   * @param domain the domain as supplied by the caller, may be {@code null}
   * @return the normalized domain, or {@code null} if none was supplied
   */
  static String normalizeDomain(final String domain) {
    if (domain == null) {
      return null;
    }
    final String trimmed = domain.trim().toLowerCase(Locale.ROOT);
    return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
  }

  /**
   * Resolves the calling organization, or reports it as not found. The organization API never auto-creates.
   *
   * @param organizationRecord the calling organization
   * @return the organization
   */
  private Organization requireOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.find(organizationRecord)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Organization %s not found on tenant %s"
                .formatted(organizationRecord.orgNumber(), organizationRecord.tenant())));
  }

  private OrganizationDto toDto(final Organization organization, final OrganizationRecord organizationRecord) {
    return OrganizationMapper.toOrganizationDto(organization, organizationRecord.tenant(),
        this.organizationDomainRepository.findByOrganization_OrganizationId(organization.getOrganizationId()),
        this.organizationTrustMarkRepository
            .findByOrganization_OrganizationId(organization.getOrganizationId())
            .stream()
            .map(OrganizationTrustMark::getTrustMarkType)
            .toList());
  }
}
