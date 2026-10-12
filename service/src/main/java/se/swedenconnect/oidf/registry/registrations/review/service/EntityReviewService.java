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

import net.minidev.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.service.DomainMatcher;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationStepRepository;
import se.swedenconnect.oidf.registry.registrationflow.dto.Mapper;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.PersistedContext;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.SerializableList;
import se.swedenconnect.oidf.registry.registrationflow.process.StepDefinition;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationType;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;
import se.swedenconnect.oidf.registry.registrations.review.dto.EntityReviewDto;
import se.swedenconnect.oidf.registry.registrations.review.dto.EntityReviewState;
import se.swedenconnect.oidf.registry.registrations.review.dto.EntityReviewSummaryDto;
import se.swedenconnect.oidf.registry.registrations.review.dto.ReviewDecisionRequestDto;
import se.swedenconnect.oidf.registry.registrations.review.dto.ReviewItemDto;
import se.swedenconnect.oidf.registry.registrations.review.dto.ReviewItemState;
import se.swedenconnect.oidf.registry.registrations.review.dto.ReviewSessionDto;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReview;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReviewDecision;
import se.swedenconnect.oidf.registry.registrations.review.model.EntityReviewStatus;
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewDecision;
import se.swedenconnect.oidf.registry.registrations.review.model.ReviewItemType;
import se.swedenconnect.oidf.registry.registrations.review.repository.EntityReviewDecisionRepository;
import se.swedenconnect.oidf.registry.registrations.review.repository.EntityReviewRepository;
import se.swedenconnect.oidf.registry.registrations.service.RegistrationAdminService;

import java.time.LocalDateTime;

