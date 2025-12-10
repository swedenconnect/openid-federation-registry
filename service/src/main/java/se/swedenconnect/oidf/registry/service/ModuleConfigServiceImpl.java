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
import se.swedenconnect.oidf.registry.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.dto.ResolverDto;
import se.swedenconnect.oidf.registry.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.ResolverEntity;
import se.swedenconnect.oidf.registry.entity.TaImEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustmarkIssuerEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.repository.ResolverRepository;
import se.swedenconnect.oidf.registry.repository.TaImRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.repository.TrustmarkIssuerRepository;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

import java.util.UUID;

/**
 * Default implementation of {@link ModuleConfigService} for TrustAnchor, Resolver and Trustmark modules.
 *
 * @author Per Fredrik Plars
 */
@Service
public class ModuleConfigServiceImpl implements ModuleConfigService {

  private final TaImRepository moduleRepository;
  private final EntityRepository entityRepository;
  private final TrustMarkRepository trustMarkRepository;
  private final TrustmarkIssuerRepository trustmarkIssuerRepository;
  private final ResolverRepository resolverRepository;
  private final OrganizationService organizationService;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param moduleRepository the module repository
   * @param entityRepository the entity repository
   * @param trustMarkRepository the trust mark repository
   * @param trustmarkIssuerRepository the trustmark issuer repository
   * @param resolverRepository the resolver repository
   * @param organizationService the organization service
   * @param auditService the audit service
   */
  public ModuleConfigServiceImpl(final TaImRepository moduleRepository,
      final EntityRepository entityRepository,
      final TrustMarkRepository trustMarkRepository,
      final TrustmarkIssuerRepository trustmarkIssuerRepository,
      final ResolverRepository resolverRepository,
      final OrganizationService organizationService,
      final RegistryAuditService auditService) {
    this.moduleRepository = moduleRepository;
    this.entityRepository = entityRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.trustmarkIssuerRepository = trustmarkIssuerRepository;
    this.resolverRepository = resolverRepository;
    this.organizationService = organizationService;
    this.auditService = auditService;
  }

