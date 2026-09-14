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
package se.swedenconnect.oidf.registry.registrations.service;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrgRightsService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.organization.dto.AdminDomainDto;
import se.swedenconnect.oidf.registry.organization.dto.AdminOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRejectionResultDto;
import se.swedenconnect.oidf.registry.organization.dto.OrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.PreValidatedTrustMarksDto;
import se.swedenconnect.oidf.registry.organization.mapper.OrganizationMapper;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.model.OrganizationTrustMark;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationDomainRepository;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationRepository;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationTrustMarkRepository;
import se.swedenconnect.oidf.registry.organization.service.DomainHolders;
import se.swedenconnect.oidf.registry.organization.service.DomainMatcher;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationRejectionReasons;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RegistrationAdminService}.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Service
public class RegistrationAdminServiceImpl implements RegistrationAdminService {

  /** Statuses a registration can be in and still be affected by a domain rejection. */
  private static final List<RegistrationStatus> IN_FLIGHT_STATUSES =
      List.of(RegistrationStatus.STARTED, RegistrationStatus.PENDING_APPROVAL);

  private final RegistrationRepository registrationRepository;
  private final EntityConfigService entityConfigService;
  private final RegistrationFlowService registrationFlowService;
  private final OrganizationService organizationService;
  private final OrganizationRepository organizationRepository;
  private final OrganizationDomainRepository organizationDomainRepository;
  private final OrganizationTrustMarkRepository organizationTrustMarkRepository;
  private final TrustMarkRepository trustMarkRepository;
  private final InstancePlacementService instancePlacementService;
  private final TaImRepository taImRepository;
  private final OrgRightsService orgRightsService;
  private final AuditorAware<String> auditorAware;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param registrationRepository repository for registration records
   * @param entityConfigService service for checking hosted entities
   * @param registrationFlowService service for resuming pipeline execution on step approval
   * @param organizationService service for resolving the calling organization
   * @param organizationRepository repository for organization records
   * @param organizationDomainRepository repository for the domains organizations have claimed
   * @param organizationTrustMarkRepository repository for pre-validated trust mark types
   * @param trustMarkRepository repository the trust mark types issued on an instance are read from
   * @param instancePlacementService service resolving the instance a tenant is backed by
   * @param taImRepository repository used to establish that the caller owns a trust anchor
   * @param orgRightsService service used to recognise a superuser caller
   * @param auditorAware supplies the principal recorded as the reviewer
   * @param auditService the audit service domain review events are emitted through
   */
  public RegistrationAdminServiceImpl(final RegistrationRepository registrationRepository,
      final EntityConfigService entityConfigService,
      final RegistrationFlowService registrationFlowService,
      final OrganizationService organizationService,
      final OrganizationRepository organizationRepository,
      final OrganizationDomainRepository organizationDomainRepository,
      final OrganizationTrustMarkRepository organizationTrustMarkRepository,
      final TrustMarkRepository trustMarkRepository,
      final InstancePlacementService instancePlacementService,
      final TaImRepository taImRepository,
      final OrgRightsService orgRightsService,
      final AuditorAware<String> auditorAware,
      final RegistryAuditService auditService) {
    this.registrationRepository = registrationRepository;
    this.entityConfigService = entityConfigService;
    this.registrationFlowService = registrationFlowService;
    this.organizationService = organizationService;
    this.organizationRepository = organizationRepository;
    this.organizationDomainRepository = organizationDomainRepository;
    this.organizationTrustMarkRepository = organizationTrustMarkRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.instancePlacementService = instancePlacementService;
    this.taImRepository = taImRepository;
    this.orgRightsService = orgRightsService;
    this.auditorAware = auditorAware;
    this.auditService = auditService;
  }

