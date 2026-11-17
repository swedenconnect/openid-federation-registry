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

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import se.swedenconnect.oidf.registry.infrastructure.config.StateSyncProperties;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Compacts the change log at the configured interval: removes every change that a later change of the same key
 * supersedes, and the {@code delete} changes older than the retention. After compaction the log holds the latest
 * change of each live key, so reading from 0 gives every live record. A federation service that has not read up to
 * the removed {@code delete} changes reads all changes again from 0.
 * <p>
 * Every node of the registry compacts. This is safe without coordination and while writers append and readers page:
 * a change is only removed when a later change of the same key exists, which a reader that has not read the removed
 * change reads later, and the stored highest sequence number of the removed {@code delete} changes only goes up.
 * Compaction reads committed rows only, so it takes no gap locks that would hold up writers.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class StateChangePruner {

  private final StateChangeStore store;
  private final TransactionTemplate transaction;
  private final Duration retention;
  private final Duration interval;
  private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

  /**
   * Constructor.
   *
   * @param store the change log
   * @param transactionManager the transaction manager
   * @param properties the retention and the interval
   */
  public StateChangePruner(final StateChangeStore store, final PlatformTransactionManager transactionManager,
      final StateSyncProperties properties) {
    this.store = store;
    this.transaction = new TransactionTemplate(transactionManager);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    this.retention = properties.effectiveRetention();
    this.interval = properties.effectiveCompactionInterval();
  }

  /**
   * Starts compacting once the application is ready.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    this.scheduler.setThreadNamePrefix("state-change-pruner-");
    this.scheduler.initialize();
    this.scheduler.scheduleWithFixedDelay(this::compact, this.interval);
    log.info("Compacting changes of the federation service API every {}, delete changes are kept for {}",
        this.interval, this.retention);
  }

  /**
   * Stops compacting.
   */
  @PreDestroy
  public void stop() {
    this.scheduler.shutdown();
  }

  /**
   * Compacts the changes of every instance.
   */
  public void compact() {
    try {
      final List<UUID> instanceIds = this.transaction.execute(status -> this.store.instanceIds());
      for (final UUID instanceId : instanceIds) {
        final Integer superseded = this.transaction.execute(status -> this.store.removeSuperseded(instanceId));
        final Integer tombstones =
            this.transaction.execute(status -> this.store.removeTombstones(instanceId, this.retention));
        if ((superseded != null && superseded > 0) || (tombstones != null && tombstones > 0)) {
          log.info("Compacted changes of the federation service API for instance {}: {} superseded, {} delete "
              + "changes removed", instanceId, superseded, tombstones);
        }
      }
    }
    catch (final RuntimeException e) {
      log.error("Failed to compact changes of the federation service API", e);
    }
  }
}
