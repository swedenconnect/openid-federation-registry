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
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.policy.dto.PolicyDto;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import tools.jackson.databind.json.JsonMapper;

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

  private final JsonMapper mapper;

  /**
   * Constructs a new instance of RegistryAuditServiceAdapter, which adapts the audit functionality by leveraging
   * the provided JsonMapper.
   *
   * @param mapper the JsonMapper instance responsible for handling JSON conversion or serialization processes
   *     required for auditing purposes.
   */
  public RegistryAuditServiceAdapter(final JsonMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Constructs an instance of RegistryAuditServiceAdapter with a default {@link JsonMapper}. This class acts as
   * an adapter for auditing federation registry service events, enabling the logging or processing of such events in a
   * standardized manner. The adapter can facilitate operations such as event serialization and integration with logging
   * frameworks or monitoring systems. The default constructor initializes the required JsonMapper for internal
   * operations.
   */
  public RegistryAuditServiceAdapter() {
    this(new JsonMapper());
  }

  @Override
  public void policyCreated(final UUID policyId, final UUID instanceId, final UUID organizationId,
      final PolicyDto oldData, final PolicyDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.POLICY_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(policyId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void policyUpdated(final UUID policyId, final UUID instanceId, final UUID organizationId,
      final PolicyDto oldData, final PolicyDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.POLICY_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(policyId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void policyDeleted(final UUID policyId, final UUID instanceId, final UUID organizationId,
      final PolicyDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.POLICY_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(policyId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void federationEntityCreated(final UUID entityId, final UUID instanceId, final UUID organizationId,
      final String issuer, final String subject, final FederationEntityDto oldData, final FederationEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.FEDERATION_ENTITY_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void federationEntityUpdated(final UUID entityId, final UUID instanceId, final UUID organizationId,
      final String issuer, final String subject, final FederationEntityDto oldData, final FederationEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.FEDERATION_ENTITY_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void federationEntityDeleted(final UUID entityId, final UUID instanceId, final UUID organizationId,
      final String issuer, final String subject, final FederationEntityDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.FEDERATION_ENTITY_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(entityId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void hostedEntityCreated(final UUID entityId, final UUID instanceId, final UUID organizationId,
      final HostedEntityDto oldData, final HostedEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.HOSTED_ENTITY_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void hostedEntityUpdated(final UUID entityId, final UUID instanceId, final UUID organizationId,
      final HostedEntityDto oldData, final HostedEntityDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.HOSTED_ENTITY_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(entityId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void hostedEntityDeleted(final UUID entityId, final UUID instanceId, final UUID organizationId,
      final HostedEntityDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.HOSTED_ENTITY_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(entityId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustAnchorCreated(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final TrustAnchorDto oldData, final TrustAnchorDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUST_ANCHOR_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustAnchorUpdated(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final TrustAnchorDto oldData, final TrustAnchorDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUST_ANCHOR_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustAnchorDeleted(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final TrustAnchorDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUST_ANCHOR_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void intermediateCreated(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final IntermediateDto oldData, final IntermediateDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.INTERMEDIATE_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void intermediateUpdated(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final IntermediateDto oldData, final IntermediateDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.INTERMEDIATE_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void intermediateDeleted(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final IntermediateDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.INTERMEDIATE_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void resolverCreated(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final ResolverDto oldData, final ResolverDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.RESOLVER_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void resolverUpdated(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final ResolverDto oldData, final ResolverDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.RESOLVER_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void resolverDeleted(final UUID moduleId, final UUID instanceId, final UUID organizationId,
      final ResolverDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.RESOLVER_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(moduleId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustmarkCreated(final UUID trustmarkId, final UUID instanceId, final UUID organizationId,
      final TrustmarkDto oldData, final TrustmarkDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkUpdated(final UUID trustmarkId, final UUID instanceId, final UUID organizationId,
      final TrustmarkDto oldData, final TrustmarkDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkDeleted(final UUID trustmarkId, final UUID instanceId, final UUID organizationId,
      final TrustmarkDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustmarkSubjectCreated(final UUID trustmarkSubjectId, final UUID instanceId, final UUID trustmarkId,
      final UUID organizationId, final TrustmarkSubjectDto oldData, final TrustmarkSubjectDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_SUBJECT_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkSubjectId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkSubjectUpdated(final UUID trustmarkSubjectId, final UUID instanceId, final UUID trustmarkId,
      final UUID organizationId, final TrustmarkSubjectDto oldData, final TrustmarkSubjectDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_SUBJECT_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkSubjectId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkSubjectDeleted(final UUID trustmarkSubjectId, final UUID instanceId, final UUID trustmarkId,
      final UUID organizationId, final TrustmarkSubjectDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_SUBJECT_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkSubjectId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void trustmarkIssuerCreated(final UUID trustmarkIssuerId, final UUID instanceId, final UUID organizationId,
      final TrustmarkIssuerDto oldData, final TrustmarkIssuerDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_ISSUER_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkIssuerId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkIssuerUpdated(final UUID trustmarkIssuerId, final UUID instanceId, final UUID organizationId,
      final TrustmarkIssuerDto oldData, final TrustmarkIssuerDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_ISSUER_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkIssuerId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void trustmarkIssuerDeleted(final UUID trustmarkIssuerId, final UUID instanceId, final UUID organizationId,
      final TrustmarkIssuerDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.TRUSTMARK_ISSUER_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(trustmarkIssuerId.toString())
            .oldData(this.toJson(deletedData))
            .build());
  }

  @Override
  public void subordinateCreated(final UUID subordinateId, final UUID instanceId, final UUID organizationId,
      final SubordinateDto oldData, final SubordinateDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.SUBORDINATE_CREATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(subordinateId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void subordinateUpdated(final UUID subordinateId, final UUID instanceId, final UUID organizationId,
      final SubordinateDto oldData, final SubordinateDto newData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.SUBORDINATE_UPDATED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(subordinateId.toString())
            .oldData(this.toJson(oldData))
            .newData(this.toJson(newData))
            .build());
  }

  @Override
  public void subordinateDeleted(final UUID subordinateId, final UUID instanceId, final UUID organizationId,
      final SubordinateDto deletedData) {
    this.emitEvent(
        FederationAuditEvent.builder()
            .event(RegistryAuditEventType.SUBORDINATE_DELETED)
            .instanceId(instanceId.toString())
            .organizationId(organizationId.toString())
            .extId(subordinateId.toString())
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
      return this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

}