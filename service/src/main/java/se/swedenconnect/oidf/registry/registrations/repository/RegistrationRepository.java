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
package se.swedenconnect.oidf.registry.registrations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Registration} entities.
 *
 * @author Per Fredrik Plars
 */
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

  /**
   * Finds a registration by entity ID and status.
   *
   * @param entityId the entity identifier
   * @param status the registration status
   * @return optional registration
   */
  Optional<Registration> findByEntityIdAndStatus(String entityId, RegistrationStatus status);

  /**
   * Finds a registration by entity ID
   *
   * @param entityId the entity identifier
   * @return optional registration
   */
  Optional<Registration> findByEntityId(String entityId);


  /**
   * Finds all registrations for an intermediate with the given status.
   *
   * @param taimId the intermediate ID
   * @param status the registration status
   * @return list of matching registrations
   */
  List<Registration> findByFlowAssignment_TaIm_TaImIdAndStatus(UUID taimId, RegistrationStatus status);

  /**
   * Finds a registration by ID, scoped to the instance (tenant) the registering organization belongs to.
   *
   * @param registrationId the registration ID
   * @param instanceId the instance ID
   * @return optional registration
   */
  Optional<Registration> findByRegistrationIdAndOrganization_Instance_InstanceId(UUID registrationId,
      UUID instanceId);

  /**
   * Finds all registrations submitted on an instance (tenant).
   *
   * @param instanceId the instance ID
   * @return list of matching registrations
   */
  List<Registration> findByOrganization_Instance_InstanceId(UUID instanceId);

  /**
   * Finds all registrations for which the given organization is the registrant.
   *
   * @param organizationId the ID of the registrant organization
   * @return list of matching registrations
   */
  List<Registration> findByOrganization_OrganizationId(UUID organizationId);

  /**
   * Finds all registrations for which the given organization is the registrant, restricted to the given
   * statuses. Backs the cascade of a domain rejection, which only touches registrations still in flight.
   *
   * @param organizationId the ID of the registrant organization
   * @param statuses the statuses to include
   * @return list of matching registrations
   */
  List<Registration> findByOrganization_OrganizationIdAndStatusIn(UUID organizationId,
      Collection<RegistrationStatus> statuses);

  /**
   * Finds a registration by ID, scoped to the registrant organization.
   *
   * @param registrationId the registration ID
   * @param organizationId the ID of the registrant organization
   * @return optional registration
   */
  Optional<Registration> findByRegistrationIdAndOrganization_OrganizationId(
      UUID registrationId, UUID organizationId);

  /**
   * Finds all trust mark subordinate registrations that were generated for the given parent registration.
   *
   * @param parentId the registration ID of the parent registration
   * @return list of trust mark subordinate registrations
   */
  List<Registration> findByParentRegistration_RegistrationId(UUID parentId);

  /**
   * Finds a trust mark subordinate registration by trust mark type (entityId) and parent registration.
   *
   * @param entityId the trust mark type stored as entityId
   * @param parentRegistrationId the parent registration ID
   * @return optional registration
   */
  Optional<Registration> findByEntityIdAndParentRegistration_RegistrationId(String entityId,
      UUID parentRegistrationId);

  /**
   * Deletes registrations with the given status created before the given date.
   *
   * @param status the registration status
   * @param before the cutoff date
   */
  void deleteByStatusAndCreatedDateBefore(RegistrationStatus status, LocalDateTime before);
}