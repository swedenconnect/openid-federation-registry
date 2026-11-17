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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.swedenconnect.oidf.registry.federationservice.service.OidfApiService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Marks every record as changed at the first startup, and when the stored version of how the records are built differs
 * from {@link #RECORD_VERSION}.
 * <p>
 * Raise {@link #RECORD_VERSION} when a release changes how the records are built, so that the federation services read
 * every record again after the upgrade.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class RecordConfigCheck {

  /** The version of how the records are built. */
  static final int RECORD_VERSION = 1;

  private final StateChangeStore store;
  private final OidfApiService oidfApiService;
  private final TransactionTemplate transaction;

  /**
   * Constructor.
   *
   * @param store the change log
   * @param oidfApiService builds the records
   * @param transactionManager the transaction manager
   */
  public RecordConfigCheck(final StateChangeStore store, final OidfApiService oidfApiService,
      final PlatformTransactionManager transactionManager) {
    this.store = store;
    this.oidfApiService = oidfApiService;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  /**
   * Compares {@link #RECORD_VERSION} with the stored version, once the instances exist. The version row is locked, so
   * that nodes that start at once mark the records only once.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void check() {
    this.transaction.executeWithoutResult(status -> {
      final Optional<Integer> stored = this.store.lockRecordVersion();
      if (stored.filter(version -> version == RECORD_VERSION).isPresent()) {
        return;
      }
      log.info("The record version of the federation service API changed from {} to {}, every record is marked "
          + "as changed", stored.orElse(null), RECORD_VERSION);
      for (final UUID instanceId : this.store.instanceIds()) {
        final Map<String, String> changes = new LinkedHashMap<>();
        this.oidfApiService.stateKeys(instanceId).forEach(key -> changes.put(key, StateKey.UPSERT));
        if (!changes.isEmpty() && this.store.lockInstance(instanceId)) {
          this.store.append(instanceId, changes);
        }
      }
      this.store.storeRecordVersion(RECORD_VERSION);
    });
  }
}