  /**
   * Finds a registration by ID, verifying it is connected to an intermediate owned by the calling organization. Both
   * "no such registration" and "registration belongs to another organization" collapse to the same not-found error, so
   * a foreign registration ID is indistinguishable from a nonexistent one.
   *
   * @param organizationRecord the calling organization
   * @param registrationId the registration ID
   * @return the owned registration
   */
  private Registration findOwnedRegistrationOrThrow(final OrganizationRecord organizationRecord,
      final UUID registrationId) {
    final UUID organizationId = this.organizationService.find(organizationRecord)
        .map(Organization::getOrganizationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
    return this.registrationRepository
        .findByRegistrationIdAndFlowAssignment_TaIm_Organization_OrganizationId(registrationId, organizationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
  }

  @Override
  public long countPending(final OrganizationRecord organizationRecord, final UUID taimId) {
    return this.organizationService.find(organizationRecord)
        .map(org -> this.registrationRepository
            .countByFlowAssignment_TaIm_TaImIdAndFlowAssignment_TaIm_Organization_OrganizationIdAndStatus(
                taimId, org.getOrganizationId(), RegistrationStatus.PENDING_APPROVAL))
        .orElse(0L);
  }

  @Override
  @Transactional
  public RegistrationDto reject(final OrganizationRecord organizationRecord, final UUID registrationId,
      final String rejectionReason) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    if (reg.getStatus() != RegistrationStatus.PENDING_APPROVAL) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Registration %s is not pending approval".formatted(registrationId));
    }
    reg.setStatus(RegistrationStatus.REJECTED);
    reg.setRejectionReason(rejectionReason);
    reg.setReviewedAt(LocalDateTime.now());
    this.registrationRepository.save(reg);
    final List<HostedEntityDto> hostedEntities = this.entityConfigService.listHostedEntity(reg.getEntityId());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    return RegistrationMapper.toRegistrationDto(reg, isHosted, hostedMetadata);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationDto> listRegistrationsConnectedToThisOrgIM(final OrganizationRecord organizationRecord) {
    final Map<String, Map<String, Object>> hostedMetadataByEntityId = new HashMap<>();
    this.entityConfigService.listHostedEntity((String) null)
        .forEach(h -> hostedMetadataByEntityId.put(h.getEntityIdentifier(), h.getMetadata()));
    final List<Registration> allRegs = this.organizationService.find(organizationRecord)
        .map(org -> this.registrationRepository.findByFlowAssignment_TaIm_Organization_OrganizationId(
            org.getOrganizationId()))
        .orElse(List.of());
    final Map<UUID, Map<String, RegistrationStatus>> tmStatusByParent = allRegs.stream()
        .filter(r -> r.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE)
        .filter(r -> r.getParentRegistration() != null)
        .collect(Collectors.groupingBy(
            r -> r.getParentRegistration().getRegistrationId(),
            Collectors.toMap(Registration::getEntityId, Registration::getStatus)));
    return allRegs.stream()
        .map(r -> RegistrationMapper.toRegistrationDto(r,
            hostedMetadataByEntityId.containsKey(r.getEntityId()),
            hostedMetadataByEntityId.get(r.getEntityId()),
            tmStatusByParent.getOrDefault(r.getRegistrationId(), Map.of())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RegistrationDto getRegistrationById(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    final List<HostedEntityDto> hostedEntities = this.entityConfigService.listHostedEntity(reg.getEntityId());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    final Map<String, RegistrationStatus> tmStatusByType =
        this.registrationRepository.findByParentRegistration_RegistrationId(registrationId)
            .stream()
            .collect(Collectors.toMap(Registration::getEntityId, Registration::getStatus));
    return RegistrationMapper.toRegistrationDto(reg, isHosted, hostedMetadata, tmStatusByType);
  }

  @Override
  @Transactional
  public RegistrationDto approveStep(final OrganizationRecord organizationRecord, final UUID registrationId,
      final int stepIndex) {
    final Registration reg = this.findOwnedRegistrationOrThrow(organizationRecord, registrationId);
    this.registrationFlowService.approveStep(reg, stepIndex);
    return this.getRegistrationById(organizationRecord, registrationId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminDomainDto> listDomains(final OrganizationRecord organizationRecord, final DomainStatus status) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    final List<OrganizationDomain> domains = status == null
        ? this.organizationDomainRepository.findByOrganization_Instance_InstanceId(instanceId)
        : this.organizationDomainRepository.findByOrganization_Instance_InstanceIdAndStatus(instanceId, status);
    return domains.stream().map(OrganizationMapper::toAdminDomainDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countPendingDomains(final OrganizationRecord organizationRecord) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    return this.organizationDomainRepository
        .countByOrganization_Instance_InstanceIdAndStatus(instanceId, DomainStatus.PENDING);
  }

  @Override
  @Transactional
  public AdminDomainDto approveDomain(final OrganizationRecord organizationRecord, final UUID domainId) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    final OrganizationDomain domain = this.findReviewableDomainOrThrow(domainId, instanceId);
    requirePending(domain);
    this.requireNotHeldByAnotherOrganization(instanceId, domain);

    final DomainDto oldData = OrganizationMapper.toDomainDto(domain);
    domain.setStatus(DomainStatus.VALIDATED);
    domain.setRejectionReason(null);
    domain.setReviewedAt(LocalDateTime.now());
    domain.setReviewedBy(this.currentReviewer(organizationRecord));
    final OrganizationDomain saved = this.organizationDomainRepository.save(domain);

    this.auditService.organizationDomainApproved(saved.getDomainId(), instanceId,
        saved.getOrganization().getOrganizationId(), oldData, OrganizationMapper.toDomainDto(saved));
    return OrganizationMapper.toAdminDomainDto(saved);
  }

  @Override
  @Transactional
  public DomainRejectionResultDto rejectDomain(final OrganizationRecord organizationRecord, final UUID domainId,
      final String rejectionReason) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    final OrganizationDomain domain = this.findReviewableDomainOrThrow(domainId, instanceId);
    requirePending(domain);

    final DomainDto oldData = OrganizationMapper.toDomainDto(domain);
    final String reviewer = this.currentReviewer(organizationRecord);
    final LocalDateTime reviewedAt = LocalDateTime.now();
    domain.setStatus(DomainStatus.REJECTED);
    domain.setRejectionReason(rejectionReason);
    domain.setReviewedAt(reviewedAt);
    domain.setReviewedBy(reviewer);
    final OrganizationDomain saved = this.organizationDomainRepository.save(domain);

    final List<UUID> cascaded = this.cascadeDomainRejection(saved, reviewer, reviewedAt);

    this.auditService.organizationDomainRejected(saved.getDomainId(), instanceId,
        saved.getOrganization().getOrganizationId(), oldData, OrganizationMapper.toDomainDto(saved));
    return OrganizationMapper.toDomainRejectionResultDto(saved, cascaded);
  }

  @Override
  @Transactional
  public OrganizationDto replacePreValidatedTrustMarks(final OrganizationRecord organizationRecord,
      final String targetOrgNumber, final PreValidatedTrustMarksDto request) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    ValidateDto.init(organizationRecord).validate(request);
    final Organization target = this.findOrganizationOnInstanceOrThrow(instanceId, targetOrgNumber);

    final OrganizationDto oldData = this.toOrganizationDto(target);

    this.organizationTrustMarkRepository.deleteByOrganization_OrganizationId(target.getOrganizationId());
    this.organizationTrustMarkRepository.flush();
    Optional.ofNullable(request.preValidatedTrustMarks()).orElse(List.of())
        .forEach(trustMarkType -> {
          final OrganizationTrustMark entity = new OrganizationTrustMark();
          entity.setOrganization(target);
          entity.setTrustMarkType(trustMarkType);
          this.organizationTrustMarkRepository.save(entity);
        });

    final OrganizationDto newData = this.toOrganizationDto(target);
    this.auditService.organizationUpdated(target.getOrganizationId(), instanceId,
        target.getOrganizationId(), oldData, newData);
    return newData;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminOrganizationDto> listOrganizations(final OrganizationRecord organizationRecord) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);

    final Map<UUID, List<OrganizationDomain>> domainsByOrganization = this.organizationDomainRepository
        .findByOrganization_Instance_InstanceId(instanceId)
        .stream()
        .collect(Collectors.groupingBy(domain -> domain.getOrganization().getOrganizationId()));
    final Map<UUID, List<String>> trustMarksByOrganization = this.organizationTrustMarkRepository
        .findByOrganization_Instance_InstanceId(instanceId)
        .stream()
        .collect(Collectors.groupingBy(trustMark -> trustMark.getOrganization().getOrganizationId(),
            Collectors.mapping(OrganizationTrustMark::getTrustMarkType, Collectors.toList())));

    return this.organizationRepository.findByInstance_InstanceId(instanceId)
        .stream()
        .map(organization -> {
          final List<OrganizationDomain> domains =
              domainsByOrganization.getOrDefault(organization.getOrganizationId(), List.of());
          final long pending = domains.stream()
              .filter(domain -> domain.getStatus() == DomainStatus.PENDING)
              .count();
          return OrganizationMapper.toAdminOrganizationDto(organization, domains.size(), pending,
              sortedDistinctTrustMarkTypes(
                  trustMarksByOrganization.getOrDefault(organization.getOrganizationId(), List.of())));
        })
        .sorted(byDisplayName())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public OrganizationDto getOrganizationOnTenant(final OrganizationRecord organizationRecord,
      final String targetOrgNumber) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    return this.toOrganizationDto(this.findOrganizationOnInstanceOrThrow(instanceId, targetOrgNumber));
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> listTrustMarkTypes(final OrganizationRecord organizationRecord) {
    final UUID instanceId = this.requireReviewerInstance(organizationRecord);
    return sortedDistinctTrustMarkTypes(this.trustMarkRepository.findTrustmarkTypesByInstanceId(instanceId));
  }

  /**
   * Orders organizations the way the operator reads them: by the name shown in the listing -- the legal name where
   * there is one, the token-claim name otherwise -- case-insensitively, with the nameless ones last and the
   * organization number breaking ties so the order is stable across calls.
   *
   * @return the listing order
   */
  static Comparator<AdminOrganizationDto> byDisplayName() {
    final Function<AdminOrganizationDto, String> displayName = organization ->
        Optional.ofNullable(organization.legalName())
            .filter(name -> !name.isBlank())
            .orElseGet(() -> Optional.ofNullable(organization.orgName()).filter(name -> !name.isBlank())
                .orElse(null));
    return Comparator
        .comparing(displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        .thenComparing(AdminOrganizationDto::orgNumber,
            Comparator.nullsLast(Comparator.naturalOrder()));
  }

  /**
   * Collapses trust mark types to the form the API hands out: no nulls, no blanks, each type once, sorted. The
   * database can hold the same type for several organizations, and a query spanning a whole instance therefore
   * returns it more than once.
   *
   * @param trustMarkTypes the types as read from the database
   * @return the distinct, sorted types
   */
  static List<String> sortedDistinctTrustMarkTypes(final Collection<String> trustMarkTypes) {
    return trustMarkTypes.stream()
        .filter(Objects::nonNull)
        .filter(trustMarkType -> !trustMarkType.isBlank())
        .distinct()
        .sorted()
        .toList();
  }

  /**
   * Resolves an organization by number on the instance the operator administers. An organization number that is
   * not placed on this instance is reported as missing, never as somebody else's.
   *
   * @param instanceId the instance the organization must be placed on
   * @param targetOrgNumber the organization number to resolve
   * @return the organization
   */
  private Organization findOrganizationOnInstanceOrThrow(final UUID instanceId, final String targetOrgNumber) {
    return this.organizationRepository
        .findByInstance_InstanceIdAndOrgNumber(instanceId, targetOrgNumber)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Organization not found: %s".formatted(targetOrgNumber)));
  }

  /**
   * Rejects every in-flight registration of the domain's organization that depended on the rejected domain and
   * on nothing else: a registration whose entity identifier is still covered by another of the organization's
   * PENDING/VALIDATED domains is left alone. Trust mark subordinate registrations are never selected directly --
   * they follow their parent.
   *
   * @param rejected the domain that was just rejected
   * @param reviewer the principal recorded as having rejected it
   * @param reviewedAt the timestamp recorded on the rejected registrations
   * @return the identifiers of the registrations this rejection cascaded to
   */
  private List<UUID> cascadeDomainRejection(final OrganizationDomain rejected, final String reviewer,
      final LocalDateTime reviewedAt) {
    final UUID organizationId = rejected.getOrganization().getOrganizationId();
    final List<String> remainingDomains = this.organizationDomainRepository
        .findByOrganization_OrganizationIdAndStatusIn(organizationId, OrganizationService.REGISTERED_DOMAIN_STATUSES)
        .stream()
        .filter(domain -> !domain.getDomainId().equals(rejected.getDomainId()))
        .map(OrganizationDomain::getDomain)
        .toList();

    final List<UUID> cascaded = new ArrayList<>();
    this.registrationRepository.findByOrganization_OrganizationIdAndStatusIn(organizationId, IN_FLIGHT_STATUSES)
        .stream()
        .filter(registration -> isCascadeTarget(registration, rejected.getDomain(), remainingDomains))
        .forEach(registration -> {
          cascaded.add(this.rejectAsNotAcceptedDomain(registration, reviewer, reviewedAt));
          this.registrationRepository.findByParentRegistration_RegistrationId(registration.getRegistrationId())
              .stream()
              .filter(child -> child.getStatus() != RegistrationStatus.APPROVED
                  && child.getStatus() != RegistrationStatus.REJECTED)
              .forEach(child -> cascaded.add(this.rejectAsNotAcceptedDomain(child, reviewer, reviewedAt)));
        });
    return List.copyOf(cascaded);
  }

  /**
   * Decides whether a single registration is carried along by the rejection of a domain. A registration is a
   * target when it is still in flight, is not a trust mark subordinate (those follow their parent rather than
   * being selected on their own), and its entity identifier's host is covered by the rejected domain but by none
   * of the organization's remaining registered domains.
   *
   * @param registration the registration to test
   * @param rejectedDomain the domain that was rejected
   * @param remainingDomains the organization's other PENDING/VALIDATED domains
   * @return true if the registration should be rejected along with the domain
   */
  static boolean isCascadeTarget(final Registration registration, final String rejectedDomain,
      final Collection<String> remainingDomains) {
    if (registration.getRegistrationType() == RegistrationType.TRUST_MARK_SUBORDINATE) {
      return false;
    }
    if (!IN_FLIGHT_STATUSES.contains(registration.getStatus())) {
      return false;
    }
    return DomainMatcher.hostOf(registration.getEntityId())
        .filter(host -> DomainMatcher.matches(host, rejectedDomain))
        .filter(host -> !DomainMatcher.matchesAny(host, remainingDomains))
        .isPresent();
  }

  /**
   * Requires that no other organization on the tenant already holds the domain being approved. The claim path
   * refuses this already, and the {@code uk_instance_held_domain} unique key refuses it in the database, so this
   * can only bite on rows that predate that rule -- but approving a second holder is exactly what the rule
   * exists to prevent, so the reviewer is stopped here too.
   *
   * @param instanceId the instance the review is scoped to
   * @param domain the domain being approved
   */
  private void requireNotHeldByAnotherOrganization(final UUID instanceId, final OrganizationDomain domain) {
    if (DomainHolders.heldByAnotherOrganization(
        this.organizationDomainRepository.findByInstance_InstanceIdAndDomainAndStatusIn(instanceId,
            domain.getDomain(), OrganizationService.REGISTERED_DOMAIN_STATUSES),
        domain.getOrganization().getOrganizationId())) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          DomainHolders.alreadyHeldMessage(domain.getDomain()));
    }
  }

  /**
   * Requires that a domain is still awaiting review. Approving or rejecting a domain that has already been
   * reviewed is a conflict, not a second review — a domain goes back to PENDING only by being requested again.
   *
   * @param domain the domain being reviewed
   */
  static void requirePending(final OrganizationDomain domain) {
    if (domain.getStatus() != DomainStatus.PENDING) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Domain %s is not pending review".formatted(domain.getDomainId()));
    }
  }

  private UUID rejectAsNotAcceptedDomain(final Registration registration, final String reviewer,
      final LocalDateTime reviewedAt) {
    registration.setStatus(RegistrationStatus.REJECTED);
    registration.setRejectionReason(RegistrationRejectionReasons.NOT_ACCEPTED_DOMAIN);
    registration.setReviewedAt(reviewedAt);
    registration.setReviewedBy(reviewer);
    this.registrationRepository.save(registration);
    return registration.getRegistrationId();
  }

  /**
   * Resolves the instance whose domains the caller may review, having established that the caller is a tenant
   * operator: a superuser, or an organization owning at least one trust anchor on the tenant's instance. A caller
   * that is neither is told the resource does not exist, the same convention foreign registrations follow.
   *
   * @param organizationRecord the calling organization
   * @return the instance the review is scoped to
   */
  private UUID requireReviewerInstance(final OrganizationRecord organizationRecord) {
    final UUID instanceId = this.instancePlacementService.resolveInstance(organizationRecord)
        .map(Instance::getInstanceId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Unknown tenant: %s".formatted(organizationRecord.tenant())));

    if (this.orgRightsService.isCurrentUserSuperuser()) {
      return instanceId;
    }

    final boolean ownsTrustAnchor = this.organizationService.find(organizationRecord)
        .map(organization -> !this.taImRepository
            .findByOrganizationIdAndModuleType(organization.getOrganizationId(), ModuleType.TRUSTANCHOR)
            .isEmpty())
        .orElse(false);

    if (!ownsTrustAnchor) {
      throw new RegistryServerException(ErrorTypes.NOT_FOUND,
          "No domain review available for organization %s on tenant %s"
              .formatted(organizationRecord.orgNumber(), organizationRecord.tenant()));
    }
    return instanceId;
  }

  private OrganizationDomain findReviewableDomainOrThrow(final UUID domainId, final UUID instanceId) {
    return this.organizationDomainRepository
        .findByDomainIdAndOrganization_Instance_InstanceId(domainId, instanceId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Domain not found: %s".formatted(domainId)));
  }

  private String currentReviewer(final OrganizationRecord organizationRecord) {
    return this.auditorAware.getCurrentAuditor().orElse(organizationRecord.orgNumber());
  }

  private OrganizationDto toOrganizationDto(final Organization organization) {
    return OrganizationMapper.toOrganizationDto(organization,
        this.instancePlacementService.resolveTenantForPlacedOrg(organization).orElse(null),
        this.organizationDomainRepository.findByOrganization_OrganizationId(organization.getOrganizationId()),
        this.organizationTrustMarkRepository
            .findByOrganization_OrganizationId(organization.getOrganizationId())
            .stream()
            .map(OrganizationTrustMark::getTrustMarkType)
            .toList());
  }
}
