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
package se.swedenconnect.oidf.registry.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.registry.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.dto.PolicyDto;
import se.swedenconnect.oidf.registry.dto.ResolverDto;
import se.swedenconnect.oidf.registry.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkSubjectDto;

import java.util.UUID;

/**
 * The RegistryAuditServiceAdapter class implements the RegistryAuditService interface to provide audit event logging
 * functionality. This service utilizes an ApplicationEventPublisher to publish audit events and an AuditorAware to
 * determine the current user performing the actions.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public abstract class RegistryAuditServiceAdapter implements RegistryAuditService {

  private final ObjectMapper mapper;

  /**
   * Constructs a new instance of RegistryAuditServiceAdapter, which adapts the audit functionality by leveraging
   * the provided ObjectMapper.
   *
   * @param mapper the ObjectMapper instance responsible for handling JSON conversion or serialization processes
   *     required for auditing purposes.
   */
  public RegistryAuditServiceAdapter(final ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Constructs an instance of RegistryAuditServiceAdapter with a default {@link ObjectMapper}. This class acts as
   * an adapter for auditing federation registry service events, enabling the logging or processing of such events in a
   * standardized manner. The adapter can facilitate operations such as event serialization and integration with logging
   * frameworks or monitoring systems. The default constructor initializes the required ObjectMapper for internal
   * operations.
   */
  public RegistryAuditServiceAdapter() {
    this(new ObjectMapper());
  }

  @Override
  public void policyCreated(final UUID policyId, final PolicyDto oldData, final PolicyDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.POLICY_CREATED)
            .extId(policyId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void policyUpdated(final UUID policyId, final PolicyDto oldData, final PolicyDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.POLICY_UPDATED)
            .extId(policyId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void policyDeleted(final UUID policyId, final PolicyDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.POLICY_DELETED)
            .extId(policyId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void federationEntityCreated(final UUID entityId, final String issuer, final String subject,
      final FederationEntityDto oldData, final FederationEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.FEDERATION_ENTITY_CREATED)
            .extId(entityId.toString())
            .issuer(issuer)
            .subject(subject)
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void federationEntityUpdated(final UUID entityId, final String issuer, final String subject,
      final FederationEntityDto oldData, final FederationEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.FEDERATION_ENTITY_UPDATED)
            .extId(entityId.toString())
            .issuer(issuer)
            .subject(subject)
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void federationEntityDeleted(final UUID entityId, final String issuer, final String subject,
      final FederationEntityDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.FEDERATION_ENTITY_DELETED)
            .extId(entityId.toString())
            .issuer(issuer)
            .subject(subject)
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void hostedEntityCreated(final UUID entityId, final HostedEntityDto oldData, final HostedEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.HOSTED_ENTITY_CREATED)
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void hostedEntityUpdated(final UUID entityId, final HostedEntityDto oldData, final HostedEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.HOSTED_ENTITY_UPDATED)
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void hostedEntityDeleted(final UUID entityId, final HostedEntityDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.HOSTED_ENTITY_DELETED)
            .extId(entityId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void subordinateEntityCreated(final UUID entityId, final SubordinateEntityDto oldData,
      final SubordinateEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.SUBORDINATE_ENTITY_CREATED)
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void subordinateEntityUpdated(final UUID entityId, final SubordinateEntityDto oldData,
      final SubordinateEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.SUBORDINATE_ENTITY_UPDATED)
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void subordinateEntityDeleted(final UUID entityId, final SubordinateEntityDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.SUBORDINATE_ENTITY_DELETED)
            .extId(entityId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustAnchorCreated(final UUID moduleId, final TrustAnchorDto oldData, final TrustAnchorDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUST_ANCHOR_CREATED)
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustAnchorUpdated(final UUID moduleId, final TrustAnchorDto oldData, final TrustAnchorDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUST_ANCHOR_UPDATED)
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustAnchorDeleted(final UUID moduleId, final TrustAnchorDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUST_ANCHOR_DELETED)
            .extId(moduleId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void resolverCreated(final UUID moduleId, final ResolverDto oldData, final ResolverDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.RESOLVER_CREATED)
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void resolverUpdated(final UUID moduleId, final ResolverDto oldData, final ResolverDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.RESOLVER_UPDATED)
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void resolverDeleted(final UUID moduleId, final ResolverDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.RESOLVER_DELETED)
            .extId(moduleId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustmarkCreated(final UUID trustmarkId, final TrustmarkDto oldData, final TrustmarkDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_CREATED)
            .extId(trustmarkId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkUpdated(final UUID trustmarkId, final TrustmarkDto oldData, final TrustmarkDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_UPDATED)
            .extId(trustmarkId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkDeleted(final UUID trustmarkId, final TrustmarkDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_DELETED)
            .extId(trustmarkId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustmarkSubjectCreated(final UUID trustmarkSubjectId, final UUID trustmarkId,
      final TrustmarkSubjectDto oldData,
      final TrustmarkSubjectDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_SUBJECT_CREATED)
            .extId(trustmarkSubjectId.toString())
            .trustMarkId(trustmarkId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkSubjectUpdated(final UUID trustmarkSubjectId, final UUID trustmarkId,
      final TrustmarkSubjectDto oldData,
      final TrustmarkSubjectDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_SUBJECT_UPDATED)
            .extId(trustmarkSubjectId.toString())
            .trustMarkId(trustmarkId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkSubjectDeleted(final UUID trustmarkSubjectId, final UUID trustmarkId,
      final TrustmarkSubjectDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_SUBJECT_DELETED)
            .extId(trustmarkSubjectId.toString())
            .trustMarkId(trustmarkId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustmarkIssuerCreated(final UUID trustmarkIssuerId, final TrustmarkIssuerDto oldData,
      final TrustmarkIssuerDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_ISSUER_CREATED)
            .extId(trustmarkIssuerId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkIssuerUpdated(final UUID trustmarkIssuerId, final TrustmarkIssuerDto oldData,
      final TrustmarkIssuerDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_ISSUER_UPDATED)
            .extId(trustmarkIssuerId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkIssuerDeleted(final UUID trustmarkIssuerId, final TrustmarkIssuerDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_ISSUER_DELETED)
            .extId(trustmarkIssuerId.toString())
            .oldData(this.toJson(deletedData))
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

  /**
   * Converts an object to JSON string representation for audit logging.
   *
   * @param obj the object to convert to JSON
   * @return JSON string representation of the object, or error message if conversion fails
   */
  protected String toJson(final Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
    catch (final JsonProcessingException e) {
      log.info("Unable to create json from object.", e);
      return "<No Json Representation Available>";
    }
  }

}
