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
package se.swedenconnect.oidf.entity.registry.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.UUID;

/**
 * The RegistryAuditServiceAuditEvent class implements the RegistryAuditService interface to provide audit event logging
 * functionality for the Federation API. This service utilizes an ApplicationEventPublisher to publish audit events and
 * an AuditorAware<String> to determine the current user performing the actions. It logs the following types of actions
 * as audit events - Read operations for federation entities - Read operations for trust marks subject to an entity -
 * Read operations for federation policies Audit events are constructed using FederationAuditEvent builder and are
 * published using the configured ApplicationEventPublisher.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public abstract class RegistryAuditServiceAdapter implements RegistryAuditService {

  private final ObjectMapper mapper;

  /**
   * Constructs a new instance of RegistryAuditServiceAuditAdapter, which adapts the audit functionality by leveraging
   * the provided ObjectMapper.
   *
   * @param mapper the ObjectMapper instance responsible for handling JSON conversion or serialization processes
   *     required for auditing purposes.
   */
  public RegistryAuditServiceAdapter(final ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Constructs an instance of RegistryAuditServiceAuditAdapter with a default {@link ObjectMapper}. This class acts as
   * an adapter for auditing federation registry service events, enabling the logging or processing of such events in a
   * standardized manner. The adapter can facilitate operations such as event serialization and integration with logging
   * frameworks or monitoring systems. The default constructor initializes the required ObjectMapper for internal
   * operations.
   */
  public RegistryAuditServiceAdapter() {
    this(new ObjectMapper());
  }



  @Override
  public void optionsCreate(final UUID optionsRecordId, final FkKeyType fkKeyType, final OptionsRecord oldRecord,
      final OptionsRecord newRecord) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.OPTIONS_CREATED)
            .fkKeyType(fkKeyType)
            .optionId(optionsRecordId)
            .oldData(this.toJson(oldRecord))
            .newData(this.toJson(newRecord))
            .build());
  }

  @Override
  public void optionsUpdate(final UUID optionsRecordId, final FkKeyType fkKeyType, final OptionsRecord oldRecord,
      final OptionsRecord newRecord) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.OPTIONS_UPDATE)
            .fkKeyType(fkKeyType)
            .optionId(optionsRecordId)
            .oldData(this.toJson(oldRecord))
            .newData(this.toJson(newRecord))
            .build());
  }

  @Override
  public void optionsDelete(final UUID optionsRecordId, final FkKeyType fkKeyType, final OptionsRecord deleteRecord) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.OPTIONS_DELETED)
            .fkKeyType(fkKeyType)
            .optionId(optionsRecordId)
            .oldData(this.toJson(deleteRecord))
            .build());
  }

  /**
   * Emits an audit event to be processed. This method is used to perform specific actions related to an audit event,
   * which may include logging, monitoring, or compliance-related processing.
   *
   * @param event the {@link FederationAuditEvent} to be emitted. It contains details about the event such as event
   *     type, issuer, subject, and potential changes made to the data (old and new).
   */
  protected abstract void emitEvent(final FederationAuditEvent event);

  private String toJson(final Object obj) {
    try {
      return this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
    catch (final JsonProcessingException e) {
      log.info("Unable to create json from object.", e);
      return "<No Json Representation Available>";
    }
  }

}
