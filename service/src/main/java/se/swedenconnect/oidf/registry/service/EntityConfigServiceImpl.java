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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.api.dto.EntityToDto;
import se.swedenconnect.oidf.registry.api.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.api.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.api.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.api.dto.input.FederationEntityInputDto;
import se.swedenconnect.oidf.registry.api.dto.input.HostedEntityInputDto;
import se.swedenconnect.oidf.registry.api.dto.input.SubordinateEntityInputDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.EntityRepository;

import java.util.UUID;

/**
 * Default implementation of {@link EntityConfigService} using JPA entities.
 *
 * @author Per Fredrik Plars
 */
@Service
public class EntityConfigServiceImpl implements EntityConfigService {

  private final EntityRepository entityRepository;
  private final OrganizationService organizationService;

  public EntityConfigServiceImpl(final EntityRepository entityRepository,
      final OrganizationService organizationService) {
    this.entityRepository = entityRepository;
    this.organizationService = organizationService;
  }

  private OrganizationEntity resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(
        organizationRecord.orgNumber(), organizationRecord.orgName());
  }

  private EntityEntity findEntityOrThrow(final OrganizationRecord organizationRecord,
      final UUID id, final EntityKeyType type) {
    return this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), id, type)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No entity found for type %s and id %s".formatted(type, id)));
  }

  // ---------------------------------------------------------------------------
  // Federation entities
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public FederationEntityDto createFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id, final FederationEntityInputDto input) {

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final PolicyEntity policy = null; // policy relation can be handled later if needed

    final EntityEntity entity = EntityToDto.toEntity(
        id, input, EntityKeyType.FEDERATION_ENTITY, org, policy);

    this.entityRepository.save(entity);
    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public FederationEntityDto updateFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id, final FederationEntityInputDto input) {

    final EntityEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.FEDERATION_ENTITY);

    EntityToDto.updateEntity(existing, input);

    this.entityRepository.save(existing);
    return EntityToDto.toDto(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public FederationEntityDto getFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.FEDERATION_ENTITY);
    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public void deleteFederationEntity(final OrganizationRecord organizationRecord, final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.FEDERATION_ENTITY);
    this.entityRepository.delete(entity);
  }

  // ---------------------------------------------------------------------------
  // Hosted entities
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public HostedEntityDto createHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id, final HostedEntityInputDto input) {

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final PolicyEntity policy = null;

    final EntityEntity entity = EntityToDto.toEntity(
        id, input, EntityKeyType.HOSTED_ENTITY, org, policy);

    this.entityRepository.save(entity);
    return EntityToDto.toDtoHosted(entity);
  }

  @Override
  @Transactional
  public HostedEntityDto updateHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id, final HostedEntityInputDto input) {

    final EntityEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.HOSTED_ENTITY);

    EntityToDto.updateEntity(existing, input);

    this.entityRepository.save(existing);
    return EntityToDto.toDtoHosted(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public HostedEntityDto getHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.HOSTED_ENTITY);
    return EntityToDto.toDtoHosted(entity);
  }

  @Override
  @Transactional
  public void deleteHostedEntity(final OrganizationRecord organizationRecord, final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.HOSTED_ENTITY);
    this.entityRepository.delete(entity);
  }

  // ---------------------------------------------------------------------------
  // Subordinate entities
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public SubordinateEntityDto createSubordinateEntity(final OrganizationRecord organizationRecord,
      final UUID id, final SubordinateEntityInputDto input) {

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final PolicyEntity policy = null;

    final EntityEntity entity = EntityToDto.toEntity(
        id, input, EntityKeyType.SUBORDINATE_ENTITY, org, policy);

    this.entityRepository.save(entity);
    return EntityToDto.toDtoSubordinate(entity);
  }

  @Override
  @Transactional
  public SubordinateEntityDto updateSubordinateEntity(final OrganizationRecord organizationRecord,
      final UUID id, final SubordinateEntityInputDto input) {

    final EntityEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.SUBORDINATE_ENTITY);

    EntityToDto.updateEntity(existing, input);

    this.entityRepository.save(existing);
    return EntityToDto.toDtoSubordinate(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public SubordinateEntityDto getSubordinateEntity(final OrganizationRecord organizationRecord,
      final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.SUBORDINATE_ENTITY);
    return EntityToDto.toDtoSubordinate(entity);
  }

  @Override
  @Transactional
  public void deleteSubordinateEntity(final OrganizationRecord organizationRecord, final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.SUBORDINATE_ENTITY);
    this.entityRepository.delete(entity);
  }
}


