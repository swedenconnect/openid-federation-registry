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

package se.swedenconnect.oidf.registry.entity.service;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.dto.EntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityWithModulesDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.mapper.DtoToEntityMapper;
import se.swedenconnect.oidf.registry.entity.mapper.EntityToDtoMapper;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.guioperations.JwksKeysCacheService;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.subordinate.service.SubordinateService;

import java.util.ArrayList;
import java.util.List;
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
  private final RegistryAuditService auditService;
  private final SubordinateService subordinateService;
  private final JwksKeysCacheService jwksKeysCacheService;

  /**
   * Constructor.
   *
   * @param entityRepository the entity repository
   * @param organizationService the organization service
   * @param subordinateService the subordinate service
   * @param auditService the audit service
   * @param jwksKeysCacheService the JWKS key cache service for signing key validation
   */
  public EntityConfigServiceImpl(final EntityRepository entityRepository,
      final OrganizationService organizationService,
      final RegistryAuditService auditService,
      final SubordinateService subordinateService,
      final JwksKeysCacheService jwksKeysCacheService) {
    this.entityRepository = entityRepository;
    this.organizationService = organizationService;
    this.auditService = auditService;
    this.subordinateService = subordinateService;
    this.jwksKeysCacheService = jwksKeysCacheService;
  }

  private Organization resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(organizationRecord);
  }

  private FederationEntity findEntityOrThrow(final OrganizationRecord organizationRecord,
      final UUID id, final EntityType type) {
    return this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), id, type)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No entity found for type %s and id %s".formatted(type, id)));
  }

  // ---------------------------------------------------------------------------
  // Federation entities
  // ---------------------------------------------------------------------------

  /**
   * Creates a federation entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the federation entity data
   * @return the created federation entity
   */
  @Override
  @Transactional
  public FederationEntityDto createFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id, final FederationEntityDto input) {
    ValidateDto.init(organizationRecord).validate(input);
    this.validateSigningKeyIds(input.getSigningKeyId(), true, organizationRecord);

    final Organization org = this.resolveOrganization(organizationRecord);

    final FederationEntity entity = DtoToEntityMapper.toEntity(id, input, EntityType.FEDERATION_ENTITY, org);
    this.entityRepository.save(entity);
    final FederationEntityDto dto = EntityToDtoMapper.toFederationEntity(entity, false);
    this.auditService.federationEntityCreated(id, org.getInstance().getInstanceId(), org.getOrganizationId(),
        dto.getEntityIdentifier(), dto.getEntityIdentifier(), null, dto);
    return dto;
  }

  /**
   * Updates a federation entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the federation entity data
   * @return the updated federation entity
   */
  @Override
  @Transactional
  public FederationEntityDto updateFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id, final FederationEntityDto input) {
    ValidateDto.init(organizationRecord).validate(input);
    this.validateSigningKeyIds(input.getSigningKeyId(), true, organizationRecord);

    final FederationEntity existing = this.findEntityOrThrow(organizationRecord, id, EntityType.FEDERATION_ENTITY);
    final FederationEntityDto oldDto = EntityToDtoMapper.toFederationEntity(existing, false);

    DtoToEntityMapper.updateEntity(existing, input);

    this.entityRepository.save(existing);
    final FederationEntityDto newDto = EntityToDtoMapper.toFederationEntity(existing, false);
    this.auditService.federationEntityUpdated(id, existing.getOrganization().getInstance().getInstanceId(),
        existing.getOrganization().getOrganizationId(),
        newDto.getEntityIdentifier(), newDto.getEntityIdentifier(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a federation entity by ID.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param includeModules whether to include modules in the response
   * @return the federation entity with optional modules
   */
  @Override
  @Transactional(readOnly = true)
  public FederationEntityWithModulesDto getFederationEntity(final OrganizationRecord organizationRecord,
      final UUID id,
      final boolean includeModules) {
    final FederationEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityType.FEDERATION_ENTITY);
    return EntityToDtoMapper.toFederationEntity(entity, includeModules);
  }

  /**
   * Deletes a federation entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  @Override
  @Transactional
  public void deleteFederationEntity(final OrganizationRecord organizationRecord, final UUID id) {
    final FederationEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityType.FEDERATION_ENTITY);
    final FederationEntityDto dto = EntityToDtoMapper.toFederationEntity(entity, false);
    this.entityRepository.delete(entity);
    this.auditService.federationEntityDeleted(id, entity.getOrganization().getInstance().getInstanceId(),
        entity.getOrganization().getOrganizationId(),
        entity.getIssuer(), dto.getEntityIdentifier(), dto);
  }

  // ---------------------------------------------------------------------------
  // Hosted entities
  // ---------------------------------------------------------------------------

  /**
   * Creates a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the hosted entity data
   * @return the created hosted entity
   */
  @Override
  @Transactional
  public HostedEntityDto createHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id, final HostedEntityDto input) {
    ValidateDto.init(organizationRecord).validate(input);
    this.validateSigningKeyIds(input.getSigningKeyId(), false, organizationRecord);

    final Organization org = this.resolveOrganization(organizationRecord);
    final FederationEntity entity = DtoToEntityMapper.toEntity(
        id, input, EntityType.HOSTED_ENTITY, org);
    entity.setSubject(organizationRecord.entityPrefix());
    this.entityRepository.save(entity);
    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);
    this.auditService.hostedEntityCreated(id, org.getInstance().getInstanceId(), org.getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @param input the hosted entity data
   * @return the updated hosted entity
   */
  @Override
  @Transactional
  public HostedEntityDto updateHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id, final HostedEntityDto input) {
    ValidateDto.init(organizationRecord).validate(input);
    this.validateSigningKeyIds(input.getSigningKeyId(), false, organizationRecord);

    final FederationEntity existing = this.findEntityOrThrow(
        organizationRecord, id, EntityType.HOSTED_ENTITY);
    final HostedEntityDto oldDto = EntityToDtoMapper.toDtoHosted(existing);
    DtoToEntityMapper.updateEntity(existing, input);
    existing.setSubject(organizationRecord.entityPrefix());
    this.entityRepository.save(existing);
    final HostedEntityDto newDto = EntityToDtoMapper.toDtoHosted(existing);
    this.auditService.hostedEntityUpdated(id, existing.getOrganization().getInstance().getInstanceId(),
        existing.getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a hosted entity by ID.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   * @return the hosted entity
   */
  @Override
  @Transactional(readOnly = true)
  public HostedEntityDto getHostedEntity(final OrganizationRecord organizationRecord,
      final UUID id) {
    final FederationEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityType.HOSTED_ENTITY);
    return EntityToDtoMapper.toDtoHosted(entity);
  }

  /**
   * List a hosted entity by ID.
   *
   * @param organizationRecord the organization record
   * @param entityIdentifier the entity ID
   * @return the hosted entity
   */
  @Override
  @Transactional(readOnly = true)
  public List<HostedEntityDto> listHostedEntity(final OrganizationRecord organizationRecord,
      final String entityIdentifier) {

    final List<HostedEntityDto> results = new ArrayList<>();
    if (entityIdentifier != null && !entityIdentifier.isEmpty()) {
      this.entityRepository.findByOrgNumberAndEntityKeyTypeAndIssuer(organizationRecord.orgNumber(),
              EntityType.HOSTED_ENTITY, entityIdentifier).map(EntityToDtoMapper::toDtoHosted)
          .ifPresent(results::add);
    }
    else {
      this.entityRepository.findByOrgNumberAndOptionalEntityKeyType(organizationRecord.orgNumber(),
              EntityType.HOSTED_ENTITY)
          .stream()
          .map(EntityToDtoMapper::toDtoHosted)
          .forEach(results::add);
    }

    return results;
  }

  @Override
  @Transactional(readOnly = true)
  public List<HostedEntityDto> listHostedEntity(final String entityIdentifier) {
    final String issuerFilter = (entityIdentifier != null && !entityIdentifier.isEmpty())
        ? entityIdentifier : null;
    return this.entityRepository
        .findByEntityTypeAndOptionalIssuer(EntityType.HOSTED_ENTITY, issuerFilter)
        .stream()
        .map(EntityToDtoMapper::toDtoHosted)
        .toList();
  }

  /**
   * Deletes a hosted entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  @Override
  @Transactional
  public void deleteHostedEntity(final OrganizationRecord organizationRecord, final UUID id) {
    final FederationEntity entity = this.findEntityOrThrow(
        organizationRecord, id, EntityType.HOSTED_ENTITY);
    final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entity);
    this.entityRepository.delete(entity);
    this.auditService.hostedEntityDeleted(id, entity.getOrganization().getInstance().getInstanceId(),
        entity.getOrganization().getOrganizationId(), dto);
  }

  /**
   * Deletes a subordinate entity.
   *
   * @param organizationRecord the organization record
   * @param id the entity ID
   */
  @Override
  @Transactional
  @Deprecated
  public void deleteSubordinateEntity(final OrganizationRecord organizationRecord, final UUID id) {
    throw new RegistryServerException(ErrorTypes.BLANK,
        "Operation is not supported use new API to handle this operation");
  }

  // ---------------------------------------------------------------------------
  // List entities
  // ---------------------------------------------------------------------------

  /**
   * Lists all entities for the organization, optionally filtered by type and with modules included.
   *
   * @param organizationRecord the organization record
   * @param type optional entity type filter (federation, hosted, subordinate)
   * @param includeModules whether to include modules (trustanchor, intermediate, resolver, trustmarkissuer)
   * @return list of entities with optional modules
   */
  @Override
  @Transactional(readOnly = true)
  public EntityWithModulesDto listEntities(final OrganizationRecord organizationRecord,
      final String type, final boolean includeModules) {
    final EntityType entityType = this.parseEntityType(type);
    final List<FederationEntity> entities = this.entityRepository
        .findByOrgNumberAndOptionalEntityKeyType(organizationRecord.orgNumber(), entityType);

    final EntityWithModulesDto dto = new EntityWithModulesDto();
    dto.setFederationEntity(entities.stream()
        .filter(entity -> entity.getEntityType().equals(EntityType.FEDERATION_ENTITY))
        .map(entity -> EntityToDtoMapper.toFederationEntity(entity, includeModules))
        .toList());

    dto.setHostedEntity(entities.stream()
        .filter(entity -> entity.getEntityType().equals(EntityType.HOSTED_ENTITY))
        .map(EntityToDtoMapper::toDtoHosted)
        .toList());

    return dto;

  }

  /**
   * Validates that all provided signing key IDs exist in the allowed key set for the entity type. Federation entities
   * may only use federation keys; hosted entities may only use hosted keys. If {@code signingKeyIds} is null or empty
   * the check is skipped (key selection is optional).
   *
   * @param signingKeyIds the key IDs to validate
   * @param isFederation {@code true} for federation entities, {@code false} for hosted entities
   * @throws RegistryServerException if any key ID is not found in the allowed set
   */
  private void validateSigningKeyIds(final List<String> signingKeyIds, final boolean isFederation,
      final OrganizationRecord organizationRecord) {
    if (signingKeyIds == null || signingKeyIds.isEmpty()) {
      return;
    }
    final List<String> validKids = (isFederation
        ? this.jwksKeysCacheService.getFederationKeys(organizationRecord)
        : this.jwksKeysCacheService.getHostedKeys(organizationRecord))
        .stream()
        .map(JWK::getKeyID)
        .toList();

    final List<String> invalidKids = signingKeyIds.stream()
        .filter(kid -> !validKids.contains(kid))
        .toList();

    if (!invalidKids.isEmpty()) {
      final String entityTypeName = isFederation ? "federation" : "hosted";
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "Signing key ID(s) %s are not valid %s keys. Valid kids: %s"
              .formatted(invalidKids, entityTypeName, validKids));
    }
  }

  private EntityType parseEntityType(final String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return EntityType.valueOf(type.toUpperCase());
    }
    catch (final IllegalArgumentException e) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "Invalid entity type: %s. Valid values are: federation, hosted, subordinate".formatted(type));
    }
  }

}
