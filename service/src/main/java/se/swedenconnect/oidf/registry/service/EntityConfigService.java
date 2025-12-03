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

import se.swedenconnect.oidf.registry.api.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.api.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.api.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;

import java.util.UUID;

/**
 * Service interface for managing Federation/Hosted/Subordinate entities.
 *
 * @author Per Fredrik Plars
 */
public interface EntityConfigService {

  FederationEntityDto createFederationEntity(OrganizationRecord organizationRecord,
      UUID id, FederationEntityDto input);

  FederationEntityDto updateFederationEntity(OrganizationRecord organizationRecord,
      UUID id, FederationEntityDto input);

  FederationEntityDto getFederationEntity(OrganizationRecord organizationRecord, UUID id);

  void deleteFederationEntity(OrganizationRecord organizationRecord, UUID id);

  HostedEntityDto createHostedEntity(OrganizationRecord organizationRecord,
      UUID id, HostedEntityDto input);

  HostedEntityDto updateHostedEntity(OrganizationRecord organizationRecord,
      UUID id, HostedEntityDto input);

  HostedEntityDto getHostedEntity(OrganizationRecord organizationRecord, UUID id);

  void deleteHostedEntity(OrganizationRecord organizationRecord, UUID id);

  SubordinateEntityDto createSubordinateEntity(OrganizationRecord organizationRecord,
      UUID id, SubordinateEntityDto input);

  SubordinateEntityDto updateSubordinateEntity(OrganizationRecord organizationRecord,
      UUID id, SubordinateEntityDto input);

  SubordinateEntityDto getSubordinateEntity(OrganizationRecord organizationRecord, UUID id);

  void deleteSubordinateEntity(OrganizationRecord organizationRecord, UUID id);
}


