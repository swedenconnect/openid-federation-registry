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
package se.swedenconnect.oidf.registry.federationservice.state;

import com.nimbusds.jose.util.JSONObjectUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Maps a change of the registry, as told by its audit event, to the keys of the records that may have changed. A key
 * that has not changed costs a federation service one read, while a missing key leaves its record stale, so the
 * mapping includes a key whenever it is unsure. The database is read as it is after the change, and the data of the
 * event gives what was there before it.
 * <p>
 * Besides the record of the changed row itself, a record depends on:
 * <ul>
 *   <li>an entity record: the name of the organization (federation entities), the modules of the entity, the
 *   subordinates in the organization whose entity identifier is the entity's, whose Trust Anchors give the authority
 *   hints, and the trust mark subject entries naming the entity, which give the trust mark sources of a hosted
 *   entity;</li>
 *   <li>a Trust Anchor: the entity identifier of its entity, and the active trust mark issuers of the organization and
 *   their trust mark types;</li>
 *   <li>a subordinate: the metadata policy and the active flag of its Trust Anchor, the hosted entity with its entity
 *   identifier in any organization or instance, and whether a federation entity with its entity identifier in any
 *   organization or instance has a Trust Anchor or Intermediate;</li>
 *   <li>a trust mark issuer: the entity identifier of its entity, its trust marks and their subjects;</li>
 *   <li>a resolver: the entity identifier of its entity.</li>
 * </ul>
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class StateDependencies {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * The changes that a set of events gives, per instance. A key that is both updated and deleted is updated, since a
   * key that gives no record is read as removed.
   */
  public static final class Changes {

    private final Map<UUID, Map<String, String>> byInstance = new TreeMap<>();

    /**
     * Adds an updated key.
     *
     * @param instanceId the instance
     * @param key the key
     */
    public void upsert(final UUID instanceId, final String key) {
      this.byInstance.computeIfAbsent(instanceId, id -> new TreeMap<>()).put(key, StateKey.UPSERT);
    }

    /**
     * Adds a deleted key, unless it is updated.
     *
     * @param instanceId the instance
     * @param key the key
     */
    public void delete(final UUID instanceId, final String key) {
      this.byInstance.computeIfAbsent(instanceId, id -> new TreeMap<>()).putIfAbsent(key, StateKey.DELETE);
    }

    /**
     * The changes, per instance in the order of the instance IDs.
     *
     * @return instance to key to operation
     */
    public Map<UUID, Map<String, String>> byInstance() {
      return this.byInstance;
    }

    /**
     * Whether there are no changes.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
      return this.byInstance.values().stream().allMatch(Map::isEmpty);
    }
  }

  /**
   * Whether an event may change any record.
   *
   * @param event the event
   * @return true if the event is mapped to keys
   */
  public boolean affectsRecords(final FederationAuditEvent event) {
    return switch (event.getEvent()) {
      case FEDERATION_ENTITY_CREATED, FEDERATION_ENTITY_UPDATED, FEDERATION_ENTITY_DELETED,
           HOSTED_ENTITY_CREATED, HOSTED_ENTITY_UPDATED, HOSTED_ENTITY_DELETED,
           SUBORDINATE_CREATED, SUBORDINATE_UPDATED, SUBORDINATE_DELETED,
           SUBORDINATE_ENTITY_CREATED, SUBORDINATE_ENTITY_UPDATED, SUBORDINATE_ENTITY_DELETED,
           TRUST_ANCHOR_CREATED, TRUST_ANCHOR_UPDATED, TRUST_ANCHOR_DELETED,
           INTERMEDIATE_CREATED, INTERMEDIATE_UPDATED, INTERMEDIATE_DELETED,
           RESOLVER_CREATED, RESOLVER_UPDATED, RESOLVER_DELETED,
           TRUSTMARK_ISSUER_CREATED, TRUSTMARK_ISSUER_UPDATED, TRUSTMARK_ISSUER_DELETED,
           TRUSTMARK_CREATED, TRUSTMARK_UPDATED, TRUSTMARK_DELETED,
           TRUSTMARK_SUBJECT_CREATED, TRUSTMARK_SUBJECT_UPDATED, TRUSTMARK_SUBJECT_DELETED,
           ORGANIZATION_UPDATED -> event.getInstanceId() != null && event.getExtId() != null;
      default -> false;
    };
  }

  /**
   * Adds the keys that an event may have changed. The persistence context must be flushed first.
   *
   * @param event the event
   * @param changes the changes to add to
   */
  public void map(final FederationAuditEvent event, final Changes changes) {
    if (!this.affectsRecords(event)) {
      return;
    }
    final UUID instanceId = UUID.fromString(event.getInstanceId());
    final UUID organizationId = Optional.ofNullable(event.getOrganizationId()).map(UUID::fromString).orElse(null);
    final UUID id = UUID.fromString(event.getExtId());
    final Map<String, Object> oldData = parse(event.getOldData());
    final Map<String, Object> newData = parse(event.getNewData());
    switch (event.getEvent()) {
    case FEDERATION_ENTITY_CREATED, FEDERATION_ENTITY_UPDATED ->
        this.federationEntity(instanceId, organizationId, id, oldData, newData, false, changes);
    case FEDERATION_ENTITY_DELETED ->
        this.federationEntity(instanceId, organizationId, id, oldData, newData, true, changes);
    case HOSTED_ENTITY_CREATED, HOSTED_ENTITY_UPDATED ->
        this.hostedEntity(instanceId, id, oldData, newData, false, changes);
    case HOSTED_ENTITY_DELETED -> this.hostedEntity(instanceId, id, oldData, newData, true, changes);
    case SUBORDINATE_CREATED, SUBORDINATE_UPDATED, SUBORDINATE_ENTITY_CREATED, SUBORDINATE_ENTITY_UPDATED ->
        this.subordinate(instanceId, organizationId, id, oldData, newData, false, changes);
    case SUBORDINATE_DELETED, SUBORDINATE_ENTITY_DELETED ->
        this.subordinate(instanceId, organizationId, id, oldData, newData, true, changes);
    case TRUST_ANCHOR_CREATED, TRUST_ANCHOR_UPDATED, INTERMEDIATE_CREATED, INTERMEDIATE_UPDATED ->
        this.trustAnchor(instanceId, id, oldData, newData, false, changes);
    case TRUST_ANCHOR_DELETED, INTERMEDIATE_DELETED ->
        this.trustAnchor(instanceId, id, oldData, newData, true, changes);
    case RESOLVER_CREATED, RESOLVER_UPDATED, RESOLVER_DELETED -> {
      this.upsertOrDelete(instanceId, StateKey.resolver(id), event.getEvent().name().endsWith("_DELETED"), changes);
      uuids(oldData, newData, "entityId").forEach(entityId -> changes.upsert(instanceId, StateKey.entity(entityId)));
    }
    case TRUSTMARK_ISSUER_CREATED, TRUSTMARK_ISSUER_UPDATED, TRUSTMARK_ISSUER_DELETED -> {
      this.upsertOrDelete(instanceId, StateKey.trustMarkIssuer(id), event.getEvent().name().endsWith("_DELETED"),
          changes);
      uuids(oldData, newData, "entityId").forEach(entityId -> changes.upsert(instanceId, StateKey.entity(entityId)));
      this.trustAnchorsOf(organizationId).forEach(taId -> changes.upsert(instanceId, StateKey.trustAnchor(taId)));
    }
    case TRUSTMARK_CREATED, TRUSTMARK_UPDATED, TRUSTMARK_DELETED -> {
      final Set<UUID> issuers = new LinkedHashSet<>(uuids(oldData, newData, "trustmarkissuerId"));
      this.trustMarkIssuerOfTrustMark(id).ifPresent(issuers::add);
      issuers.forEach(issuerId -> changes.upsert(instanceId, StateKey.trustMarkIssuer(issuerId)));
      this.trustAnchorsOf(organizationId).forEach(taId -> changes.upsert(instanceId, StateKey.trustAnchor(taId)));
    }
    case TRUSTMARK_SUBJECT_CREATED, TRUSTMARK_SUBJECT_UPDATED, TRUSTMARK_SUBJECT_DELETED ->
        this.trustMarkSubject(instanceId, organizationId, oldData, newData, changes);
    case ORGANIZATION_UPDATED -> this.federationEntitiesOf(organizationId)
        .forEach(entityId -> changes.upsert(instanceId, StateKey.entity(entityId)));
    default -> {
    }
    }
  }

  private void federationEntity(final UUID instanceId, final UUID organizationId, final UUID entityId,
      final Map<String, Object> oldData, final Map<String, Object> newData, final boolean deleted,
      final Changes changes) {
    this.upsertOrDelete(instanceId, StateKey.entity(entityId), deleted, changes);
    this.uuidList("SELECT m.taImId FROM TrustAnchorIntermediateModule m WHERE m.entity.entityId = :id", entityId)
        .forEach(moduleId -> this.trustAnchorAndSubordinates(instanceId, moduleId, changes));
    this.uuidList("SELECT t.trustmarkIssuerId FROM TrustMarkIssuer t WHERE t.entity.entityId = :id", entityId)
        .forEach(moduleId -> changes.upsert(instanceId, StateKey.trustMarkIssuer(moduleId)));
    this.uuidList("SELECT r.resolverId FROM Resolver r WHERE r.entity.entityId = :id", entityId)
        .forEach(moduleId -> changes.upsert(instanceId, StateKey.resolver(moduleId)));
    this.trustAnchorsOf(organizationId).forEach(taId -> changes.upsert(instanceId, StateKey.trustAnchor(taId)));
    this.subordinatesNamed(strings(oldData, newData, "entityIdentifier"), false, changes);
  }

  private void hostedEntity(final UUID instanceId, final UUID entityId, final Map<String, Object> oldData,
      final Map<String, Object> newData, final boolean deleted, final Changes changes) {
    this.upsertOrDelete(instanceId, StateKey.entity(entityId), deleted, changes);
    this.subordinatesNamed(strings(oldData, newData, "entityIdentifier"), true, changes);
  }

  private void subordinate(final UUID instanceId, final UUID organizationId, final UUID subordinateId,
      final Map<String, Object> oldData, final Map<String, Object> newData, final boolean deleted,
      final Changes changes) {
    final Optional<UUID> current = this.trustAnchorOfSubordinate(subordinateId, deleted);
    final Set<UUID> trustAnchors = new LinkedHashSet<>(uuids(oldData, newData, "taImId"));
    current.ifPresent(trustAnchors::add);
    trustAnchors.forEach(taId -> this.upsertOrDelete(instanceId, StateKey.subordinate(taId, subordinateId),
        current.map(taId::equals).map(same -> !same).orElse(true), changes));
    final List<String> issuers = strings(oldData, newData, "entityIdentifier");
    if (organizationId != null && !issuers.isEmpty()) {
      this.entityManager.createQuery("SELECT e.entityId FROM FederationEntity e "
              + "WHERE e.organization.organizationId = :organizationId AND e.issuer IN :issuers", UUID.class)
          .setParameter("organizationId", organizationId)
          .setParameter("issuers", issuers)
          .getResultList()
          .forEach(entityId -> changes.upsert(instanceId, StateKey.entity(entityId)));
    }
  }

  private void trustAnchor(final UUID instanceId, final UUID moduleId, final Map<String, Object> oldData,
      final Map<String, Object> newData, final boolean deleted, final Changes changes) {
    if (deleted) {
      changes.delete(instanceId, StateKey.trustAnchor(moduleId));
      Stream.of(oldData, newData)
          .filter(Objects::nonNull)
          .map(data -> data.get("subordinates"))
          .filter(List.class::isInstance)
          .flatMap(list -> ((List<?>) list).stream())
          .filter(Map.class::isInstance)
          .map(subordinate -> ((Map<?, ?>) subordinate).get("subordinateId"))
          .map(StateDependencies::toUuid)
          .flatMap(Optional::stream)
          .forEach(subordinateId -> changes.delete(instanceId, StateKey.subordinate(moduleId, subordinateId)));
    }
    else {
      this.trustAnchorAndSubordinates(instanceId, moduleId, changes);
    }
    final List<UUID> entityIds = uuids(oldData, newData, "entityId");
    entityIds.forEach(entityId -> changes.upsert(instanceId, StateKey.entity(entityId)));
    if (!entityIds.isEmpty()) {
      this.subordinatesNamed(this.entityManager.createQuery(
              "SELECT e.issuer FROM FederationEntity e WHERE e.entityId IN :ids", String.class)
          .setParameter("ids", entityIds)
          .getResultList(), false, changes);
    }
  }

  private void trustMarkSubject(final UUID instanceId, final UUID organizationId, final Map<String, Object> oldData,
      final Map<String, Object> newData, final Changes changes) {
    // The entity record of the subject carries the trust mark as a trust mark source, so it changes with the entry.
    this.hostedEntitiesNamed(strings(oldData, newData, "subject"), changes);
    boolean found = false;
    for (final UUID trustMarkId : uuids(oldData, newData, "trustmarkId")) {
      final List<Object[]> rows = this.entityManager.createQuery(
              "SELECT t.trustmarkIssuer.trustmarkIssuerId, t.trustmarkIssuer.entity.organization.organizationId "
                  + "FROM TrustMark t WHERE t.trustmarkId = :id", Object[].class)
          .setParameter("id", trustMarkId)
          .getResultList();
      for (final Object[] row : rows) {
        found = true;
        changes.upsert(instanceId, StateKey.trustMarkIssuer((UUID) row[0]));
        this.trustAnchorsOf((UUID) row[1]).forEach(taId -> changes.upsert(instanceId, StateKey.trustAnchor(taId)));
      }
    }
    if (!found) {
      this.uuidList("SELECT t.trustmarkIssuerId FROM TrustMarkIssuer t "
              + "WHERE t.entity.organization.organizationId = :id", organizationId)
          .forEach(issuerId -> changes.upsert(instanceId, StateKey.trustMarkIssuer(issuerId)));
      this.trustAnchorsOf(organizationId).forEach(taId -> changes.upsert(instanceId, StateKey.trustAnchor(taId)));
    }
  }

  private void trustAnchorAndSubordinates(final UUID instanceId, final UUID moduleId, final Changes changes) {
    changes.upsert(instanceId, StateKey.trustAnchor(moduleId));
    this.uuidList("SELECT s.subordinateId FROM Subordinate s WHERE s.taIm.taImId = :id", moduleId)
        .forEach(subordinateId -> changes.upsert(instanceId, StateKey.subordinate(moduleId, subordinateId)));
    this.uuidList("SELECT e.entityId FROM FederationEntity e, Subordinate s WHERE s.taIm.taImId = :id "
            + "AND e.organization = s.taIm.organization AND e.issuer = s.entityidentifier", moduleId)
        .forEach(entityId -> changes.upsert(instanceId, StateKey.entity(entityId)));
  }

  /** Subordinates with one of the entity identifiers, in any organization and instance. */
  private void hostedEntitiesNamed(final Collection<String> entityIdentifiers, final Changes changes) {
    if (entityIdentifiers.isEmpty()) {
      return;
    }
    this.entityManager.createQuery("SELECT e.entityId, e.organization.instance.instanceId FROM FederationEntity e "
            + "WHERE e.entityType = :entityType AND e.issuer IN :issuers", Object[].class)
        .setParameter("entityType", EntityType.HOSTED_ENTITY)
        .setParameter("issuers", entityIdentifiers)
        .getResultList()
        .forEach(row -> changes.upsert((UUID) row[1], StateKey.entity((UUID) row[0])));
  }

  private void subordinatesNamed(final Collection<String> entityIdentifiers, final boolean automaticOnly,
      final Changes changes) {
    if (entityIdentifiers.isEmpty()) {
      return;
    }
    String query = "SELECT s.taIm.taImId, s.subordinateId, "
        + "s.taIm.entity.organization.instance.instanceId FROM Subordinate s "
        + "WHERE s.entityidentifier IN :issuers";
    if (automaticOnly) {
      query += " AND s.ecLocationAutomatic = true";
    }
    this.entityManager.createQuery(query, Object[].class)
        .setParameter("issuers", entityIdentifiers)
        .getResultList()
        .forEach(row -> changes.upsert((UUID) row[2], StateKey.subordinate((UUID) row[0], (UUID) row[1])));
  }

  private List<UUID> trustAnchorsOf(final UUID organizationId) {
    return this.uuidList("SELECT m.taImId FROM TrustAnchorIntermediateModule m "
        + "WHERE m.organization.organizationId = :id OR m.entity.organization.organizationId = :id", organizationId);
  }

  private List<UUID> federationEntitiesOf(final UUID organizationId) {
    if (organizationId == null) {
      return List.of();
    }
    return this.entityManager.createQuery("SELECT e.entityId FROM FederationEntity e "
            + "WHERE e.organization.organizationId = :id AND e.entityType = :type", UUID.class)
        .setParameter("id", organizationId)
        .setParameter("type", EntityType.FEDERATION_ENTITY)
        .getResultList();
  }

  private Optional<UUID> trustAnchorOfSubordinate(final UUID subordinateId, final boolean deleted) {
    if (deleted) {
      return Optional.empty();
    }
    return this.uuidList("SELECT s.taIm.taImId FROM Subordinate s WHERE s.subordinateId = :id", subordinateId)
        .stream().findFirst();
  }

  private Optional<UUID> trustMarkIssuerOfTrustMark(final UUID trustMarkId) {
    return this.uuidList("SELECT t.trustmarkIssuer.trustmarkIssuerId FROM TrustMark t WHERE t.trustmarkId = :id",
        trustMarkId).stream().findFirst();
  }

  private List<UUID> uuidList(final String jpql, final UUID id) {
    if (id == null) {
      return List.of();
    }
    return this.entityManager.createQuery(jpql, UUID.class).setParameter("id", id).getResultList();
  }

  private void upsertOrDelete(final UUID instanceId, final String key, final boolean deleted, final Changes changes) {
    if (deleted) {
      changes.delete(instanceId, key);
    }
    else {
      changes.upsert(instanceId, key);
    }
  }

  private static Map<String, Object> parse(final String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return JSONObjectUtils.parse(json);
    }
    catch (final ParseException e) {
      log.warn("The data of an audit event is not a JSON object, it is not used to find changed records: {}",
          e.getMessage());
      return null;
    }
  }

  private static List<String> strings(final Map<String, Object> oldData, final Map<String, Object> newData,
      final String field) {
    return Stream.of(oldData, newData)
        .filter(Objects::nonNull)
        .map(data -> data.get(field))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .distinct()
        .toList();
  }

  private static List<UUID> uuids(final Map<String, Object> oldData, final Map<String, Object> newData,
      final String field) {
    return strings(oldData, newData, field).stream()
        .map(StateDependencies::toUuid)
        .flatMap(Optional::stream)
        .distinct()
        .toList();
  }

  private static Optional<UUID> toUuid(final Object value) {
    if (!(value instanceof final String text)) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(text));
    }
    catch (final IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
