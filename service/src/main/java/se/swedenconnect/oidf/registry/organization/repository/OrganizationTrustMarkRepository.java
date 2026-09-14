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
package se.swedenconnect.oidf.registry.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.organization.model.OrganizationTrustMark;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link OrganizationTrustMark} entities — the trust mark types a tenant operator has
 * pre-approved for an organization.
 *
 * @author Felix Hellman
 */
public interface OrganizationTrustMarkRepository
    extends JpaRepository<OrganizationTrustMark, OrganizationTrustMark.OrganizationTrustMarkId> {

  /**
   * Finds every pre-validated trust mark type of the given organization.
   *
   * @param organizationId the organization the trust mark types are pre-approved for
   * @return the organization's pre-validated trust marks
   */
  List<OrganizationTrustMark> findByOrganization_OrganizationId(UUID organizationId);

  /**
   * Finds every pre-validated trust mark type of every organization placed on the given instance. Used to build
   * the operator's tenant-wide organization listing in one query rather than one per organization.
   *
   * @param instanceId the instance the organizations are placed on
   * @return the pre-validated trust marks on that instance
   */
  List<OrganizationTrustMark> findByOrganization_Instance_InstanceId(UUID instanceId);

  /**
   * Tells whether the given trust mark type is pre-approved for the given organization. This is the check that
   * lets a trust mark enrollment skip manual review.
   *
   * @param organizationId the organization enrolling for the trust mark
   * @param trustMarkType the trust mark type being enrolled for
   * @return true if the type is pre-approved for that organization
   */
  boolean existsByOrganization_OrganizationIdAndTrustMarkType(UUID organizationId, String trustMarkType);

  /**
   * Deletes every pre-validated trust mark type of the given organization. Used to replace the whole list in
   * one operation.
   *
   * @param organizationId the organization whose trust mark types are removed
   */
  void deleteByOrganization_OrganizationId(UUID organizationId);
}
