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

  TrustAnchorDto createTrustAnchor(OrganizationRecord organizationRecord,
      UUID id, TrustAnchorDto input);

  TrustAnchorDto updateTrustAnchor(OrganizationRecord organizationRecord,
      UUID id, TrustAnchorDto input);

  TrustAnchorDto getTrustAnchor(OrganizationRecord organizationRecord, UUID id);

  void deleteTrustAnchor(OrganizationRecord organizationRecord, UUID id);

  ResolverDto createResolver(OrganizationRecord organizationRecord,
      UUID id, ResolverDto input);

  ResolverDto updateResolver(OrganizationRecord organizationRecord,
      UUID id, ResolverDto input);

  ResolverDto getResolver(OrganizationRecord organizationRecord, UUID id);

  void deleteResolver(OrganizationRecord organizationRecord, UUID id);

  TrustmarkDto createTrustmark(OrganizationRecord organizationRecord,
      UUID id, TrustmarkDto input);

  TrustmarkDto updateTrustmark(OrganizationRecord organizationRecord,
      UUID id, TrustmarkDto input);

  TrustmarkDto getTrustmark(OrganizationRecord organizationRecord, UUID id);

  void deleteTrustmark(OrganizationRecord organizationRecord, UUID id);
}


