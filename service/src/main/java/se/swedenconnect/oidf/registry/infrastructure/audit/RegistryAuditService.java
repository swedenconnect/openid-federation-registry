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

import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;

import java.net.URI;
import java.util.UUID;

/**
 * RegistryAuditService defines an interface for auditing write operations within a federation API.
 * Implementations of this interface are responsible for logging or handling details related to write actions,
 * often for monitoring or compliance purposes. The interface provides methods to audit create, update, and delete
 * operations for policies, entities, modules, and trustmark subjects.
 *
 * @author Per Fredrik Plars
 */
public interface RegistryAuditService {

  /**
   * Audits the creation of a federation entity.
   *
   * @param entityId the unique identifier of the entity being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the entity.
   * @param issuer the issuer of the entity.
   * @param subject the subject of the entity.
   * @param oldData the previous state of the entity. Typically null during creation.
   * @param newData the new state of the entity after it has been created.
   */
  void federationEntityCreated(UUID entityId, UUID instanceId, UUID organizationId, String issuer, String subject,
      FederationEntityDto oldData, FederationEntityDto newData);

  /**
   * Audits the update operation performed on a federation entity.
   *
   * @param entityId the unique identifier of the entity being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the entity.
   * @param issuer the issuer of the entity.
   * @param subject the subject of the entity.
   * @param oldData the previous state of the entity before the update.
   * @param newData the new state of the entity after the update.
   */
  void federationEntityUpdated(UUID entityId, UUID instanceId, UUID organizationId, String issuer, String subject,
      FederationEntityDto oldData, FederationEntityDto newData);

  /**
   * Audits the deletion of a federation entity.
   *
   * @param entityId the unique identifier of the entity being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the entity.
   * @param issuer the issuer of the entity.
   * @param subject the subject of the entity.
   * @param deletedData the data of the deleted entity.
   */
  void federationEntityDeleted(UUID entityId, UUID instanceId, UUID organizationId, String issuer, String subject,
      FederationEntityDto deletedData);

  /**
   * Audits the creation of a hosted entity.
   *
   * @param entityId the unique identifier of the entity being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the entity.
   * @param oldData the previous state of the entity. Typically null during creation.
   * @param newData the new state of the entity after it has been created.
   */
  void hostedEntityCreated(UUID entityId, UUID instanceId, UUID organizationId, HostedEntityDto oldData,
      HostedEntityDto newData);

  /**
   * Audits the update operation performed on a hosted entity.
   *
   * @param entityId the unique identifier of the entity being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the entity.
   * @param oldData the previous state of the entity before the update.
   * @param newData the new state of the entity after the update.
   */
  void hostedEntityUpdated(UUID entityId, UUID instanceId, UUID organizationId, HostedEntityDto oldData,
      HostedEntityDto newData);

  /**
   * Audits the deletion of a hosted entity.
   *
   * @param entityId the unique identifier of the entity being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the entity.
   * @param deletedData the data of the deleted entity.
   */
  void hostedEntityDeleted(UUID entityId, UUID instanceId, UUID organizationId, HostedEntityDto deletedData);


  /**
   * Audits the creation of a trust anchor module.
   *
   * @param moduleId the unique identifier of the module being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param oldData the previous state of the module. Typically null during creation.
   * @param newData the new state of the module after it has been created.
   */
  void trustAnchorCreated(UUID moduleId, UUID instanceId, UUID organizationId, TrustAnchorDto oldData,
      TrustAnchorDto newData);

  /**
   * Audits the update operation performed on a trust anchor module.
   *
   * @param moduleId the unique identifier of the module being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param oldData the previous state of the module before the update.
   * @param newData the new state of the module after the update.
   */
  void trustAnchorUpdated(UUID moduleId, UUID instanceId, UUID organizationId, TrustAnchorDto oldData,
      TrustAnchorDto newData);

  /**
   * Audits the deletion of a trust anchor module.
   *
   * @param moduleId the unique identifier of the module being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param deletedData the data of the deleted module.
   */
  void trustAnchorDeleted(UUID moduleId, UUID instanceId, UUID organizationId, TrustAnchorDto deletedData);