  private OrganizationEntity resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(
        organizationRecord.orgNumber(), organizationRecord.orgName());
  }

  private EntityEntity findFederationEntityOrThrow(final OrganizationRecord organizationRecord,
      final UUID entityId) {
    return this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), entityId, EntityKeyType.FEDERATION_ENTITY)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.RELATION_NOT_FOUND,
            "No federation entity found for id %s".formatted(entityId)));
  }

  private TaImEntity findModuleOrThrow(final OrganizationRecord organizationRecord,
      final UUID id, final FkKeyType type) {
    return this.moduleRepository.findByOrgNumberAndTaImIdAndModuleType(
            organizationRecord.orgNumber(), id, type.name())
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND,
            "No module of type %s found for id %s".formatted(type, id)));
  }

  // ---------------------------------------------------------------------------
  // TrustAnchor
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public TrustAnchorDto createTrustAnchor(final OrganizationRecord organizationRecord,
      final UUID id, final TrustAnchorDto input) {
    new ValidateDto(organizationRecord).validate(input);

    // entityId is a database UUID to EntityEntity
    final UUID entityId = input.getEntityId();
    final EntityEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final OrganizationEntity org = this.resolveOrganization(organizationRecord);

    if (entityEntity.getModuleByType(FkKeyType.TRUSTANCHOR).isPresent()) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "A TRUSTANCHOR module already exists for entity %s".formatted(entityEntity.getEntityId()));
    }

    final TaImEntity module = EntityToDto.toEntity(id, input, entityEntity, org);

    this.moduleRepository.save(module);
    final TrustAnchorDto dto = EntityToDto.toDto(module);
    this.auditService.trustAnchorCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public TrustAnchorDto updateTrustAnchor(final OrganizationRecord organizationRecord,
      final UUID id, final TrustAnchorDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final TaImEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.TRUSTANCHOR);
    final TrustAnchorDto oldDto = EntityToDto.toDto(module);

    EntityToDto.updateModuleEntity(module, input);
    this.moduleRepository.save(module);
    final TrustAnchorDto newDto = EntityToDto.toDto(module);
    this.auditService.trustAnchorUpdated(id, oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional(readOnly = true)
  public TrustAnchorDto getTrustAnchor(final OrganizationRecord organizationRecord, final UUID id) {
    final TaImEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.TRUSTANCHOR);
    return EntityToDto.toDto(module);
  }

  @Override
  @Transactional
  public void deleteTrustAnchor(final OrganizationRecord organizationRecord, final UUID id) {
    final TaImEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.TRUSTANCHOR);
    final TrustAnchorDto dto = EntityToDto.toDto(module);
    this.moduleRepository.delete(module);
    this.auditService.trustAnchorDeleted(id, dto);
  }

  // ---------------------------------------------------------------------------
  // Intermediate
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public IntermediateDto createIntermediate(final OrganizationRecord organizationRecord,
      final UUID id, final IntermediateDto input) {
    new ValidateDto(organizationRecord).validate(input);

    // entityId is a database UUID to EntityEntity
    final UUID entityId = input.getEntityId();
    final EntityEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final OrganizationEntity org = this.resolveOrganization(organizationRecord);

    if (entityEntity.getModuleByType(FkKeyType.INTERMEDIATE).isPresent()) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "An INTERMEDIATE module already exists for entity %s".formatted(entityEntity.getEntityId()));
    }

    final TaImEntity module = EntityToDto.toEntity(id, input, entityEntity, org);

    this.moduleRepository.save(module);
    final IntermediateDto dto = EntityToDto.toDtoIntermediate(module);
    this.auditService.intermediateCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public IntermediateDto updateIntermediate(final OrganizationRecord organizationRecord,
      final UUID id, final IntermediateDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final TaImEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.INTERMEDIATE);
    final IntermediateDto oldDto = EntityToDto.toDtoIntermediate(module);

    EntityToDto.updateModuleEntity(module, input);
    this.moduleRepository.save(module);
    final IntermediateDto newDto = EntityToDto.toDtoIntermediate(module);
    this.auditService.intermediateUpdated(id, oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional(readOnly = true)
  public IntermediateDto getIntermediate(final OrganizationRecord organizationRecord, final UUID id) {
    final TaImEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.INTERMEDIATE);
    return EntityToDto.toDtoIntermediate(module);
  }

  @Override
  @Transactional
  public void deleteIntermediate(final OrganizationRecord organizationRecord, final UUID id) {
    final TaImEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.INTERMEDIATE);
    final IntermediateDto dto = EntityToDto.toDtoIntermediate(module);
    this.moduleRepository.delete(module);
    this.auditService.intermediateDeleted(id, dto);
  }

  // ---------------------------------------------------------------------------
  // Resolver
  // ---------------------------------------------------------------------------

  private ResolverEntity findResolverOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    return this.resolverRepository.findByOrgNumberAndResolverId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No resolver found for id %s".formatted(id)));
  }

  @Override
  @Transactional
  public ResolverDto createResolver(final OrganizationRecord organizationRecord,
      final UUID id, final ResolverDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final UUID entityId = input.getEntityId();
    final EntityEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);

    final ResolverEntity entity = EntityToDto.toEntity(id, input, entityEntity);

    this.resolverRepository.save(entity);
    final ResolverDto dto = EntityToDto.toDto(entity);
    this.auditService.resolverCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public ResolverDto updateResolver(final OrganizationRecord organizationRecord,
      final UUID id, final ResolverDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final ResolverEntity existing = this.findResolverOrThrow(organizationRecord, id);
    final ResolverDto oldDto = EntityToDto.toDto(existing);

    EntityToDto.updateEntity(existing, input);
    this.resolverRepository.save(existing);
    final ResolverDto newDto = EntityToDto.toDto(existing);
    this.auditService.resolverUpdated(id, oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional(readOnly = true)
  public ResolverDto getResolver(final OrganizationRecord organizationRecord, final UUID id) {
    final ResolverEntity entity = this.findResolverOrThrow(organizationRecord, id);
    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public void deleteResolver(final OrganizationRecord organizationRecord, final UUID id) {
    final ResolverEntity entity = this.findResolverOrThrow(organizationRecord, id);
    final ResolverDto dto = EntityToDto.toDto(entity);
    this.resolverRepository.delete(entity);
    this.auditService.resolverDeleted(id, dto);
  }

  // ---------------------------------------------------------------------------
  // Trustmark
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public TrustmarkDto createTrustmark(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkDto input) {
    new ValidateDto(organizationRecord).validate(input);

    // trustmarkissuerId is a module UUID (TRUSTMARKISSUER)
    final UUID inputTrustmarkissuerId = input.getTrustmarkissuerId();
    final TrustmarkIssuerEntity issuerModule = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), inputTrustmarkissuerId)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.RELATION_NOT_FOUND,
            "No TRUSTMARKISSUER  found for id %s".formatted(inputTrustmarkissuerId)));

    final TrustMarkEntity entity = EntityToDto.toEntity(id, input, issuerModule);
    this.trustMarkRepository.save(entity);
    final TrustmarkDto dto = EntityToDto.toDto(entity);
    this.auditService.trustmarkCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public TrustmarkDto updateTrustmark(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final TrustMarkEntity existing = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));
    final TrustmarkDto oldDto = EntityToDto.toDto(existing);

    EntityToDto.updateEntity(existing, input);

    this.trustMarkRepository.save(existing);
    final TrustmarkDto newDto = EntityToDto.toDto(existing);
    this.auditService.trustmarkUpdated(id, oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional(readOnly = true)
  public TrustmarkDto getTrustmark(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkEntity entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));

    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public void deleteTrustmark(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkEntity entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));
    final TrustmarkDto dto = EntityToDto.toDto(entity);
    this.trustMarkRepository.delete(entity);
    this.auditService.trustmarkDeleted(id, dto);
  }

  // ---------------------------------------------------------------------------
  // Trustmark Issuer
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public TrustmarkIssuerDto createTrustmarkIssuer(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkIssuerDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final UUID entityId = input.getEntityId();
    final EntityEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);

    final TrustmarkIssuerEntity entity = EntityToDto.toEntity(id, input, entityEntity);

    this.trustmarkIssuerRepository.save(entity);
    final TrustmarkIssuerDto dto = EntityToDto.toDto(entity);
    this.auditService.trustmarkIssuerCreated(id, null, dto);
    return dto;
  }

  @Override
  @Transactional
  public TrustmarkIssuerDto updateTrustmarkIssuer(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkIssuerDto input) {
    new ValidateDto(organizationRecord).validate(input);

    final TrustmarkIssuerEntity existing = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark issuer found for id %s".formatted(id)));
    final TrustmarkIssuerDto oldDto = EntityToDto.toDto(existing);

    EntityToDto.updateEntity(existing, input);

    this.trustmarkIssuerRepository.save(existing);
    final TrustmarkIssuerDto newDto = EntityToDto.toDto(existing);
    this.auditService.trustmarkIssuerUpdated(id, oldDto, newDto);
    return newDto;
  }

  @Override
  @Transactional(readOnly = true)
  public TrustmarkIssuerDto getTrustmarkIssuer(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustmarkIssuerEntity entity = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark issuer found for id %s".formatted(id)));

    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public void deleteTrustmarkIssuer(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustmarkIssuerEntity entity = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark issuer found for id %s".formatted(id)));
    final TrustmarkIssuerDto dto = EntityToDto.toDto(entity);
    this.trustmarkIssuerRepository.delete(entity);
    this.auditService.trustmarkIssuerDeleted(id, dto);
  }
}


