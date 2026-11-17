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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records the changes of the records that the federation services read, in the transaction that makes them.
 * <p>
 * Each audit event of the transaction is collected as it is published. Just before the transaction commits, the
 * persistence context is flushed, the events are mapped to keys by {@link StateDependencies}, the keys are deduplicated
 * and written once: the rows of the affected instances are locked in the order of their IDs and one change per key is
 * appended. A failure rolls back the transaction, so a committed change always has its keys recorded.
 * <p>
 * A {@code @TransactionalEventListener} is not used, since it is called once per event, and the keys of all events of
 * a transaction are to be written once.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class StateChangeRecorder {

  private final StateDependencies dependencies;
  private final StateChangeStore store;
  private final TransactionTemplate newTransaction;

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Constructor.
   *
   * @param dependencies maps events to keys
   * @param store the change log
   * @param transactionManager the transaction manager, for an event published outside a transaction
   */
  public StateChangeRecorder(final StateDependencies dependencies, final StateChangeStore store,
      final PlatformTransactionManager transactionManager) {
    this.dependencies = dependencies;
    this.store = store;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Collects an audit event of the current transaction. An event published outside a transaction is recorded at
   * once, in a transaction of its own.
   *
   * @param event the audit event
   */
  @EventListener
  public void onAuditEvent(final FederationAuditEvent event) {
    if (!this.dependencies.affectsRecords(event)) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      log.warn("Audit event {} for {} was published outside a transaction, its changes are recorded on their own",
          event.getEvent(), event.getExtId());
      this.newTransaction.executeWithoutResult(status -> this.record(List.of(event)));
      return;
    }
    final PendingEvents pending = TransactionSynchronizationManager.getSynchronizations().stream()
        .filter(PendingEvents.class::isInstance)
        .map(PendingEvents.class::cast)
        .filter(candidate -> candidate.owner() == this)
        .findFirst()
        .orElseGet(() -> {
          final PendingEvents created = new PendingEvents();
          TransactionSynchronizationManager.registerSynchronization(created);
          return created;
        });
    pending.events.add(event);
  }

  private void record(final List<FederationAuditEvent> events) {
    this.entityManager.flush();
    final StateDependencies.Changes changes = new StateDependencies.Changes();
    events.forEach(event -> this.dependencies.map(event, changes));
    if (changes.isEmpty()) {
      return;
    }
    changes.byInstance().entrySet().stream()
        .filter(instance -> !instance.getValue().isEmpty())
        .forEachOrdered(instance -> this.append(instance.getKey(), instance.getValue()));
    log.debug("Recorded changed records {}", changes.byInstance());
  }

  private void append(final UUID instance, final Map<String, String> records) {
    if (!this.store.lockInstance(instance)) {
      log.warn("No instance {} found, its changed records {} are not recorded", instance, records.keySet());
      return;
    }
    this.store.append(instance, records);
  }

  /** The events of one transaction, written just before it commits. */
  private final class PendingEvents implements TransactionSynchronization {

    private final List<FederationAuditEvent> events = new ArrayList<>();

    private StateChangeRecorder owner() {
      return StateChangeRecorder.this;
    }

    @Override
    public void beforeCommit(final boolean readOnly) {
      StateChangeRecorder.this.record(List.copyOf(this.events));
    }
  }
}