  /**
   * Audits the creation of an intermediate module.
   *
   * @param moduleId the unique identifier of the module being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param oldData the previous state of the module. Typically null during creation.
   * @param newData the new state of the module after it has been created.
   */
  void intermediateCreated(UUID moduleId, UUID instanceId, UUID organizationId, IntermediateDto oldData,
      IntermediateDto newData);

  /**
   * Audits the update operation performed on an intermediate module.
   *
   * @param moduleId the unique identifier of the module being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param oldData the previous state of the module before the update.
   * @param newData the new state of the module after the update.
   */
  void intermediateUpdated(UUID moduleId, UUID instanceId, UUID organizationId, IntermediateDto oldData,
      IntermediateDto newData);

  /**
   * Audits the deletion of an intermediate module.
   *
   * @param moduleId the unique identifier of the module being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param deletedData the data of the deleted module.
   */
  void intermediateDeleted(UUID moduleId, UUID instanceId, UUID organizationId, IntermediateDto deletedData);

  /**
   * Audits the creation of a resolver module.
   *
   * @param moduleId the unique identifier of the module being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param oldData the previous state of the module. Typically null during creation.
   * @param newData the new state of the module after it has been created.
   */
  void resolverCreated(UUID moduleId, UUID instanceId, UUID organizationId, ResolverDto oldData, ResolverDto newData);

  /**
   * Audits the update operation performed on a resolver module.
   *
   * @param moduleId the unique identifier of the module being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param oldData the previous state of the module before the update.
   * @param newData the new state of the module after the update.
   */
  void resolverUpdated(UUID moduleId, UUID instanceId, UUID organizationId, ResolverDto oldData, ResolverDto newData);

  /**
   * Audits the deletion of a resolver module.
   *
   * @param moduleId the unique identifier of the module being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the module.
   * @param deletedData the data of the deleted module.
   */
  void resolverDeleted(UUID moduleId, UUID instanceId, UUID organizationId, ResolverDto deletedData);

  /**
   * Audits the creation of a trustmark.
   *
   * @param trustmarkId the unique identifier of the trustmark being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the trustmark.
   * @param oldData the previous state of the trustmark. Typically null during creation.
   * @param newData the new state of the trustmark after it has been created.
   */
  void trustmarkCreated(UUID trustmarkId, UUID instanceId, UUID organizationId, TrustmarkDto oldData,
      TrustmarkDto newData);

  /**
   * Audits the update operation performed on a trustmark.
   *
   * @param trustmarkId the unique identifier of the trustmark being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the trustmark.
   * @param oldData the previous state of the trustmark before the update.
   * @param newData the new state of the trustmark after the update.
   */
  void trustmarkUpdated(UUID trustmarkId, UUID instanceId, UUID organizationId, TrustmarkDto oldData,
      TrustmarkDto newData);

  /**
   * Audits the deletion of a trustmark.
   *
   * @param trustmarkId the unique identifier of the trustmark being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the trustmark.
   * @param deletedData the data of the deleted trustmark.
   */
  void trustmarkDeleted(UUID trustmarkId, UUID instanceId, UUID organizationId, TrustmarkDto deletedData);

  /**
   * Audits the creation of a trustmark subject.
   *
   * @param trustmarkSubjectId the unique identifier of the trustmark subject being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param trustmarkId the unique identifier of the associated trustmark.
   * @param organizationId the unique identifier of the organization that owns the trustmark subject.
   * @param oldData the previous state of the trustmark subject. Typically null during creation.
   * @param newData the new state of the trustmark subject after it has been created.
   */
  void trustmarkSubjectCreated(UUID trustmarkSubjectId, UUID instanceId, UUID trustmarkId, UUID organizationId,
      TrustmarkSubjectDto oldData, TrustmarkSubjectDto newData);

