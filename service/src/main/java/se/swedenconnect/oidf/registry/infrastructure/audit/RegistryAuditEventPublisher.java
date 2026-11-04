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
package se.swedenconnect.oidf.registry.infrastructure.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

/**
 * The RegistryAuditServiceAuditEvent class implements the RegistryAuditService interface to provide audit event logging
 * functionality for the Federation API. This service utilizes an ApplicationEventPublisher to publish audit events and
 * an AuditorAware to determine the current user performing the actions. It logs the following types of actions as audit
 * events: - Read operations for federation entities - Read operations for trust marks subject to an entity - Read
 * operations for federation policies Audit events are constructed using FederationAuditEvent builder and are published
 * using the configured ApplicationEventPublisher.
 * <p>
 * An audit record describes a change that was made, so inside a transaction it is published only once the transaction
 * has committed. A change that is rolled back, because a later step fails or the commit itself fails, leaves no audit
 * record. Outside a transaction the record is published at once.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class RegistryAuditEventPublisher extends RegistryAuditServiceAdapter {

  private final ApplicationEventPublisher publisher;
  private final AuditorAware<String> currentUser;

  /**
   * Constructs a new instance of RegistryAuditServiceEvent, which extends the functionality of
   * RegistryAuditServiceAdapter to provide custom event publishing and auditing capabilities for the Federation API.
   *
   * @param publisher the ApplicationEventPublisher used to publish audit events to the application's event system.
   * @param currentUser the AuditorAware responsible for determining the current user performing the actions.
   * @param mapper the ObjectMapper responsible for handling JSON conversion or serialization processes.
   */
  public RegistryAuditEventPublisher(
      final ApplicationEventPublisher publisher,
      final AuditorAware<String> currentUser,
      final JsonMapper mapper) {
    super(mapper);
    this.publisher = publisher;
    this.currentUser = currentUser;

  }

  @Override
  protected void emitEvent(final FederationAuditEvent event) {
    // Its listener, the notification of the federation services, waits for the commit itself.
    this.publisher.publishEvent(event);
    // The principal is resolved now, while the security context of the request is at hand.
    final AuditApplicationEvent auditEvent = new AuditApplicationEvent(
        event.toAuditEvent(Optional.ofNullable(event.getPrincipal())
            .or(this.currentUser::getCurrentAuditor)
            .orElse("<NoActiveUserSet>")));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          RegistryAuditEventPublisher.this.publisher.publishEvent(auditEvent);
        }
      });
    }
    else {
      this.publisher.publishEvent(auditEvent);
    }
  }

}
