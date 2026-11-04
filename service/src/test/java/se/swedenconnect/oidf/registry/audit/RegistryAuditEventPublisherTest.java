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

package se.swedenconnect.oidf.registry.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventPublisher;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link RegistryAuditEventPublisher} publishes an audit record only for a change that was committed.
 *
 * @author Per Fredrik Plars
 */
class RegistryAuditEventPublisherTest {

  private final List<Object> events = new ArrayList<>();
  private String auditor = "admin";
  private final RegistryAuditService auditService = new RegistryAuditEventPublisher(this.events::add,
      () -> Optional.of(this.auditor), new JsonMapper());

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("Outside a transaction the audit record is published at once")
  void publishedAtOnceOutsideTransaction() {
    this.auditService.resolveJwks(URI.create("https://digg.se/papartner"));

    assertThat(this.auditEvents()).singleElement()
        .satisfies(event -> assertThat(event.getAuditEvent().getPrincipal()).isEqualTo("admin"));
    assertThat(this.events).hasAtLeastOneElementOfType(FederationAuditEvent.class);
  }

  @Test
  @DisplayName("Inside a transaction the audit record is published after commit, with the principal of the request")
  void publishedAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();
    this.auditService.resolveJwks(URI.create("https://digg.se/papartner"));

    assertThat(this.auditEvents()).isEmpty();
    assertThat(this.events).hasAtLeastOneElementOfType(FederationAuditEvent.class);

    this.auditor = "someone-else";
    TransactionSynchronizationUtils.invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());

    assertThat(this.auditEvents()).singleElement()
        .satisfies(event -> assertThat(event.getAuditEvent().getPrincipal()).isEqualTo("admin"));
  }

  @Test
  @DisplayName("A rolled back transaction leaves no audit record")
  void notPublishedOnRollback() {
    TransactionSynchronizationManager.initSynchronization();
    this.auditService.resolveJwks(URI.create("https://digg.se/papartner"));

    TransactionSynchronizationUtils.invokeAfterCompletion(TransactionSynchronizationManager.getSynchronizations(),
        TransactionSynchronization.STATUS_ROLLED_BACK);

    assertThat(this.auditEvents()).isEmpty();
  }

  private List<AuditApplicationEvent> auditEvents() {
    return this.events.stream()
        .filter(AuditApplicationEvent.class::isInstance)
        .map(AuditApplicationEvent.class::cast)
        .toList();
  }
}
