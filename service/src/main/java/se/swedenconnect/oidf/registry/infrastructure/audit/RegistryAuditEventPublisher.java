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
import tools.jackson.databind.json.JsonMapper;

/**
 * The RegistryAuditServiceAuditEvent class implements the RegistryAuditService interface to provide audit event logging
 * functionality for the Federation API. This service utilizes an ApplicationEventPublisher to publish audit events and
 * an AuditorAware to determine the current user performing the actions. It logs the following types of actions as audit
 * events: - Read operations for federation entities - Read operations for trust marks subject to an entity - Read
 * operations for federation policies Audit events are constructed using FederationAuditEvent builder and are published
 * using the configured ApplicationEventPublisher.
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
    this.publisher.publishEvent(event);
    this.publisher.publishEvent(new AuditApplicationEvent(
        event.toAuditEvent(this.currentUser.getCurrentAuditor().orElse("<NoActiveUserSet>"))));
  }

}