  /**
   * Audits the update operation performed on a trustmark subject.
   *
   * @param trustmarkSubjectId the unique identifier of the trustmark subject being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param trustmarkId the unique identifier of the associated trustmark.
   * @param organizationId the unique identifier of the organization that owns the trustmark subject.
   * @param oldData the previous state of the trustmark subject before the update.
   * @param newData the new state of the trustmark subject after the update.
   */
  void trustmarkSubjectUpdated(UUID trustmarkSubjectId, UUID instanceId, UUID trustmarkId, UUID organizationId,
      TrustmarkSubjectDto oldData, TrustmarkSubjectDto newData);

  /**
   * Audits the deletion of a trustmark subject.
   *
   * @param trustmarkSubjectId the unique identifier of the trustmark subject being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param trustmarkId the unique identifier of the associated trustmark.
   * @param organizationId the unique identifier of the organization that owns the trustmark subject.
   * @param deletedData the data of the deleted trustmark subject.
   */
  void trustmarkSubjectDeleted(UUID trustmarkSubjectId, UUID instanceId, UUID trustmarkId, UUID organizationId,
      TrustmarkSubjectDto deletedData);

  /**
   * Audits the creation of a trustmark issuer.
   *
   * @param trustmarkIssuerId the unique identifier of the trustmark issuer being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the trustmark issuer.
   * @param oldData the previous state of the trustmark issuer. Typically null during creation.
   * @param newData the new state of the trustmark issuer after it has been created.
   */
  void trustmarkIssuerCreated(UUID trustmarkIssuerId, UUID instanceId, UUID organizationId,
      TrustmarkIssuerDto oldData, TrustmarkIssuerDto newData);

  /**
   * Audits the update operation performed on a trustmark issuer.
   *
   * @param trustmarkIssuerId the unique identifier of the trustmark issuer being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the trustmark issuer.
   * @param oldData the previous state of the trustmark issuer before the update.
   * @param newData the new state of the trustmark issuer after the update.
   */
  void trustmarkIssuerUpdated(UUID trustmarkIssuerId, UUID instanceId, UUID organizationId,
      TrustmarkIssuerDto oldData, TrustmarkIssuerDto newData);

  /**
   * Audits the deletion of a trustmark issuer.
   *
   * @param trustmarkIssuerId the unique identifier of the trustmark issuer being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the trustmark issuer.
   * @param deletedData the data of the deleted trustmark issuer.
   */
  void trustmarkIssuerDeleted(UUID trustmarkIssuerId, UUID instanceId, UUID organizationId,
      TrustmarkIssuerDto deletedData);

  /**
   * Audits the creation of a subordinate.
   *
   * @param subordinateId the unique identifier of the subordinate being created.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the subordinate.
   * @param oldData the previous state of the subordinate. Typically null during creation.
   * @param newData the new state of the subordinate after it has been created.
   */
  void subordinateCreated(UUID subordinateId, UUID instanceId, UUID organizationId, SubordinateDto oldData,
      SubordinateDto newData);

  /**
   * Audits the update operation performed on a subordinate.
   *
   * @param subordinateId the unique identifier of the subordinate being updated.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the subordinate.
   * @param oldData the previous state of the subordinate before the update.
   * @param newData the new state of the subordinate after the update.
   */
  void subordinateUpdated(UUID subordinateId, UUID instanceId, UUID organizationId, SubordinateDto oldData,
      SubordinateDto newData);

  /**
   * Audits the deletion of a subordinate.
   *
   * @param subordinateId the unique identifier of the subordinate being deleted.
   * @param instanceId the unique identifier of the instance that owns the organization.
   * @param organizationId the unique identifier of the organization that owns the subordinate.
   * @param deletedData the data of the deleted subordinate.
   */
  void subordinateDeleted(UUID subordinateId, UUID instanceId, UUID organizationId, SubordinateDto deletedData);

  /**
   * Trigger when a entity configuration resolve is made
   *
   * @param entityConfigurationLocation URL for this entityconfiguration
   */
  void resolveJwks(URI entityConfigurationLocation);
}