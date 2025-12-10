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

import se.swedenconnect.oidf.registry.dto.*;

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
   * Audits the creation of a policy.
   *
   * @param policyId the unique identifier of the policy being created.
   * @param oldData the previous state of the policy. Typically null during creation.
   * @param newData the new state of the policy after it has been created.
   */
  void policyCreated(UUID policyId, PolicyDto oldData, PolicyDto newData);

  /**
   * Audits the update operation performed on a policy.
   *
   * @param policyId the unique identifier of the policy being updated.
   * @param oldData the previous state of the policy before the update.
   * @param newData the new state of the policy after the update.
   */
  void policyUpdated(UUID policyId, PolicyDto oldData, PolicyDto newData);

  /**
   * Audits the deletion of a policy.
   *
   * @param policyId the unique identifier of the policy being deleted.
   * @param deletedData the data of the deleted policy.
   */
  void policyDeleted(UUID policyId, PolicyDto deletedData);

  /**
   * Audits the creation of a federation entity.
   *
   * @param entityId the unique identifier of the entity being created.
   * @param issuer the issuer of the entity.
   * @param subject the subject of the entity.
   * @param oldData the previous state of the entity. Typically null during creation.
   * @param newData the new state of the entity after it has been created.
   */
  void federationEntityCreated(UUID entityId, String issuer, String subject, FederationEntityDto oldData,
      FederationEntityDto newData);

  /**
   * Audits the update operation performed on a federation entity.
   *
   * @param entityId the unique identifier of the entity being updated.
   * @param issuer the issuer of the entity.
   * @param subject the subject of the entity.
   * @param oldData the previous state of the entity before the update.
   * @param newData the new state of the entity after the update.
   */
  void federationEntityUpdated(UUID entityId, String issuer, String subject, FederationEntityDto oldData,
      FederationEntityDto newData);

  /**
   * Audits the deletion of a federation entity.
   *
   * @param entityId the unique identifier of the entity being deleted.
   * @param issuer the issuer of the entity.
   * @param subject the subject of the entity.
   * @param deletedData the data of the deleted entity.
   */
  void federationEntityDeleted(UUID entityId, String issuer, String subject, FederationEntityDto deletedData);

  /**
   * Audits the creation of a hosted entity.
   *
   * @param entityId the unique identifier of the entity being created.
   * @param oldData the previous state of the entity. Typically null during creation.
   * @param newData the new state of the entity after it has been created.
   */
  void hostedEntityCreated(UUID entityId, HostedEntityDto oldData, HostedEntityDto newData);

  /**
   * Audits the update operation performed on a hosted entity.
   *
   * @param entityId the unique identifier of the entity being updated.
   * @param oldData the previous state of the entity before the update.
   * @param newData the new state of the entity after the update.
   */
  void hostedEntityUpdated(UUID entityId, HostedEntityDto oldData, HostedEntityDto newData);

  /**
   * Audits the deletion of a hosted entity.
   *
   * @param entityId the unique identifier of the entity being deleted.
   * @param deletedData the data of the deleted entity.
   */
  void hostedEntityDeleted(UUID entityId, HostedEntityDto deletedData);

  /**
   * Audits the creation of a subordinate entity.
   *
   * @param entityId the unique identifier of the entity being created.
   * @param oldData the previous state of the entity. Typically null during creation.
   * @param newData the new state of the entity after it has been created.
   */
  void subordinateEntityCreated(UUID entityId, SubordinateEntityDto oldData, SubordinateEntityDto newData);

  /**
   * Audits the update operation performed on a subordinate entity.
   *
   * @param entityId the unique identifier of the entity being updated.
   * @param oldData the previous state of the entity before the update.
   * @param newData the new state of the entity after the update.
   */
  void subordinateEntityUpdated(UUID entityId, SubordinateEntityDto oldData, SubordinateEntityDto newData);

  /**
   * Audits the deletion of a subordinate entity.
   *
   * @param entityId the unique identifier of the entity being deleted.
   * @param deletedData the data of the deleted entity.
   */
  void subordinateEntityDeleted(UUID entityId, SubordinateEntityDto deletedData);

  /**
   * Audits the creation of a trust anchor module.
   *
   * @param moduleId the unique identifier of the module being created.
   * @param oldData the previous state of the module. Typically null during creation.
   * @param newData the new state of the module after it has been created.
   */
  void trustAnchorCreated(UUID moduleId, TrustAnchorDto oldData, TrustAnchorDto newData);

  /**
   * Audits the update operation performed on a trust anchor module.
   *
   * @param moduleId the unique identifier of the module being updated.
   * @param oldData the previous state of the module before the update.
   * @param newData the new state of the module after the update.
   */
  void trustAnchorUpdated(UUID moduleId, TrustAnchorDto oldData, TrustAnchorDto newData);

  /**
   * Audits the deletion of a trust anchor module.
   *
   * @param moduleId the unique identifier of the module being deleted.
   * @param deletedData the data of the deleted module.
   */
  void trustAnchorDeleted(UUID moduleId, TrustAnchorDto deletedData);

  /**
   * Audits the creation of an intermediate module.
   *
   * @param moduleId the unique identifier of the module being created.
   * @param oldData the previous state of the module. Typically null during creation.
   * @param newData the new state of the module after it has been created.
   */
  void intermediateCreated(UUID moduleId, IntermediateDto oldData, IntermediateDto newData);

  /**
   * Audits the update operation performed on an intermediate module.
   *
   * @param moduleId the unique identifier of the module being updated.
   * @param oldData the previous state of the module before the update.
   * @param newData the new state of the module after the update.
   */
  void intermediateUpdated(UUID moduleId, IntermediateDto oldData, IntermediateDto newData);

  /**
   * Audits the deletion of an intermediate module.
   *
   * @param moduleId the unique identifier of the module being deleted.
   * @param deletedData the data of the deleted module.
   */
  void intermediateDeleted(UUID moduleId, IntermediateDto deletedData);

  /**
   * Audits the creation of a resolver module.
   *
   * @param moduleId the unique identifier of the module being created.
   * @param oldData the previous state of the module. Typically null during creation.
   * @param newData the new state of the module after it has been created.
   */
  void resolverCreated(UUID moduleId, ResolverDto oldData, ResolverDto newData);

  /**
   * Audits the update operation performed on a resolver module.
   *
   * @param moduleId the unique identifier of the module being updated.
   * @param oldData the previous state of the module before the update.
   * @param newData the new state of the module after the update.
   */
  void resolverUpdated(UUID moduleId, ResolverDto oldData, ResolverDto newData);

  /**
   * Audits the deletion of a resolver module.
   *
   * @param moduleId the unique identifier of the module being deleted.
   * @param deletedData the data of the deleted module.
   */
  void resolverDeleted(UUID moduleId, ResolverDto deletedData);

  /**
   * Audits the creation of a trustmark.
   *
   * @param trustmarkId the unique identifier of the trustmark being created.
   * @param oldData the previous state of the trustmark. Typically null during creation.
   * @param newData the new state of the trustmark after it has been created.
   */
  void trustmarkCreated(UUID trustmarkId, TrustmarkDto oldData, TrustmarkDto newData);

  /**
   * Audits the update operation performed on a trustmark.
   *
   * @param trustmarkId the unique identifier of the trustmark being updated.
   * @param oldData the previous state of the trustmark before the update.
   * @param newData the new state of the trustmark after the update.
   */
  void trustmarkUpdated(UUID trustmarkId, TrustmarkDto oldData, TrustmarkDto newData);

  /**
   * Audits the deletion of a trustmark.
   *
   * @param trustmarkId the unique identifier of the trustmark being deleted.
   * @param deletedData the data of the deleted trustmark.
   */
  void trustmarkDeleted(UUID trustmarkId, TrustmarkDto deletedData);

  /**
   * Audits the creation of a trustmark subject.
   *
   * @param trustmarkSubjectId the unique identifier of the trustmark subject being created.
   * @param trustmarkId the unique identifier of the associated trustmark.
   * @param oldData the previous state of the trustmark subject. Typically null during creation.
   * @param newData the new state of the trustmark subject after it has been created.
   */
  void trustmarkSubjectCreated(UUID trustmarkSubjectId, UUID trustmarkId, TrustmarkSubjectDto oldData,
      TrustmarkSubjectDto newData);

  /**
   * Audits the update operation performed on a trustmark subject.
   *
   * @param trustmarkSubjectId the unique identifier of the trustmark subject being updated.
   * @param trustmarkId the unique identifier of the associated trustmark.
   * @param oldData the previous state of the trustmark subject before the update.
   * @param newData the new state of the trustmark subject after the update.
   */
  void trustmarkSubjectUpdated(UUID trustmarkSubjectId, UUID trustmarkId, TrustmarkSubjectDto oldData,
      TrustmarkSubjectDto newData);

  /**
   * Audits the deletion of a trustmark subject.
   *
   * @param trustmarkSubjectId the unique identifier of the trustmark subject being deleted.
   * @param trustmarkId the unique identifier of the associated trustmark.
   * @param deletedData the data of the deleted trustmark subject.
   */
  void trustmarkSubjectDeleted(UUID trustmarkSubjectId, UUID trustmarkId, TrustmarkSubjectDto deletedData);

  /**
   * Audits the creation of a trustmark issuer.
   *
   * @param trustmarkIssuerId the unique identifier of the trustmark issuer being created.
   * @param oldData the previous state of the trustmark issuer. Typically null during creation.
   * @param newData the new state of the trustmark issuer after it has been created.
   */
  void trustmarkIssuerCreated(UUID trustmarkIssuerId, TrustmarkIssuerDto oldData, TrustmarkIssuerDto newData);

  /**
   * Audits the update operation performed on a trustmark issuer.
   *
   * @param trustmarkIssuerId the unique identifier of the trustmark issuer being updated.
   * @param oldData the previous state of the trustmark issuer before the update.
   * @param newData the new state of the trustmark issuer after the update.
   */
  void trustmarkIssuerUpdated(UUID trustmarkIssuerId, TrustmarkIssuerDto oldData, TrustmarkIssuerDto newData);

  /**
   * Audits the deletion of a trustmark issuer.
   *
   * @param trustmarkIssuerId the unique identifier of the trustmark issuer being deleted.
   * @param deletedData the data of the deleted trustmark issuer.
   */
  void trustmarkIssuerDeleted(UUID trustmarkIssuerId, TrustmarkIssuerDto deletedData);
}
