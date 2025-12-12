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
import se.swedenconnect.oidf.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.EntityToDto;
import se.swedenconnect.oidf.registry.dto.EntityWithModulesDto;
import se.swedenconnect.oidf.registry.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.dto.FederationEntityWithModulesDto;
import se.swedenconnect.oidf.registry.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link EntityConfigService} using JPA entities.
 *
 * @author Per Fredrik Plars
 */
@Service
public class EntityConfigServiceImpl implements EntityConfigService {

  private final EntityRepository entityRepository;
  private final OrganizationService organizationService;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param entityRepository the entity repository
   * @param organizationService the organization service
   * @param auditService the audit service
   */
  public EntityConfigServiceImpl(final EntityRepository entityRepository,
      final OrganizationService organizationService,
      final RegistryAuditService auditService) {
    this.entityRepository = entityRepository;
    this.organizationService = organizationService;
    this.auditService = auditService;
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
      final UUID id, final FederationEntityDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final PolicyEntity policy = null; // policy relation can be handled later if needed

    final EntityEntity entity = EntityToDto.toEntity(
        id, input, EntityKeyType.FEDERATION_ENTITY, org, policy);

    this.entityRepository.save(entity);
    final FederationEntityDto dto = EntityToDto.toDto(entity);
    this.auditService.federationEntityCreated(id, dto.getIssuer(), dto.getSubject(), null, dto);
    return dto;
  }

  @Override
  @Transactional
  public FederationEntityDto updateFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id, final FederationEntityDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final EntityEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.FEDERATION_ENTITY);
    final FederationEntityDto oldDto = EntityToDto.toDto(existing);

    EntityToDto.updateEntity(existing, input);

    this.entityRepository.save(existing);
    final FederationEntityDto newDto = EntityToDto.toDto(existing);
    this.auditService.federationEntityUpdated(id, newDto.getIssuer(), newDto.getSubject(), oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional(readOnly = true)
  public FederationEntityWithModulesDto getFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id,
      final boolean includeModules) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.FEDERATION_ENTITY);
    return EntityToDto.toFederationEntityWithModules(entity);
  }

  @Override
  @Transactional
  public void deleteFederationEntity(final OrganizationRecord organizationRecord, final UUID id) {
    final EntityEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.FEDERATION_ENTITY);
    final FederationEntityDto dto = EntityToDto.toDto(entity);
    this.entityRepository.delete(entity);
    this.auditService.federationEntityDeleted(id, dto.getIssuer(), dto.getSubject(), dto);
  }

  // ---------------------------------------------------------------------------
  // Hosted entities
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public HostedEntityDto createHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id, final HostedEntityDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final PolicyEntity policy = null;

    final EntityEntity entity = EntityToDto.toEntity(
        id, input, EntityKeyType.HOSTED_ENTITY, org, policy);

    this.entityRepository.save(entity);
    final HostedEntityDto dto = EntityToDto.toDtoHosted(entity);
    this.auditService.hostedEntityCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public HostedEntityDto updateHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id, final HostedEntityDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final EntityEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.HOSTED_ENTITY);
    final HostedEntityDto oldDto = EntityToDto.toDtoHosted(existing);

    EntityToDto.updateEntity(existing, input);

    this.entityRepository.save(existing);
    final HostedEntityDto newDto = EntityToDto.toDtoHosted(existing);
    this.auditService.hostedEntityUpdated(id, oldDto, newDto);
    return newDto;
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
    final HostedEntityDto dto = EntityToDto.toDtoHosted(entity);
    this.entityRepository.delete(entity);
    this.auditService.hostedEntityDeleted(id, dto);
  }

  // ---------------------------------------------------------------------------
  // Subordinate entities
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public SubordinateEntityDto createSubordinateEntity(final OrganizationRecord organizationRecord,
      final UUID id, final SubordinateEntityDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);

    final EntityEntity entity = EntityToDto.toEntity(
        id, input, EntityKeyType.SUBORDINATE_ENTITY, org);

    org.getPolicies()
        .stream()
        .filter(policyEntity -> policyEntity.getPolicyId().equals(input.getPolicyId()))
        .findFirst()
        .ifPresent(entity::setPolicyEntity);


    this.entityRepository.save(entity);
    final SubordinateEntityDto dto = EntityToDto.toDtoSubordinate(entity);
    this.auditService.subordinateEntityCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public SubordinateEntityDto updateSubordinateEntity(final OrganizationRecord organizationRecord,
      final UUID id, final SubordinateEntityDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final EntityEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityKeyType.SUBORDINATE_ENTITY);
    final SubordinateEntityDto oldDto = EntityToDto.toDtoSubordinate(existing);

    EntityToDto.updateEntity(existing, input);

    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    org.getPolicies()
        .stream()
        .filter(policyEntity -> policyEntity.getPolicyId().equals(input.getPolicyId()))
        .findFirst()
        .ifPresent(existing::setPolicyEntity);

    this.entityRepository.save(existing);
    final SubordinateEntityDto newDto = EntityToDto.toDtoSubordinate(existing);
    this.auditService.subordinateEntityUpdated(id, oldDto, newDto);
    return newDto;
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
    final SubordinateEntityDto dto = EntityToDto.toDtoSubordinate(entity);
    this.entityRepository.delete(entity);
    this.auditService.subordinateEntityDeleted(id, dto);
  }

  // ---------------------------------------------------------------------------
  // List entities
  // ---------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<EntityWithModulesDto> listEntities(final OrganizationRecord organizationRecord,
      final String type, final boolean includeModules) {
    final EntityKeyType entityKeyType = this.parseEntityType(type);
    final List<EntityEntity> entities = this.entityRepository
        .findByOrgNumberAndOptionalEntityKeyType(organizationRecord.orgNumber(), entityKeyType);

    return entities.stream()
        .map(entity -> EntityToDto.toEntityWithModulesDto(entity, includeModules))
        .collect(Collectors.toList());
  }

  private EntityKeyType parseEntityType(final String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return EntityKeyType.valueOf(type.toUpperCase());
    }
    catch (final IllegalArgumentException e) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "Invalid entity type: %s. Valid values are: federation, hosted, subordinate".formatted(type));
    }
  }


}


