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

import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.dto.ModuleDto;
import se.swedenconnect.oidf.registry.dto.ResolverDto;
import se.swedenconnect.oidf.registry.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkWithSubjectsDto;

import java.util.List;
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
   * Creates an intermediate.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   * @param input the intermediate data
   * @return the created intermediate
   */
  IntermediateDto createIntermediate(OrganizationRecord organizationRecord,
      UUID id, IntermediateDto input);

  /**
   * Updates an intermediate.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   * @param input the intermediate data
   * @return the updated intermediate
   */
  IntermediateDto updateIntermediate(OrganizationRecord organizationRecord,
      UUID id, IntermediateDto input);

  /**
   * Gets an intermediate by ID.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   * @return the intermediate
   */
  IntermediateDto getIntermediate(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes an intermediate.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   */
  void deleteIntermediate(OrganizationRecord organizationRecord, UUID id);

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

  /**
   * Creates a trust mark issuer.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   * @param input the trust mark issuer data
   * @return the created trust mark issuer
   */
  TrustmarkIssuerDto createTrustmarkIssuer(OrganizationRecord organizationRecord,
      UUID id, TrustmarkIssuerDto input);

  /**
   * Updates a trust mark issuer.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   * @param input the trust mark issuer data
   * @return the updated trust mark issuer
   */
  TrustmarkIssuerDto updateTrustmarkIssuer(OrganizationRecord organizationRecord,
      UUID id, TrustmarkIssuerDto input);

  /**
   * Gets a trust mark issuer by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   * @return the trust mark issuer
   */
  TrustmarkIssuerDto getTrustmarkIssuer(OrganizationRecord organizationRecord, UUID id);

  /**
   * Deletes a trust mark issuer.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   */
  void deleteTrustmarkIssuer(OrganizationRecord organizationRecord, UUID id);

  /**
   * Lists all modules for the organization, optionally filtered by type.
   *
   * @param organizationRecord the organization record
   * @param type optional module type filter (trustanchor, intermediate, resolver, trustmarkissuer)
   * @return modules grouped by type
   */
  ModuleDto listModules(OrganizationRecord organizationRecord, String type);

  /**
   * Lists all trustmarks for the organization, optionally including trustmark subjects.
   *
   * @param organizationRecord the organization record
   * @param includeSubjects if true, includes trustmark subjects in the response
   * @return list of trustmarks with optionally included trustmark subjects
   */
  List<TrustmarkWithSubjectsDto> listTrustmarks(OrganizationRecord organizationRecord, boolean includeSubjects);
}


