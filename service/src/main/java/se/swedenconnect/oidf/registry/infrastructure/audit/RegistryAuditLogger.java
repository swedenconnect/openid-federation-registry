/*
 * Copyright 2025 Sweden Connect
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

/**
 * The RegistryAuditServiceLog class is an extension of RegistryAuditServiceAdapter that is responsible for emitting
 * audit events by logging them using the SLF4J logger. This implementation ensures that audit events are captured in a
 * structured and consistent manner for monitoring and compliance purposes. The primary responsibility of this class is
 * to process and log audit events by invoking the `toAuditEvent` method of the {@link FederationAuditEvent} class and
 * rendering the resulting audit data as a log entry. Features: - Logs audit events with a predefined principal value
 * when no specific principal is identified. - Processes all types of {@link FederationAuditEvent} by converting them to
 * a and formatting them into structured logs. The implementation provides a concrete mechanism for logging audit
 * events, with the option to include additional contextual data for audit compliance.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class RegistryAuditLogger extends RegistryAuditServiceAdapter {
  @Override
  protected void emitEvent(final FederationAuditEvent event) {
    log.info(event.toAuditEvent("<NoPrincipal>").toString());
  }
}