import static se.swedenconnect.oidf.registry.infrastructure.persistence.Timestamps.toOffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reviews everything an entity waits for in one session: its domain, then the trust marks it asked for, then the
 * steps of its own registration. The items are not stored: they are read from the domains and registrations each
 * time, so a decision taken elsewhere (another entity's review deciding a shared domain, the single-step endpoints)
 * shows up at once. The session records who started it and every decision taken in it, deferrals included.
 *
 * <p>Decisions go through {@link RegistrationAdminService}, so a decision taken here has exactly the effect, checks,
 * cascades and audit events it has there.</p>
 *
 * @author Felix Hellman
 */
@Service
public class EntityReviewService {

  private static final List<RegistrationStatus> OPEN_STATUSES =
      List.of(RegistrationStatus.STARTED, RegistrationStatus.PENDING_APPROVAL);
  private static final Set<ReviewItemState> UNDECIDED = Set.of(ReviewItemState.PENDING, ReviewItemState.WAITING);

  private final RegistrationRepository registrationRepository;
  private final EntityReviewRepository reviewRepository;
  private final EntityReviewDecisionRepository decisionRepository;
  private final RegistrationAdminService registrationAdminService;
  private final ReviewOrder reviewOrder;
  private final RegistrationStepRepository registrationStepRepository;
  private final EntityConfigService entityConfigService;

  /**
   * Constructor.
   *
   * @param registrationRepository repository of registrations
   * @param reviewRepository repository of review sessions
   * @param decisionRepository repository of review decisions
   * @param registrationAdminService service carrying out approvals and rejections
   * @param reviewOrder the order items are reviewed in
   * @param registrationStepRepository the defined steps, to show the review steps still to come
   * @param entityConfigService service telling hosted entities apart
   */
  public EntityReviewService(final RegistrationRepository registrationRepository,
      final EntityReviewRepository reviewRepository,
      final EntityReviewDecisionRepository decisionRepository,
      final RegistrationAdminService registrationAdminService,
      final ReviewOrder reviewOrder,
      final RegistrationStepRepository registrationStepRepository,
      final EntityConfigService entityConfigService) {
    this.registrationRepository = registrationRepository;
    this.reviewRepository = reviewRepository;
    this.decisionRepository = decisionRepository;
    this.registrationAdminService = registrationAdminService;
    this.reviewOrder = reviewOrder;
    this.registrationStepRepository = registrationStepRepository;
    this.entityConfigService = entityConfigService;
  }

  /**
   * Lists the entities registered on the tenant the caller operates.
   *
   * @param organizationRecord the calling operator
   * @param openOnly {@code true} for entities that wait for a decision or have an open review; {@code false} for all
   * @return the entities, newest submission first
   */
  @Transactional(readOnly = true)
  public List<EntityReviewSummaryDto> list(final OrganizationRecord organizationRecord, final boolean openOnly) {
    final UUID instanceId = this.registrationAdminService.requireReviewerInstance(organizationRecord);
    final Set<String> hosted = this.entityConfigService.listHostedEntity((String) null).stream()
        .map(HostedEntityDto::getEntityIdentifier)
        .collect(Collectors.toSet());
    final Map<UUID, EntityReview> openReviews = this.reviewRepository.findByInstanceId(instanceId).stream()
        .filter(review -> review.getStatus() == EntityReviewStatus.IN_PROGRESS)
        .collect(Collectors.toMap(review -> review.getRegistration().getRegistrationId(), review -> review,
            (first, second) -> first));

    final List<EntityReviewSummaryDto> summaries = new ArrayList<>();
    for (final Registration entity : this.registrationRepository.findByOrganization_Instance_InstanceId(instanceId)) {
      if (entity.getRegistrationType() != RegistrationType.SUBORDINATE) {
        continue;
      }
      final EntityReview open = openReviews.get(entity.getRegistrationId());
      final List<ReviewItemDto> items = this.items(entity, open, false);
      final EntityReviewState state = state(open, items);
      if (openOnly && state == EntityReviewState.COMPLETED) {
        continue;
      }
      summaries.add(new EntityReviewSummaryDto(entity.getRegistrationId(), entity.getEntityId(),
          organizationName(entity.getOrganization()), entity.getOrganization().getOrgNumber(),
          toOffsetDateTime(entity.getCreatedDate()), entity.getStatus().name(), hosted.contains(entity.getEntityId()),
          count(items, ReviewItemType.DOMAIN), count(items, ReviewItemType.TRUST_MARK),
          count(items, ReviewItemType.ENTITY), state,
          open == null ? null : open.getStartedBy(), open == null ? null : toOffsetDateTime(open.getStartedAt()),
          open == null ? 0 : this.decidedIn(open)));
    }
    summaries.sort(Comparator.comparing(EntityReviewSummaryDto::submittedAt,
        Comparator.nullsLast(Comparator.reverseOrder())));
    return summaries;
  }

  /**
   * Returns everything reviewed for one entity, in review order, with what the operator reviews for each item.
   *
   * @param organizationRecord the calling operator
   * @param registrationId the entity's registration
   * @return the entity's review
   */
  @Transactional(readOnly = true)
  public EntityReviewDto get(final OrganizationRecord organizationRecord, final UUID registrationId) {
    return this.toDto(this.findEntity(organizationRecord, registrationId));
  }

  /**
   * Starts a review of the entity, or returns the review already open. Any operator of the tenant may continue a
   * review another started.
   *
   * @param organizationRecord the calling operator
   * @param registrationId the entity's registration
   * @return the entity's review
   * @throws RegistryServerException conflict, if nothing waits for a decision
   */
  @Transactional
  public EntityReviewDto start(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final Registration entity = this.findEntity(organizationRecord, registrationId);
    if (this.openReview(entity).isEmpty()) {
      if (this.items(entity, null, false).stream().noneMatch(item -> UNDECIDED.contains(item.state()))) {
        throw new RegistryServerException(ErrorTypes.CONFLICT,
            "Nothing waits for review for %s".formatted(entity.getEntityId()));
      }
      final EntityReview review = new EntityReview();
      review.setReviewId(UUID.randomUUID());
      review.setRegistration(entity);
      review.setInstanceId(this.registrationAdminService.requireReviewerInstance(organizationRecord));
      review.setStatus(EntityReviewStatus.IN_PROGRESS);
      review.setStartedBy(this.registrationAdminService.currentReviewer(organizationRecord));
      review.setStartedAt(LocalDateTime.now());
      this.reviewRepository.save(review);
    }
    return this.toDto(entity);
  }

  /**
   * Decides one item of an open review. Approving or rejecting goes through the same service as the single-item
   * endpoints; deferring only records the deferral. The review completes when nothing is left to decide.
   *
   * @param organizationRecord the calling operator
   * @param registrationId the entity's registration
   * @param request the decision
   * @return the entity's review after the decision
   * @throws RegistryServerException conflict, if no review is open, the item is not one of this review or is already
   *     decided, or an earlier item must be decided first; invalid parameter, if a rejection has no reason
   */
  @Transactional
  public EntityReviewDto decide(final OrganizationRecord organizationRecord, final UUID registrationId,
      final ReviewDecisionRequestDto request) {
    final Registration entity = this.findEntity(organizationRecord, registrationId);
    final EntityReview review = this.openReview(entity).orElseThrow(() -> new RegistryServerException(
        ErrorTypes.CONFLICT, "Start the review of %s first".formatted(entity.getEntityId())));
    if (request == null || request.itemType() == null || request.itemId() == null || request.decision() == null) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER, "itemType, itemId and decision are required");
    }
    if (request.decision() == ReviewDecision.REJECTED && (request.reason() == null || request.reason().isBlank())) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER, "A rejection needs a reason");
    }

    final ReviewItemDto item = this.items(entity, review, false).stream()
        .filter(candidate -> candidate.type() == request.itemType()
            && candidate.itemId().equals(request.itemId())
            && (candidate.type() == ReviewItemType.DOMAIN
            || Objects.equals(candidate.stepIndex(), request.stepIndex())))
        .filter(candidate -> UNDECIDED.contains(candidate.state()))
        .findFirst()
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.CONFLICT,
            "The item is not waiting for a decision in this review; reload the review"));
    if (item.state() == ReviewItemState.WAITING && request.decision() == ReviewDecision.APPROVED) {
      throw new RegistryServerException(ErrorTypes.CONFLICT, item.waitingFor());
    }

    switch (request.decision()) {
    case APPROVED -> {
      if (item.type() == ReviewItemType.DOMAIN) {
        this.registrationAdminService.approveDomain(organizationRecord, item.itemId());
      }
      else {
        this.registrationAdminService.approveStep(organizationRecord, item.itemId(), item.stepIndex());
      }
    }
    case REJECTED -> {
      if (item.type() == ReviewItemType.DOMAIN) {
        this.registrationAdminService.rejectDomain(organizationRecord, item.itemId(), request.reason());
      }
      else {
        this.registrationAdminService.reject(organizationRecord, item.itemId(), request.reason());
      }
    }
    case DEFERRED -> {
    }
    }

    final EntityReviewDecision decision = new EntityReviewDecision();
    decision.setDecisionId(UUID.randomUUID());
    decision.setReview(review);
    decision.setItemType(item.type());
    decision.setItemId(item.itemId());
    decision.setStepIndex(item.stepIndex());
    decision.setStepName(item.type() == ReviewItemType.DOMAIN ? null : item.title());
    decision.setDecision(request.decision());
    decision.setReason(request.reason());
    decision.setDecidedBy(this.registrationAdminService.currentReviewer(organizationRecord));
    decision.setDecidedAt(LocalDateTime.now());
    this.decisionRepository.save(decision);

    final Registration reloaded = this.registrationRepository.findById(entity.getRegistrationId()).orElseThrow();
    if (this.items(reloaded, review, false).stream().noneMatch(next -> UNDECIDED.contains(next.state()))) {
      review.setStatus(EntityReviewStatus.COMPLETED);
      review.setCompletedAt(LocalDateTime.now());
      this.reviewRepository.save(review);
    }
    return this.toDto(reloaded);
  }

  private Registration findEntity(final OrganizationRecord organizationRecord, final UUID registrationId) {
    final UUID instanceId = this.registrationAdminService.requireReviewerInstance(organizationRecord);
    return this.registrationRepository.findByRegistrationIdAndOrganization_Instance_InstanceId(registrationId,
            instanceId)
        .filter(registration -> registration.getRegistrationType() == RegistrationType.SUBORDINATE)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Entity registration not found: %s".formatted(registrationId)));
  }

  private Optional<EntityReview> openReview(final Registration entity) {
    return this.reviewRepository.findFirstByRegistration_RegistrationIdAndStatus(entity.getRegistrationId(),
        EntityReviewStatus.IN_PROGRESS);
  }

  private EntityReviewDto toDto(final Registration entity) {
    final Optional<EntityReview> open = this.openReview(entity);
    final EntityReview shown = open.orElseGet(() -> this.reviewRepository
        .findFirstByRegistration_RegistrationIdOrderByStartedAtDesc(entity.getRegistrationId()).orElse(null));
    final List<ReviewItemDto> items = this.items(entity, open.orElse(null), true);
    return new EntityReviewDto(entity.getRegistrationId(), entity.getEntityId(),
        organizationName(entity.getOrganization()), entity.getOrganization().getOrgNumber(),
        toOffsetDateTime(entity.getCreatedDate()), entity.getStatus().name(),
        !this.entityConfigService.listHostedEntity(entity.getEntityId()).isEmpty(),
        state(open.orElse(null), items), shown == null ? null : ReviewSessionDto.of(shown), items);
  }

  /**
   * Builds the entity's items in review order.
   *
   * @param entity the entity's registration
   * @param open the open review session, to mark deferred items; may be {@code null}
   * @param withPreviews whether to include what the operator reviews for each item
   */
  private List<ReviewItemDto> items(final Registration entity, final EntityReview open,
      final boolean withPreviews) {
    final Set<String> deferred = open == null ? Set.of() : this.deferredIn(open);
    final List<ReviewItemDto> items = new ArrayList<>();

    for (final OrganizationDomain domain : this.reviewOrder.domainsOf(entity)) {
      final ReviewItemState state = switch (domain.getStatus()) {
        case PENDING -> ReviewItemState.PENDING;
        case VALIDATED -> ReviewItemState.APPROVED;
        case REJECTED -> ReviewItemState.REJECTED;
      };
      final String decidedInReviewOf = state == ReviewItemState.PENDING ? null : this.decisionRepository
          .findFirstByItemTypeAndItemIdAndDecisionInOrderByDecidedAtDesc(ReviewItemType.DOMAIN, domain.getDomainId(),
              List.of(ReviewDecision.APPROVED, ReviewDecision.REJECTED))
          .map(decision -> decision.getReview().getRegistration())
          .filter(decidedFor -> !decidedFor.getRegistrationId().equals(entity.getRegistrationId()))
          .map(Registration::getEntityId)
          .orElse(null);
      items.add(new ReviewItemDto(ReviewItemType.DOMAIN, domain.getDomainId(), null, domain.getDomain(),
          "Domain claim", state, null, domain.getReviewedBy(), toOffsetDateTime(domain.getReviewedAt()),
          domain.getRejectionReason(),
          decidedInReviewOf, deferred.contains(key(ReviewItemType.DOMAIN, domain.getDomainId(), null)),
          withPreviews ? this.domainPreview(domain, entity) : null));
    }

    for (final Registration trustMark : this.registrationRepository
        .findByParentRegistration_RegistrationId(entity.getRegistrationId())) {
      final ReviewItemDto item = this.registrationItem(ReviewItemType.TRUST_MARK, trustMark, trustMark.getEntityId(),
          "Trust mark request", deferred, withPreviews);
      if (item != null) {
        items.add(item);
      }
    }

    final List<StepExecutionRecordDto> steps = Optional.ofNullable(entity.getStepResults()).orElse(List.of());
    for (int index = 0; index < steps.size(); index++) {
      final StepExecutionRecordDto step = steps.get(index);
      if (step.reviewedBy() != null) {
        items.add(new ReviewItemDto(ReviewItemType.ENTITY, entity.getRegistrationId(), index, step.stepName(),
            "Entity registration", ReviewItemState.APPROVED, null, step.reviewedBy(), step.reviewedAt(), null, null,
            false, withPreviews ? step.preview() : null));
      }
    }
    final ReviewItemDto current = this.registrationItem(ReviewItemType.ENTITY, entity, null, "Entity registration",
        deferred, withPreviews);
    if (current != null && current.state() != ReviewItemState.APPROVED) {
      items.add(current);
    }
    if (entity.getStatus() == RegistrationStatus.PENDING_APPROVAL && entity.getPendingStepIndex() != null) {
      items.addAll(this.upcomingSteps(entity));
    }
    return items;
  }

  /**
   * Builds the item for a registration paused for review or decided; {@code null} for one still running or failed.
   */
  private ReviewItemDto registrationItem(final ReviewItemType type, final Registration registration,
      final String title, final String subtitle, final Set<String> deferred, final boolean withPreviews) {
    final List<StepExecutionRecordDto> steps = Optional.ofNullable(registration.getStepResults()).orElse(List.of());
    final Integer index = registration.getPendingStepIndex();
    final StepExecutionRecordDto step = index != null && index >= 0 && index < steps.size() ? steps.get(index) : null;
    final String name = title != null ? title : step != null ? step.stepName() : "Registration";
    final StepPreview preview = withPreviews && step != null ? step.preview() : null;
    return switch (registration.getStatus()) {
      case PENDING_APPROVAL -> {
        final Optional<String> waitingFor = this.reviewOrder.waitingFor(registration);
        yield new ReviewItemDto(type, registration.getRegistrationId(), index, name, subtitle,
            waitingFor.isPresent() ? ReviewItemState.WAITING : ReviewItemState.PENDING, waitingFor.orElse(null),
            null, null, null, null, deferred.contains(key(type, registration.getRegistrationId(), index)), preview);
      }
      case APPROVED -> type == ReviewItemType.TRUST_MARK
          ? new ReviewItemDto(type, registration.getRegistrationId(), index, name, subtitle, ReviewItemState.APPROVED,
              null, registration.getReviewedBy(), toOffsetDateTime(registration.getReviewedAt()), null, null, false,
              preview)
          : null;
      case REJECTED -> new ReviewItemDto(type, registration.getRegistrationId(), index, name, subtitle,
          ReviewItemState.REJECTED, null, registration.getReviewedBy(), toOffsetDateTime(registration.getReviewedAt()),
          registration.getRejectionReason(), null, false, preview);
      case STARTED -> null;
    };
  }

  /**
   * Lists the steps after the paused one that will pause for review too. A step that does not apply to this entity
   * (such as the hosted step for an entity that serves its own configuration) is left out: each step is asked, as the
   * engine will ask it, with the context the registration saved when it paused.
   */
  private List<ReviewItemDto> upcomingSteps(final Registration entity) {
    final List<StepDefinition> steps = Mapper.toProcessFlow(entity.getFlowAssignment().getRegistrationFlow(),
        this.registrationStepRepository).getProcessFlow();
    final ProcessContext context = savedContext(entity);
    final List<ReviewItemDto> upcoming = new ArrayList<>();
    for (int index = entity.getPendingStepIndex() + 1; index < steps.size(); index++) {
      final StepDefinition step = steps.get(index);
      if (step.step() instanceof final ReviewableStep reviewable && reviewable.requiresManualReview(step.config())
          && applies(step, context)) {
        upcoming.add(new ReviewItemDto(ReviewItemType.ENTITY, entity.getRegistrationId(), index, step.name(),
            "Entity registration", ReviewItemState.UPCOMING, null, null, null, null, null, false, null));
      }
    }
    return upcoming;
  }

  /** Rebuilds what the engine's context holds for a paused registration, enough for steps to tell if they apply. */
  private static ProcessContext savedContext(final Registration entity) {
    final ProcessContext context = new ProcessContext();
    context.put(ContextKey.ENTITY_ID, entity.getEntityId());
    context.put(ContextKey.REGISTRATION_ID, entity.getRegistrationId());
    context.put(ContextKey.ORG, new OrganizationRecord(entity.getOrganization().getOrgNumber(),
        entity.getOrganization().getOrgName(), null, null));
    if (entity.getTrustmarksRequested() != null) {
      context.put(ContextKey.TRUSTMARKS_REQUESTED, new SerializableList<>(entity.getTrustmarksRequested()));
    }
    PersistedContext.restore(entity.getProcessContext(), context);
    if (context.get(ContextKey.REQUEST_METADATA).isEmpty()
        && entity.getRequestMetadata() != null && !entity.getRequestMetadata().isEmpty()) {
      context.put(ContextKey.REQUEST_METADATA, new JSONObject(entity.getRequestMetadata()));
    }
    return context;
  }

  /** Asks a step whether it applies; a step that cannot tell from the saved context is shown. */
  private static boolean applies(final StepDefinition step, final ProcessContext context) {
    try {
      return step.step().canApply(context, step.config());
    }
    catch (final RuntimeException e) {
      return true;
    }
  }

  private StepPreview domainPreview(final OrganizationDomain domain, final Registration entity) {
    final List<String> dependent = this.registrationRepository
        .findByOrganization_OrganizationIdAndStatusIn(domain.getOrganization().getOrganizationId(), OPEN_STATUSES)
        .stream()
        .filter(registration -> registration.getRegistrationType() == RegistrationType.SUBORDINATE)
        .map(Registration::getEntityId)
        .filter(entityId -> DomainMatcher.hostOf(entityId)
            .filter(host -> DomainMatcher.matches(host, domain.getDomain()))
            .isPresent())
        .toList();
    final Organization organization = domain.getOrganization();
    return StepPreview.builder()
        .add(PreviewSection.fields("Domain claim", List.of(
            new PreviewSection.Field("Domain", domain.getDomain()),
            new PreviewSection.Field("Organization",
                "%s (%s)".formatted(organizationName(organization), organization.getOrgNumber())),
            new PreviewSection.Field("Claimed", String.valueOf(domain.getCreatedDate())),
            new PreviewSection.Field("Status", domain.getStatus().name()))))
        .add(PreviewSection.checks("Checks", List.of(new PreviewSection.Check(PreviewSection.CheckStatus.OK,
            "The entity's host %s is under %s".formatted(DomainMatcher.hostOf(entity.getEntityId()).orElse("?"),
                domain.getDomain())))))
        .add(dependent.isEmpty() ? null
            : PreviewSection.list("Registrations waiting under this domain", dependent))
        .add(domain.getStatus() != DomainStatus.PENDING ? null : PreviewSection.text("If rejected",
            ("Registrations under %s that no other domain of the organization covers are rejected with reason "
                + "\"Not Accepted Domain\", and their reviews end.").formatted(domain.getDomain())))
        .build();
  }

  private Set<String> deferredIn(final EntityReview review) {
    final Map<String, ReviewDecision> latest = new HashMap<>();
    this.decisionRepository.findByReview_ReviewIdOrderByDecidedAtAsc(review.getReviewId())
        .forEach(decision -> latest.put(key(decision.getItemType(), decision.getItemId(), decision.getStepIndex()),
            decision.getDecision()));
    return latest.entrySet().stream()
        .filter(entry -> entry.getValue() == ReviewDecision.DEFERRED)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private int decidedIn(final EntityReview review) {
    return (int) this.decisionRepository.findByReview_ReviewIdOrderByDecidedAtAsc(review.getReviewId()).stream()
        .filter(decision -> decision.getDecision() != ReviewDecision.DEFERRED)
        .count();
  }

  private static String key(final ReviewItemType type, final UUID itemId, final Integer stepIndex) {
    return type + ":" + itemId + ":" + (type == ReviewItemType.DOMAIN ? "" : stepIndex);
  }

  private static EntityReviewState state(final EntityReview open, final List<ReviewItemDto> items) {
    if (open != null) {
      return EntityReviewState.IN_REVIEW;
    }
    return items.stream().anyMatch(item -> UNDECIDED.contains(item.state()))
        ? EntityReviewState.NOT_STARTED
        : EntityReviewState.COMPLETED;
  }

  private static int count(final List<ReviewItemDto> items, final ReviewItemType type) {
    return (int) items.stream().filter(item -> item.type() == type && UNDECIDED.contains(item.state())).count();
  }

  private static String organizationName(final Organization organization) {
    return organization.getLegalName() != null ? organization.getLegalName() : organization.getOrgName();
  }
}
