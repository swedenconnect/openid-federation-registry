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

package se.swedenconnect.oidf.registry.service;

import se.swedenconnect.oidf.registry.api.dto.ResolverDto;
import se.swedenconnect.oidf.registry.api.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;

import java.util.UUID;

/**
 * Service interface for managing different federation modules (TrustAnchor, Resolver, TrustmarkIssuer/Trustmark).
 *
 * @author Per Fredrik Plars
 */
public interface ModuleConfigService {

  /**
   * Creates a trust anchor.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   * @param input the trust anchor data
   * @return the created trust anchor
   */
  TrustAnchorDto createTrustAnchor(OrganizationRecord organizationRecord,
      UUID id, TrustAnchorDto input);

  /**
   * Updates a trust anchor.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   * @param input the trust anchor data
   * @return the updated trust anchor
   */
  TrustAnchorDto updateTrustAnchor(OrganizationRecord organizationRecord,
      UUID id, TrustAnchorDto input);

  /**
   * Gets a trust anchor by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   * @return the trust anchor
   */
  TrustAnchorDto getTrustAnchor(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a trust anchor.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   */
  void deleteTrustAnchor(OrganizationRecord organizationRecord, UUID id);

  /**
   * Creates a resolver.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   * @param input the resolver data
   * @return the created resolver
   */
  ResolverDto createResolver(OrganizationRecord organizationRecord,
      UUID id, ResolverDto input);

  /**
   * Updates a resolver.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   * @param input the resolver data
   * @return the updated resolver
   */
  ResolverDto updateResolver(OrganizationRecord organizationRecord,
      UUID id, ResolverDto input);

  /**
   * Gets a resolver by ID.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   * @return the resolver
   */
  ResolverDto getResolver(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a resolver.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   */
  void deleteResolver(OrganizationRecord organizationRecord, UUID id);

  /**
   * Creates a trust mark.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   * @param input the trust mark data
   * @return the created trust mark
   */
  TrustmarkDto createTrustmark(OrganizationRecord organizationRecord,
      UUID id, TrustmarkDto input);

  /**
   * Updates a trust mark.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   * @param input the trust mark data
   * @return the updated trust mark
   */
  TrustmarkDto updateTrustmark(OrganizationRecord organizationRecord,
      UUID id, TrustmarkDto input);

  /**
   * Gets a trust mark by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   * @return the trust mark
   */
  TrustmarkDto getTrustmark(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a trust mark.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   */
  void deleteTrustmark(OrganizationRecord organizationRecord, UUID id);
}


