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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes the change log of the records that the federation services read. Every method runs in the
 * transaction of its caller.
 * <p>
 * The sequence numbers of the changes of an instance are taken with the instance row locked, so they follow the commit
 * order of the instance: a change that is committed later has a higher number. Compaction removes every change that a
 * later change of the same key supersedes, so the log holds the latest change of each key. It also removes the
 * {@code delete} changes older than the retention and stores the highest sequence number removed. A reader that has
 * not read up to this number has to read all changes again from 0.
 *
 * @author Felix Hellman
 */
@Component
public class StateChangeStore {

  private static final int INSERT_CHUNK = 200;

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * One change of a record.
   *
   * @param key the key of the record
   * @param op {@link StateKey#UPSERT} or {@link StateKey#DELETE}
   * @param seq the sequence number
   */
  public record Change(String key, String op, long seq) {
  }

  /**
   * The IDs of all instances.
   *
   * @return the instance IDs
   */
  public List<UUID> instanceIds() {
    return this.entityManager.createQuery("SELECT i.instanceId FROM Instance i ORDER BY i.instanceId", UUID.class)
        .getResultList();
  }

  /**
   * Locks the row of an instance until the end of the transaction. Writers of the same instance wait for each other,
   * so that their sequence numbers follow commit order. Lock several instances in the order of their IDs.
   *
   * @param instanceId the instance ID
   * @return false if there is no such instance
   */
  public boolean lockInstance(final UUID instanceId) {
    return !this.entityManager
        .createNativeQuery("SELECT instance_id FROM instance WHERE instance_id = :instanceId FOR UPDATE")
        .setParameter("instanceId", instanceId.toString())
        .getResultList()
        .isEmpty();
  }

  /**
   * Appends changes of an instance, whose row the caller has locked with {@link #lockInstance(UUID)}.
   *
   * @param instanceId the instance ID
   * @param changes key to operation
   */
  public void append(final UUID instanceId, final Map<String, String> changes) {
    final List<Map.Entry<String, String>> entries = new ArrayList<>(changes.entrySet());
    for (int start = 0; start < entries.size(); start += INSERT_CHUNK) {
      final List<Map.Entry<String, String>> chunk = entries.subList(start, Math.min(start + INSERT_CHUNK,
          entries.size()));
      final StringBuilder sql = new StringBuilder(
          "INSERT INTO state_change (seq, instance_id, change_key, op, created_at) VALUES ");
      for (int i = 0; i < chunk.size(); i++) {
        if (i > 0) {
          sql.append(", ");
        }
        sql.append("(NEXT VALUE FOR state_change_seq, :instanceId, :key").append(i)
            .append(", :op").append(i).append(", CURRENT_TIMESTAMP(6))");
      }
      final Query insert = this.entityManager.createNativeQuery(sql.toString())
          .setParameter("instanceId", instanceId.toString());
      for (int i = 0; i < chunk.size(); i++) {
        insert.setParameter("key" + i, chunk.get(i).getKey());
        insert.setParameter("op" + i, chunk.get(i).getValue());
      }
      insert.executeUpdate();
    }
  }

  /**
   * The highest sequence number of the removed {@code delete} changes of an instance.
   *
   * @param instanceId the instance ID
   * @return the sequence number, 0 if no {@code delete} change is removed
   */
  public long prunedSeq(final UUID instanceId) {
    final List<?> result = this.entityManager
        .createNativeQuery("SELECT pruned_seq FROM state_pruned WHERE instance_id = :instanceId")
        .setParameter("instanceId", instanceId.toString())
        .getResultList();
    return firstLong(result);
  }

  private static long firstLong(final List<?> result) {
    if (result.isEmpty() || result.getFirst() == null) {
      return 0;
    }
    return ((Number) result.getFirst()).longValue();
  }

  /**
   * The head of an instance: the highest sequence number it has reached. Compaction keeps the latest change of each
   * live key, and the highest sequence number of the removed {@code delete} changes is stored, so the head never goes
   * down.
   *
   * @param instanceId the instance ID
   * @return the head, 0 if the instance has no changes
   */
  public long head(final UUID instanceId) {
    final List<?> result = this.entityManager
        .createNativeQuery("SELECT MAX(seq) FROM state_change WHERE instance_id = :instanceId")
        .setParameter("instanceId", instanceId.toString())
        .getResultList();
    return Math.max(firstLong(result), this.prunedSeq(instanceId));
  }

  /**
   * The changes of an instance with {@code seq > since}, ordered by sequence number.
   *
   * @param instanceId the instance ID
   * @param since the sequence number the reader has read up to
   * @param limit the maximum number of changes
   * @return the changes
   */
  public List<Change> changes(final UUID instanceId, final long since, final int limit) {
    final List<?> rows = this.entityManager.createNativeQuery(
            "SELECT change_key, op, seq FROM state_change WHERE instance_id = :instanceId AND seq > :since "
                + "ORDER BY seq LIMIT :limit")
        .setParameter("instanceId", instanceId.toString())
        .setParameter("since", since)
        .setParameter("limit", limit)
        .getResultList();
    return rows.stream()
        .map(Object[].class::cast)
        .map(row -> new Change((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
        .toList();
  }

  /**
   * Removes the changes of an instance that a later change of the same key supersedes. A change is removed only when
   * the later change exists, so a reader that has not read the removed change reads the later one.
   *
   * @param instanceId the instance ID
   * @return the number of changes removed
   */
  public int removeSuperseded(final UUID instanceId) {
    return this.entityManager.createNativeQuery(
            "DELETE c FROM state_change c JOIN state_change n ON n.instance_id = c.instance_id "
                + "AND n.change_key = c.change_key AND n.seq > c.seq WHERE c.instance_id = :instanceId")
        .setParameter("instanceId", instanceId.toString())
        .executeUpdate();
  }

  /**
   * Removes the {@code delete} changes of an instance that are older than the retention, and stores the highest
   * sequence number removed.
   *
   * @param instanceId the instance ID
   * @param retention how long a {@code delete} change is kept
   * @return the number of changes removed
   */
  public int removeTombstones(final UUID instanceId, final Duration retention) {
    final Object max = this.entityManager.createNativeQuery(
            "SELECT MAX(seq) FROM state_change WHERE instance_id = :instanceId AND op = :op "
                + "AND created_at < TIMESTAMPADD(SECOND, :seconds, CURRENT_TIMESTAMP(6))")
        .setParameter("instanceId", instanceId.toString())
        .setParameter("op", StateKey.DELETE)
        .setParameter("seconds", -retention.toSeconds())
        .getSingleResult();
    if (max == null) {
      return 0;
    }
    final long prunedSeq = ((Number) max).longValue();
    this.entityManager.createNativeQuery(
            "INSERT INTO state_pruned (instance_id, pruned_seq) VALUES (:instanceId, :seq) "
                + "ON DUPLICATE KEY UPDATE pruned_seq = GREATEST(pruned_seq, VALUES(pruned_seq))")
        .setParameter("seq", prunedSeq)
        .setParameter("instanceId", instanceId.toString())
        .executeUpdate();
    return this.entityManager.createNativeQuery(
            "DELETE FROM state_change WHERE instance_id = :instanceId AND op = :op AND seq <= :seq")
        .setParameter("instanceId", instanceId.toString())
        .setParameter("op", StateKey.DELETE)
        .setParameter("seq", prunedSeq)
        .executeUpdate();
  }

  /**
   * Locks and reads the stored version of how the records are built.
   *
   * @return the version, empty if none is stored
   */
  public Optional<Integer> lockRecordVersion() {
    final List<?> result = this.entityManager
        .createNativeQuery("SELECT record_version FROM state_record_config WHERE id = 1 FOR UPDATE")
        .getResultList();
    if (result.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable((Number) result.getFirst()).map(Number::intValue);
  }

  /**
   * Stores the version of how the records are built.
   *
   * @param recordVersion the version
   */
  public void storeRecordVersion(final int recordVersion) {
    this.entityManager.createNativeQuery("UPDATE state_record_config SET record_version = :version WHERE id = 1")
        .setParameter("version", recordVersion)
        .executeUpdate();
  }
}
