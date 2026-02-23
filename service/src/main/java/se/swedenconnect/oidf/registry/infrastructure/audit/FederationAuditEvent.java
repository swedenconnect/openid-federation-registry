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

import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.actuate.audit.AuditEvent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an audit event specific to federation-related actions, capturing details about the event type, issuer,
 * subject, associated trust mark, old data, and new data. Instances of this class encapsulate the details of a specific
 * audit event. It provides a mechanism to convert the event into a standardized {@link AuditEvent} format with
 * additional structured data.
 *
 * @author Per Fredrik Plars
 */
@Builder
@Getter
public class FederationAuditEvent implements Serializable {
  final RegistryAuditEventType event;
  final String organizationId;
  final String instanceId;
  final String extId;
  final String oldData;
  final String newData;

  /**
   * Converts the current instance of {@code FederationAuditEvent} into an {@code AuditEvent}. This method structures
   * the event data into a standardized format, optionally including details such as issuer, subject, trust mark ID, old
   * data, new data, and external ID.
   *
   * @param principal the principal responsible for the event. Typically, represents the user or entity on whose
   *     behalf the action was performed, included in the resulting {@code AuditEvent}.
   * @return an {@code AuditEvent} instance containing the structured data of the current audit event.
   */
  public AuditEvent toAuditEvent(final String principal) {
    final Map<String, Object> data = new HashMap<>();
    Optional.ofNullable(this.oldData).filter(old -> !old.equals(this.newData))
        .ifPresent(v -> data.put("oldData", v));
    Optional.ofNullable(this.newData).ifPresent(v -> data.put("newData", v));
    Optional.ofNullable(this.extId).ifPresent(v -> data.put("extId", v));
    Optional.ofNullable(this.organizationId).ifPresent(v -> data.put("organizationId", v));
    Optional.ofNullable(this.instanceId).ifPresent(v -> data.put("instanceId", v));
    return new AuditEvent(principal, this.event.name(), data);
  }
}
